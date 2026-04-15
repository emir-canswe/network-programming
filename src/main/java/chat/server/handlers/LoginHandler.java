package chat.server.handlers;

import chat.protocol.OpCode;
import chat.server.ChatServerServices;
import chat.server.UserListBroadcaster;
import chat.server.session.ClientSession;
import java.io.IOException;

/**
 * Giriş: kullanıcı adı, oda şifresi, oda adı, hesap şifresi (son ikisi boş olabilir).
 */
public final class LoginHandler implements ClientRequestHandler {

  private final ChatServerServices services;
  private final UserListBroadcaster broadcaster;

  public LoginHandler(ChatServerServices services, UserListBroadcaster broadcaster) {
    this.services = services;
    this.broadcaster = broadcaster;
  }

  @Override
  public void handle(ClientSession session) throws IOException {
    if (session.isAuthenticated()) {
      session.sendError("Zaten giriş yapılmış.");
      return;
    }
    String name = session.protocolReader().readUtf8().trim();
    String password = session.protocolReader().readUtf8();
    if (password == null) {
      password = "";
    }
    String roomRaw = session.protocolReader().readUtf8();
    String accountPw = session.protocolReader().readUtf8();
    if (accountPw == null) {
      accountPw = "";
    }

    if (!name.matches("[a-zA-Z0-9_\\-ğüşıöçĞÜŞİÖÇ]{2,24}")) {
      session.send(
          w -> {
            w.writeOpcode(OpCode.S_ACK_LOGIN);
            w.writeBoolean(false);
            w.writeUtf8("Geçersiz kullanıcı adı (2-24 karakter, harf/rakam/_/- veya Türkçe harf).");
          });
      return;
    }

    if (services.isBanned(name)) {
      session.send(
          w -> {
            w.writeOpcode(OpCode.S_ACK_LOGIN);
            w.writeBoolean(false);
            w.writeUtf8("Bu kullanıcı adı engellenmiş.");
          });
      return;
    }

    String expectedRoom = services.roomPassword();
    if (expectedRoom != null && !expectedRoom.equals(password)) {
      session.send(
          w -> {
            w.writeOpcode(OpCode.S_ACK_LOGIN);
            w.writeBoolean(false);
            w.writeUtf8("Oda şifresi hatalı veya eksik.");
          });
      services.logger().warn("Hatalı oda şifresi denemesi: " + name);
      return;
    }

    String expectedAccount = services.accountPassword();
    if (expectedAccount != null && !expectedAccount.equals(accountPw)) {
      session.send(
          w -> {
            w.writeOpcode(OpCode.S_ACK_LOGIN);
            w.writeBoolean(false);
            w.writeUtf8("Hesap şifresi hatalı veya eksik.");
          });
      services.logger().warn("Hatalı hesap şifresi: " + name);
      return;
    }

    String room = ChatServerServices.sanitizeRoomName(roomRaw);
    session.setRoom(room);
    session.setUsername(name);
    if (!services.registry().register(name, session)) {
      session.setUsername("");
      session.setRoom("genel");
      session.send(
          w -> {
            w.writeOpcode(OpCode.S_ACK_LOGIN);
            w.writeBoolean(false);
            w.writeUtf8("Bu kullanıcı adı kullanımda.");
          });
      return;
    }
    session.setAuthenticated(true);
    services.registerActiveSession(name, session);
    session.send(
        w -> {
          w.writeOpcode(OpCode.S_ACK_LOGIN);
          w.writeBoolean(true);
          w.writeUtf8("Bağlantı kabul edildi. Hoş geldiniz, " + name + " (oda: " + room + ").");
        });

    services.logger().info("Yeni istemci bağlandı: " + name + " oda=" + room + " " + session.remoteAddress());
    broadcaster.notifyOthersInRoom(name, room, "USER_JOINED|" + name);
    broadcaster.sendLogToRoom(room, "[" + name + "] sohbete katıldı.");
    broadcaster.broadcastUserList();
  }
}
