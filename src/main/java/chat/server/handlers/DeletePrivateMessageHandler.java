package chat.server.handlers;

import chat.server.ChatServerServices;
import chat.server.ChatServerServices.TrackedPrivateMessage;
import chat.server.UserListBroadcaster;
import chat.server.session.ClientSession;
import java.io.IOException;

public final class DeletePrivateMessageHandler implements ClientRequestHandler {

  private final ChatServerServices services;
  private final UserListBroadcaster broadcaster;

  public DeletePrivateMessageHandler(ChatServerServices services, UserListBroadcaster broadcaster) {
    this.services = services;
    this.broadcaster = broadcaster;
  }

  @Override
  public void handle(ClientSession session) throws IOException {
    long messageId = session.protocolReader().readLong();
    TrackedPrivateMessage meta = services.lookupPrivateMessage(messageId).orElse(null);
    if (meta == null) {
      session.sendError("Mesaj bulunamadı veya zaten silindi.");
      return;
    }
    if (!meta.fromUser().equalsIgnoreCase(session.getUsername())) {
      session.sendError("Bu mesajı yalnızca gönderen silebilir.");
      return;
    }
    services.forgetPrivateMessage(messageId);
    broadcaster.notifyPrivateMessageDeleted(meta.fromUser(), meta.toUser(), messageId);
    services.logger().info("ÖZEL_MESAJ_SIL id=" + messageId + " " + session.getUsername());
  }
}
