package chat.server;

import chat.protocol.OpCode;
import chat.server.session.ClientConnection;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Kullanıcı listesi güncellemelerini tüm bağlantılara iletir (SRP).
 */
public final class UserListBroadcaster {

  private final ChatServerServices services;

  public UserListBroadcaster(ChatServerServices services) {
    this.services = services;
  }

  public void broadcastUserList() {
    String list =
        services.registry().usernames().stream().sorted().collect(Collectors.joining(","));
    services
        .registry()
        .forEachConnection(
            c -> {
              try {
                c.send(
                    w -> {
                      w.writeOpcode(OpCode.S_USER_LIST);
                      w.writeUtf8(list);
                    });
              } catch (IOException e) {
                services.logger().warn("Kullanıcı listesi gönderilemedi: " + e.getMessage());
              }
            });
  }

  public void sendUserListTo(ClientConnection c) throws IOException {
    String list =
        services.registry().usernames().stream().sorted().collect(Collectors.joining(","));
    c.send(
        w -> {
          w.writeOpcode(OpCode.S_USER_LIST);
          w.writeUtf8(list);
        });
  }

  public void sendChatHistoryTo(ClientConnection c, List<ChatHistoryBuffer.Entry> entries)
      throws IOException {
    c.send(
        w -> {
          w.writeOpcode(OpCode.S_CHAT_HISTORY);
          w.writeInt(entries.size());
          for (ChatHistoryBuffer.Entry e : entries) {
            w.writeUtf8(e.fromUser());
            w.writeLong(e.epochMs());
            w.writeUtf8(e.text());
          }
        });
  }

  public void notifyOthers(String exceptUser, String eventLine) {
    services
        .registry()
        .forEachConnection(
            c -> {
              if (!c.isAuthenticated() || c.getUsername().equals(exceptUser)) {
                return;
              }
              try {
                c.send(
                    w -> {
                      w.writeOpcode(OpCode.S_NOTIFY);
                      w.writeUtf8(eventLine);
                    });
              } catch (IOException ignored) {
              }
            });
  }

  public void notifyAll(String eventLine) {
    services
        .registry()
        .forEachConnection(
            c -> {
              if (!c.isAuthenticated()) {
                return;
              }
              try {
                c.send(
                    w -> {
                      w.writeOpcode(OpCode.S_NOTIFY);
                      w.writeUtf8(eventLine);
                    });
              } catch (IOException ignored) {
              }
            });
  }

  public void sendLogToAll(String line) {
    services
        .registry()
        .forEachConnection(
            c -> {
              if (!c.isAuthenticated()) {
                return;
              }
              try {
                c.send(
                    w -> {
                      w.writeOpcode(OpCode.S_SERVER_LOG);
                      w.writeUtf8(line);
                    });
              } catch (IOException ignored) {
              }
            });
  }

  public void broadcastChat(String fromUser, long epochMs, String text) {
    services
        .registry()
        .forEachConnection(
            c -> {
              if (!c.isAuthenticated()) {
                return;
              }
              try {
                c.send(
                    w -> {
                      w.writeOpcode(OpCode.S_CHAT_BROADCAST);
                      w.writeUtf8(fromUser);
                      w.writeLong(epochMs);
                      w.writeUtf8(text);
                    });
              } catch (IOException ignored) {
              }
            });
  }

  public void sendPrivate(ClientConnection target, String fromUser, long epochMs, String text)
      throws IOException {
    target.send(
        w -> {
          w.writeOpcode(OpCode.S_CHAT_PRIVATE);
          w.writeUtf8(fromUser);
          w.writeLong(epochMs);
          w.writeUtf8(text);
        });
  }

  public void announceFile(ClientConnection exclude, String from, String id, String name, long size) {
    services
        .registry()
        .forEachConnection(
            c -> {
              if (!c.isAuthenticated() || c == exclude) {
                return;
              }
              try {
                c.send(
                    w -> {
                      w.writeOpcode(OpCode.S_FILE_AVAILABLE);
                      w.writeUtf8(id);
                      w.writeUtf8(from);
                      w.writeUtf8(name);
                      w.writeLong(size);
                    });
              } catch (IOException ignored) {
              }
            });
  }

  /** Gönderene onay için aynı duyuru. */
  public void announceFileToSelf(ClientConnection self, String from, String id, String name, long size)
      throws IOException {
    self.send(
        w -> {
          w.writeOpcode(OpCode.S_FILE_AVAILABLE);
          w.writeUtf8(id);
          w.writeUtf8(from);
          w.writeUtf8(name);
          w.writeLong(size);
        });
  }
}
