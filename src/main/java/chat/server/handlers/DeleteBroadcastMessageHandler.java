package chat.server.handlers;

import chat.server.ChatServerServices;
import chat.server.ChatServerServices.TrackedBroadcastMessage;
import chat.server.UserListBroadcaster;
import chat.server.session.ClientSession;
import java.io.IOException;

public final class DeleteBroadcastMessageHandler implements ClientRequestHandler {

  private final ChatServerServices services;
  private final UserListBroadcaster broadcaster;

  public DeleteBroadcastMessageHandler(ChatServerServices services, UserListBroadcaster broadcaster) {
    this.services = services;
    this.broadcaster = broadcaster;
  }

  @Override
  public void handle(ClientSession session) throws IOException {
    long messageId = session.protocolReader().readLong();
    TrackedBroadcastMessage meta = services.lookupBroadcastMessage(messageId).orElse(null);
    if (meta == null) {
      session.sendError("Mesaj bulunamadı veya zaten silindi.");
      return;
    }
    if (!meta.author().equalsIgnoreCase(session.getUsername())) {
      session.sendError("Bu mesajı yalnızca gönderen silebilir.");
      return;
    }
    if (!meta.room().equals(session.getRoom())) {
      session.sendError("Bu mesaj bu odada değil.");
      return;
    }
    services.forgetBroadcastMessage(messageId);
    broadcaster.broadcastChatDelete(messageId, meta.room());
    services.logger().info("MESAJ_SIL oda=" + meta.room() + " id=" + messageId + " " + session.getUsername());
  }
}
