package chat.server;

import chat.protocol.OpCode;
import chat.server.session.ClientConnection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Kullanıcı listesi ve yayınlar — oda (room) bazlı.
 */
public final class UserListBroadcaster {

  private final ChatServerServices services;

  public UserListBroadcaster(ChatServerServices services) {
    this.services = services;
  }

  private static boolean sameRoom(ClientConnection c, String room) {
    return c.isAuthenticated() && room != null && room.equals(c.getRoom());
  }

  private String csvUsernamesInRoom(String room) {
    List<String> names = new ArrayList<>();
    services
        .registry()
        .forEachConnection(
            c -> {
              if (sameRoom(c, room)) {
                names.add(c.getUsername());
              }
            });
    Collections.sort(names);
    return names.stream().collect(Collectors.joining(","));
  }

  public void broadcastUserList() {
    services
        .registry()
        .forEachConnection(
            c -> {
              if (!c.isAuthenticated()) {
                return;
              }
              String room = c.getRoom();
              String list = csvUsernamesInRoom(room);
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
    String list = csvUsernamesInRoom(c.getRoom());
    c.send(
        w -> {
          w.writeOpcode(OpCode.S_USER_LIST);
          w.writeUtf8(list);
        });
  }

  public void notifyOthersInRoom(String exceptUser, String room, String eventLine) {
    services
        .registry()
        .forEachConnection(
            c -> {
              if (!sameRoom(c, room) || c.getUsername().equals(exceptUser)) {
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

  /** Tüm odalara (sunucu içi uyumluluk için nadir). */
  public void notifyAllAuthenticated(String eventLine) {
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

  public void sendLogToRoom(String room, String line) {
    if (room == null) {
      return;
    }
    services
        .registry()
        .forEachConnection(
            c -> {
              if (!sameRoom(c, room)) {
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

  public void broadcastChat(String fromUser, long epochMs, long messageId, String text, String room) {
    services
        .registry()
        .forEachConnection(
            c -> {
              if (!sameRoom(c, room)) {
                return;
              }
              try {
                c.send(
                    w -> {
                      w.writeOpcode(OpCode.S_CHAT_BROADCAST);
                      w.writeUtf8(fromUser);
                      w.writeLong(epochMs);
                      w.writeLong(messageId);
                      w.writeUtf8(text);
                    });
              } catch (IOException ignored) {
              }
            });
  }

  public void sendPrivate(
      ClientConnection target, String fromUser, long epochMs, long messageId, String text)
      throws IOException {
    target.send(
        w -> {
          w.writeOpcode(OpCode.S_CHAT_PRIVATE);
          w.writeUtf8(fromUser);
          w.writeLong(epochMs);
          w.writeLong(messageId);
          w.writeUtf8(text);
        });
  }

  public void announceFile(
      ClientConnection exclude,
      String from,
      String id,
      String name,
      long size,
      String mime,
      String room) {
    services
        .registry()
        .forEachConnection(
            c -> {
              if (!sameRoom(c, room) || c == exclude) {
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
                      w.writeUtf8(mime != null ? mime : "application/octet-stream");
                    });
              } catch (IOException ignored) {
              }
            });
  }

  public void announceFileToSelf(
      ClientConnection self, String from, String id, String name, long size, String mime)
      throws IOException {
    self.send(
        w -> {
          w.writeOpcode(OpCode.S_FILE_AVAILABLE);
          w.writeUtf8(id);
          w.writeUtf8(from);
          w.writeUtf8(name);
          w.writeLong(size);
          w.writeUtf8(mime != null ? mime : "application/octet-stream");
        });
  }
}
