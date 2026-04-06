package chat.server.handlers;

import chat.protocol.OpCode;
import chat.server.ChatServerServices;
import chat.server.UserListBroadcaster;
import chat.server.session.ClientSession;
import java.io.IOException;

/**
 * Giriş komutu — kullanıcı adı + opsiyonel oda şifresi, geçmiş mesajlar.
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

    if (!name.matches("[a-zA-Z0-9_\\-ğüşıöçĞÜŞİÖÇ]{2,24}")) {
      session.send(
          w -> {
            w.writeOpcode(OpCode.S_ACK_LOGIN);
            w.writeBoolean(false);
            w.writeUtf8("Geçersiz kullanıcı adı (2-24 karakter, harf/rakam/_/- veya Türkçe harf).");
          });
      return;
    }

    String expected = services.roomPassword();
    if (expected != null && !expected.equals(password)) {
      session.send(
          w -> {
            w.writeOpcode(OpCode.S_ACK_LOGIN);
            w.writeBoolean(false);
            w.writeUtf8("Oda şifresi hatalı veya eksik.");
          });
      services.logger().warn("Hatalı oda şifresi denemesi: " + name);
      return;
    }

    session.setUsername(name);
    if (!services.registry().register(name, session)) {
      session.setUsername("");
      session.send(
          w -> {
            w.writeOpcode(OpCode.S_ACK_LOGIN);
            w.writeBoolean(false);
            w.writeUtf8("Bu kullanıcı adı kullanımda.");
          });
      return;
    }
    session.setAuthenticated(true);
    session.send(
        w -> {
          w.writeOpcode(OpCode.S_ACK_LOGIN);
          w.writeBoolean(true);
          w.writeUtf8("Bağlantı kabul edildi. Hoş geldiniz, " + name + ".");
        });

    try {
      broadcaster.sendChatHistoryTo(session, services.chatHistory().snapshot());
    } catch (IOException e) {
      services.logger().warn("Geçmiş mesajlar gönderilemedi: " + e.getMessage());
    }

    services.logger().info("Yeni istemci bağlandı: " + name + " (" + session.remoteAddress() + ")");
    broadcaster.notifyOthers(name, "USER_JOINED|" + name);
    broadcaster.sendLogToAll("[" + name + "] sohbete katıldı.");
    broadcaster.broadcastUserList();
  }
}
