package chat.server.session;

import chat.protocol.OpCode;
import chat.protocol.ProtocolPayload;
import chat.protocol.ProtocolReader;
import chat.protocol.ProtocolWriter;
import chat.server.ChatServerServices;
import chat.server.RateLimiter;
import chat.server.UserListBroadcaster;
import chat.server.handlers.ClientRequestDispatcher;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

/**
 * Tek TCP bağlantısının yaşam döngüsü ve eşzamanlı güvenli gönderim.
 */
public final class ClientSession implements ClientConnection, Runnable {

  private final Socket socket;
  private final ProtocolReader reader;
  private final ProtocolWriter writer;
  private final Object writeLock = new Object();
  private final ChatServerServices services;
  private final ClientRequestDispatcher dispatcher;
  private final UserListBroadcaster broadcaster;
  private final Runnable onTerminated;

  private volatile String username = "";
  private volatile boolean authenticated;
  private volatile boolean running = true;
  private final RateLimiter chatRateLimiter = new RateLimiter(15, 10_000);

  public RateLimiter chatRateLimiter() {
    return chatRateLimiter;
  }

  public ClientSession(
      Socket socket,
      ChatServerServices services,
      ClientRequestDispatcher dispatcher,
      UserListBroadcaster broadcaster,
      Runnable onTerminated)
      throws IOException {
    this.socket = socket;
    /* 0 = sınırsız bekleme; büyük dosya indirilirken istemci ping gönderemeyebilir. */
    socket.setSoTimeout(0);
    this.reader = new ProtocolReader(socket.getInputStream());
    this.writer = new ProtocolWriter(socket.getOutputStream());
    this.services = services;
    this.dispatcher = dispatcher;
    this.broadcaster = broadcaster;
    this.onTerminated = onTerminated;
  }

  @Override
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username != null ? username : "";
  }

  @Override
  public boolean isAuthenticated() {
    return authenticated;
  }

  public void setAuthenticated(boolean authenticated) {
    this.authenticated = authenticated;
  }

  public ProtocolReader protocolReader() {
    return reader;
  }

  public String remoteAddress() {
    return socket.getRemoteSocketAddress().toString();
  }

  @Override
  public void send(ProtocolPayload payload) throws IOException {
    synchronized (writeLock) {
      payload.write(writer);
      writer.flush();
    }
  }

  public void sendError(String message) throws IOException {
    send(
        w -> {
          w.writeOpcode(OpCode.S_ERROR);
          w.writeUtf8(message);
        });
  }

  @Override
  public void close() {
    running = false;
    try {
      socket.close();
    } catch (IOException ignored) {
    }
  }

  @Override
  public void run() {
    try {
      while (running && !socket.isClosed()) {
        int op = reader.readOpcode();
        if (!authenticated && op != OpCode.C_LOGIN) {
          sendError("Önce giriş (LOGIN) komutu gönderilmelidir.");
          break;
        }
        dispatcher.dispatch(op, this);
      }
    } catch (EOFException e) {
      services.logger().info("Bağlantı sonlandı (EOF): " + username);
    } catch (IOException e) {
      services.logger().warn("Oturum IO hatası [" + username + "]: " + e.getMessage());
    } finally {
      cleanup();
    }
  }

  private void cleanup() {
    if (authenticated && username != null && !username.isEmpty()) {
      services.registry().unregister(username);
      services.logger().info("İstemci ayrıldı: " + username);
      broadcaster.notifyAll("USER_LEFT|" + username);
      broadcaster.sendLogToAll("[" + username + "] sohbetten ayrıldı.");
      broadcaster.broadcastUserList();
    }
    close();
    onTerminated.run();
  }
}
