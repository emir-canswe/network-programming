package chat.server.handlers;

import chat.server.ChatServerServices;
import chat.server.UserListBroadcaster;
import chat.server.session.ClientSession;
import java.io.IOException;

public final class DeleteMessageHandler implements ClientRequestHandler {

  private final ChatServerServices services;
  private final UserListBroadcaster broadcaster;

  public DeleteMessageHandler(ChatServerServices services, UserListBroadcaster broadcaster) {
    this.services = services;
    this.broadcaster = broadcaster;
  }

  @Override
  public void handle(ClientSession session) throws IOException {
    long messageId = session.protocolReader().readLong();
    if (!services.chatHistory().tryDelete(messageId, session.getUsername())) {
      session.sendError("Mesaj silinemedi (süre, sahiplik veya bulunamadı).");
      return;
    }
    broadcaster.broadcastMessageDeleted(session.getRoom(), messageId);
    services.logger().info("DELETE " + session.getUsername() + " msg=" + messageId);
  }
}
