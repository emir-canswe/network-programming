package chat.server.handlers;

import chat.server.ChatServerServices;
import chat.server.PrivateMessageParser;
import chat.server.ServerLimits;
import chat.server.UserListBroadcaster;
import chat.server.registry.OnlineUserRegistry;
import chat.server.session.ClientConnection;
import chat.server.session.ClientSession;
import java.io.IOException;
import java.util.Optional;

public final class ChatMessageHandler implements ClientRequestHandler {

  private final ChatServerServices services;
  private final UserListBroadcaster broadcaster;

  public ChatMessageHandler(ChatServerServices services, UserListBroadcaster broadcaster) {
    this.services = services;
    this.broadcaster = broadcaster;
  }

  @Override
  public void handle(ClientSession session) throws IOException {
    String text = session.protocolReader().readUtf8();
    if (text.isBlank()) {
      session.sendError("Boş mesaj gönderilemez.");
      return;
    }
    if (text.length() > ServerLimits.MAX_CHAT_CHARS) {
      session.sendError("Mesaj çok uzun (en fazla " + ServerLimits.MAX_CHAT_CHARS + " karakter).");
      return;
    }
    long now = System.currentTimeMillis();
    OnlineUserRegistry reg = services.registry();
    Optional<PrivateMessageParser.TargetAndBody> pm = PrivateMessageParser.parse(text);
    if (pm.isPresent()) {
      String targetName = pm.get().targetUsername();
      String body = pm.get().body();
      Optional<ClientConnection> target = reg.find(targetName);
      if (target.isEmpty()) {
        session.sendError("Kullanıcı çevrimiçi değil veya bulunamadı: " + targetName);
        services
            .logger()
            .info("Özel mesaj hedefi yok: " + session.getUsername() + " -> " + targetName);
        return;
      }
      long pid = services.nextPrivateMessageId();
      services.rememberPrivateMessage(pid, session.getUsername(), targetName);
      broadcaster.sendPrivate(target.get(), session.getUsername(), now, pid, body);
      broadcaster.sendPrivate(session, session.getUsername(), now, pid, body);
      services.incPrivateCount();
      services
          .logger()
          .info("Özel mesaj: " + session.getUsername() + " -> " + targetName);
      return;
    }
    String room = session.getRoom();
    long msgId = services.nextBroadcastMessageId();
    services.rememberBroadcastMessage(msgId, room, session.getUsername());
    broadcaster.broadcastChat(session.getUsername(), now, msgId, text, room);
    services.incBroadcastCount();
    services.logger().info("Genel mesaj: " + session.getUsername() + " oda=" + room);
  }
}
