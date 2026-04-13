package chat.client;

import chat.protocol.ByteBlockProgressListener;
import chat.protocol.OpCode;
import chat.protocol.ProtocolReader;
import chat.protocol.ProtocolWriter;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.SwingUtilities;

/**
 * TCP oturumu ve protokol IO — arayüzden bağımsız (SRP).
 */
public final class ChatNetworkClient {

  private static final DateTimeFormatter TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
  private static final int CHUNK_THRESHOLD_BYTES = 1024 * 1024;
  private static final int CHUNK_SIZE = 512 * 1024;

  private final ClientCallbacks ui;
  private final ExecutorService io = Executors.newSingleThreadExecutor();
  private final AtomicBoolean running = new AtomicBoolean();
  private ScheduledExecutorService heartbeat;
  private final AtomicLong lastPongMs = new AtomicLong(System.currentTimeMillis());

  private Socket socket;
  private ProtocolReader reader;
  private ProtocolWriter writer;
  private final Object writeLock = new Object();

  public ChatNetworkClient(ClientCallbacks ui) {
    this.ui = Objects.requireNonNull(ui);
  }

  public boolean isConnected() {
    return socket != null && socket.isConnected() && !socket.isClosed();
  }

  public void connect(
      String host,
      int port,
      String username,
      String roomPassword,
      String roomName,
      String accountPassword)
      throws IOException {
    disconnectQuietly();
    stopHeartbeat();
    Socket s = new Socket(host, port);
    s.setSoTimeout(0);
    this.socket = s;
    this.reader = new ProtocolReader(s.getInputStream());
    this.writer = new ProtocolWriter(s.getOutputStream());
    running.set(true);
    lastPongMs.set(System.currentTimeMillis());
    io.submit(this::readLoop);
    String pw = roomPassword == null ? "" : roomPassword;
    String rn = roomName == null ? "" : roomName;
    String ap = accountPassword == null ? "" : accountPassword;
    synchronized (writeLock) {
      writer.writeOpcode(OpCode.C_LOGIN);
      writer.writeUtf8(username.trim());
      writer.writeUtf8(pw);
      writer.writeUtf8(rn);
      writer.writeUtf8(ap);
      writer.flush();
    }
  }

  public void startHeartbeatAfterLogin() {
    stopHeartbeat();
    lastPongMs.set(System.currentTimeMillis());
    heartbeat = Executors.newScheduledThreadPool(2);
    heartbeat.scheduleAtFixedRate(
        () -> {
          try {
            sendPing();
          } catch (IOException ignored) {
          }
        },
        25,
        25,
        TimeUnit.SECONDS);
    heartbeat.scheduleAtFixedRate(
        () -> {
          if (!running.get() || socket == null || socket.isClosed()) {
            return;
          }
          long last = lastPongMs.get();
          if (System.currentTimeMillis() - last > 55_000) {
            disconnect();
            SwingUtilities.invokeLater(
                () -> ui.onDisconnected("Sunucu yanıt vermiyor (bağlantı canlılık kontrolü)."));
          }
        },
        12,
        12,
        TimeUnit.SECONDS);
  }

  public void stopHeartbeat() {
    if (heartbeat != null) {
      heartbeat.shutdownNow();
      heartbeat = null;
    }
  }

  private void sendPing() throws IOException {
    synchronized (writeLock) {
      if (writer == null) {
        return;
      }
      writer.writeOpcode(OpCode.C_PING);
      writer.flush();
    }
  }

  public void sendListUsersRequest() throws IOException {
    synchronized (writeLock) {
      writer.writeOpcode(OpCode.C_LIST_USERS);
      writer.flush();
    }
  }

  public void sendChat(String text) throws IOException {
    synchronized (writeLock) {
      writer.writeOpcode(OpCode.C_CHAT);
      writer.writeUtf8(text);
      writer.flush();
    }
  }

  public void sendTyping(boolean started) throws IOException {
    synchronized (writeLock) {
      if (writer == null) {
        return;
      }
      writer.writeOpcode(OpCode.C_TYPING);
      writer.writeBoolean(started);
      writer.flush();
    }
  }

  public void sendEditMessage(long messageId, String newText) throws IOException {
    synchronized (writeLock) {
      writer.writeOpcode(OpCode.C_EDIT_MESSAGE);
      writer.writeLong(messageId);
      writer.writeUtf8(newText);
      writer.flush();
    }
  }

  public void sendDeleteMessage(long messageId) throws IOException {
    synchronized (writeLock) {
      writer.writeOpcode(OpCode.C_DELETE_MESSAGE);
      writer.writeLong(messageId);
      writer.flush();
    }
  }

  public void sendLogout() throws IOException {
    synchronized (writeLock) {
      writer.writeOpcode(OpCode.C_LOGOUT);
      writer.flush();
    }
  }

  public void sendFileOffer(String filename, byte[] data) throws IOException {
    if (data.length > CHUNK_THRESHOLD_BYTES) {
      sendLargeFile(filename, data);
      return;
    }
    synchronized (writeLock) {
      writer.writeOpcode(OpCode.C_FILE_OFFER);
      writer.writeUtf8(filename);
      writer.writeFully(data, 0, data.length);
      writer.flush();
    }
  }

  private void sendLargeFile(String filename, byte[] data) throws IOException {
    String mime = guessMime(filename);
    synchronized (writeLock) {
      writer.writeOpcode(OpCode.C_FILE_UPLOAD_BEGIN);
      writer.writeUtf8(filename);
      writer.writeLong(data.length);
      writer.writeUtf8(mime);
      for (int off = 0, seq = 0; off < data.length; off += CHUNK_SIZE, seq++) {
        int n = Math.min(CHUNK_SIZE, data.length - off);
        writer.writeOpcode(OpCode.C_FILE_UPLOAD_PART);
        writer.writeInt(seq);
        writer.writeFully(data, off, n);
      }
      writer.writeOpcode(OpCode.C_FILE_UPLOAD_COMMIT);
      writer.flush();
    }
  }

  public void sendFileUploadCancel() throws IOException {
    synchronized (writeLock) {
      writer.writeOpcode(OpCode.C_FILE_CANCEL);
      writer.flush();
    }
  }

  public void sendFileRequest(String fileId) throws IOException {
    synchronized (writeLock) {
      writer.writeOpcode(OpCode.C_FILE_REQUEST);
      writer.writeUtf8(fileId);
      writer.flush();
    }
  }

  public void disconnect() {
    running.set(false);
    stopHeartbeat();
    try {
      if (socket != null) {
        socket.close();
      }
    } catch (IOException ignored) {
    }
    socket = null;
    reader = null;
    writer = null;
  }

  private void disconnectQuietly() {
    running.set(false);
    stopHeartbeat();
    try {
      if (socket != null) {
        socket.close();
      }
    } catch (IOException ignored) {
    }
    socket = null;
    reader = null;
    writer = null;
  }

  private void notePong() {
    lastPongMs.set(System.currentTimeMillis());
  }

  private void readLoop() {
    try {
      while (running.get() && socket != null && !socket.isClosed()) {
        int op = reader.readOpcode();
        switch (op) {
          case OpCode.S_ACK_LOGIN -> {
            boolean ok = reader.readBoolean();
            String msg = reader.readUtf8();
            SwingUtilities.invokeLater(() -> ui.onAckLogin(ok, msg));
          }
          case OpCode.S_USER_LIST -> {
            String csv = reader.readUtf8();
            SwingUtilities.invokeLater(() -> ui.onUserList(csv));
          }
          case OpCode.S_CHAT_BROADCAST -> {
            String from = reader.readUtf8();
            long t = reader.readLong();
            long mid = reader.readLong();
            String body = reader.readUtf8();
            SwingUtilities.invokeLater(() -> ui.onChatBroadcast(from, t, mid, body));
          }
          case OpCode.S_CHAT_PRIVATE -> {
            String from = reader.readUtf8();
            long t = reader.readLong();
            long mid = reader.readLong();
            String body = reader.readUtf8();
            SwingUtilities.invokeLater(() -> ui.onChatPrivate(from, t, mid, body));
          }
          case OpCode.S_NOTIFY -> {
            String n = reader.readUtf8();
            SwingUtilities.invokeLater(() -> ui.onNotify(n));
          }
          case OpCode.S_FILE_AVAILABLE -> {
            String id = reader.readUtf8();
            String from = reader.readUtf8();
            String name = reader.readUtf8();
            long sz = reader.readLong();
            String mime = reader.readUtf8();
            SwingUtilities.invokeLater(() -> ui.onFileAvailable(id, from, name, sz, mime));
          }
          case OpCode.S_FILE_PAYLOAD -> {
            String name = reader.readUtf8();
            long sz = reader.readLong();
            ByteBlockProgressListener prog =
                (pct, cur, tot) ->
                    SwingUtilities.invokeLater(() -> ui.onFileDownloadProgress(pct, cur, tot));
            byte[] data =
                reader.readByteBlock(ProtocolReader.ABSOLUTE_MAX_BLOCK, prog);
            SwingUtilities.invokeLater(
                () -> {
                  ui.onFileDownloadProgressDone();
                  ui.onFilePayload(name, sz, data);
                });
          }
          case OpCode.S_ERROR -> {
            String err = reader.readUtf8();
            SwingUtilities.invokeLater(() -> ui.onError(err));
          }
          case OpCode.S_SERVER_LOG -> {
            String log = reader.readUtf8();
            SwingUtilities.invokeLater(() -> ui.onServerLog(log));
          }
          case OpCode.S_PONG -> notePong();
          case OpCode.S_CHAT_HISTORY -> {
            int n = reader.readInt();
            List<ClientCallbacks.HistoryEntry> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
              String from = reader.readUtf8();
              long t = reader.readLong();
              long mid = reader.readLong();
              String body = reader.readUtf8();
              list.add(new ClientCallbacks.HistoryEntry(from, t, mid, body));
            }
            List<ClientCallbacks.HistoryEntry> copy = List.copyOf(list);
            SwingUtilities.invokeLater(() -> ui.onChatHistory(copy));
          }
          case OpCode.S_USER_TYPING -> {
            String who = reader.readUtf8();
            boolean st = reader.readBoolean();
            SwingUtilities.invokeLater(() -> ui.onUserTyping(who, st));
          }
          case OpCode.S_MESSAGE_EDITED -> {
            long mid = reader.readLong();
            String nt = reader.readUtf8();
            String by = reader.readUtf8();
            SwingUtilities.invokeLater(() -> ui.onMessageEdited(mid, nt, by));
          }
          case OpCode.S_MESSAGE_DELETED -> {
            long mid = reader.readLong();
            SwingUtilities.invokeLater(() -> ui.onMessageDeleted(mid));
          }
          default ->
              SwingUtilities.invokeLater(() -> ui.onError("Bilinmeyen sunucu kodu: " + op));
        }
      }
    } catch (EOFException e) {
      SwingUtilities.invokeLater(() -> ui.onDisconnected("Bağlantı sunucu tarafından kapandı."));
    } catch (IOException e) {
      SwingUtilities.invokeLater(() -> ui.onDisconnected("Bağlantı hatası: " + e.getMessage()));
    }
  }

  public static String formatTime(long epochMs) {
    return TIME.format(Instant.ofEpochMilli(epochMs));
  }

  private static String guessMime(String filename) {
    if (filename == null) {
      return "application/octet-stream";
    }
    String n = filename.toLowerCase();
    if (n.endsWith(".pdf")) {
      return "application/pdf";
    }
    if (n.endsWith(".png")) {
      return "image/png";
    }
    if (n.endsWith(".jpg") || n.endsWith(".jpeg")) {
      return "image/jpeg";
    }
    return "application/octet-stream";
  }
}
