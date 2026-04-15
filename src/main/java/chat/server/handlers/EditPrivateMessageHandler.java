package chat.server.handlers;

import chat.server.ChatServerServices;
import chat.server.ChatServerServices.TrackedPrivateMessage;
import chat.server.ServerLimits;
import chat.server.UserListBroadcaster;
import chat.server.session.ClientSession;
import java.io.IOException;

public final class EditPrivateMessageHandler implements ClientRequestHandler {

  private final ChatServerServices services;
  private final UserListBroadcaster broadcaster;

  public EditPrivateMessageHandler(ChatServerServices services, UserListBroadcaster broadcaster) {
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
    TrackedPrivateMessage meta = services.lookupPrivateMessage(messageId).orElse(null);
    if (meta == null) {
      session.sendError("Mesaj bulunamadı veya süresi doldu.");
      return;
    }
    if (!meta.fromUser().equalsIgnoreCase(session.getUsername())) {
      session.sendError("Bu mesajı yalnızca gönderen düzenleyebilir.");
      return;
    }
    long now = System.currentTimeMillis();
    broadcaster.notifyPrivateMessageEdited(
        meta.fromUser(), meta.toUser(), now, messageId, newText.trim());
    services.logger().info("ÖZEL_MESAJ_DUZEN id=" + messageId + " " + session.getUsername());
  }
}
