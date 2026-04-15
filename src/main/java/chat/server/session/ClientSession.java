package chat.server.session;

import chat.protocol.OpCode;
import chat.protocol.ProtocolPayload;
import chat.protocol.ProtocolReader;
import chat.protocol.ProtocolWriter;
import chat.server.ChatServerServices;
import chat.server.UserListBroadcaster;
import chat.server.handlers.ClientRequestDispatcher;
import java.io.ByteArrayOutputStream;
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
  private volatile String room = "genel";
  private volatile boolean authenticated;
  private volatile boolean running = true;
  private volatile PendingFileUpload pendingUpload;

  /** Parçalı dosya yükleme tamponu (oturum başına en fazla bir aktif yükleme). */
  public static final class PendingFileUpload {
    public String filename;
    public String mime = "application/octet-stream";
    public long totalBytes;
    public final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
  }

  public PendingFileUpload pendingUpload() {
    return pendingUpload;
  }

  public void setPendingUpload(PendingFileUpload pendingUpload) {
    this.pendingUpload = pendingUpload;
  }

  public void clearPendingUpload() {
    this.pendingUpload = null;
  }

  public ClientSession(
      Socket socket,
      ChatServerServices services,
      ClientRequestDispatcher dispatcher,
      UserListBroadcaster broadcaster,
      Runnable onTerminated)
      throws IOException {
    this.socket = socket;
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
  public String getRoom() {
    return room;
  }

  @Override
  public void setRoom(String room) {
    this.room = room != null && !room.isBlank() ? room : "genel";
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
    clearPendingUpload();
    if (authenticated && username != null && !username.isEmpty()) {
      String r = getRoom();
      services.unregisterActiveSession(username);
      services.registry().unregister(username);
      services.logger().info("İstemci ayrıldı: " + username);
      broadcaster.notifyOthersInRoom(username, r, "USER_LEFT|" + username);
      broadcaster.sendLogToRoom(r, "[" + username + "] sohbetten ayrıldı.");
      broadcaster.broadcastUserList();
    }
    close();
    onTerminated.run();
  }
}
