package chat.server.handlers;

import chat.server.ChatServerServices;
import chat.server.ChatServerServices.TrackedBroadcastMessage;
import chat.server.ServerLimits;
import chat.server.UserListBroadcaster;
import chat.server.session.ClientSession;
import java.io.IOException;

public final class EditBroadcastMessageHandler implements ClientRequestHandler {

  private final ChatServerServices services;
  private final UserListBroadcaster broadcaster;

  public EditBroadcastMessageHandler(ChatServerServices services, UserListBroadcaster broadcaster) {
    this.services = services;
    this.broadcaster = broadcaster;
  }

  @Override
  public void handle(ClientSession session) throws IOException {
    long messageId = session.protocolReader().readLong();
    String newText = session.protocolReader().readUtf8();
    if (newText == null || newText.isBlank()) {
      session.sendError("Düzenlenmiş metin boş olamaz.");
      return;
    }
    if (newText.length() > ServerLimits.MAX_CHAT_CHARS) {
      session.sendError("Metin çok uzun (en fazla " + ServerLimits.MAX_CHAT_CHARS + " karakter).");
      return;
    }
    TrackedBroadcastMessage meta = services.lookupBroadcastMessage(messageId).orElse(null);
    if (meta == null) {
      session.sendError("Mesaj bulunamadı veya süresi doldu.");
      return;
    }
    if (!meta.author().equalsIgnoreCase(session.getUsername())) {
      session.sendError("Bu mesajı yalnızca gönderen düzenleyebilir.");
      return;
    }
    if (!meta.room().equals(session.getRoom())) {
      session.sendError("Bu mesaj bu odada değil.");
      return;
    }
    long now = System.currentTimeMillis();
    broadcaster.broadcastChatEdit(
        session.getUsername(), now, messageId, newText.trim(), meta.room());
    services.logger().info("MESAJ_DUZEN oda=" + meta.room() + " id=" + messageId + " " + session.getUsername());
  }
}
