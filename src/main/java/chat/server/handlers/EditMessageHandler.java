package chat.server.handlers;

import chat.server.ChatServerServices;
import chat.server.ChatHistoryBuffer;
import chat.server.UserListBroadcaster;
import chat.server.session.ClientSession;
import java.io.IOException;
import java.util.Optional;

public final class EditMessageHandler implements ClientRequestHandler {

  private final ChatServerServices services;
  private final UserListBroadcaster broadcaster;

  public EditMessageHandler(ChatServerServices services, UserListBroadcaster broadcaster) {
    this.services = services;
    this.broadcaster = broadcaster;
  }

  @Override
  public void handle(ClientSession session) throws IOException {
    long messageId = session.protocolReader().readLong();
    String newText = session.protocolReader().readUtf8();
    Optional<ChatHistoryBuffer.Entry> updated =
        services.chatHistory().tryEdit(messageId, session.getUsername(), newText);
    if (updated.isEmpty()) {
      session.sendError("Mesaj düzenlenemedi (süre, sahiplik veya bulunamadı).");
      return;
    }
    broadcaster.broadcastMessageEdited(
        session.getRoom(), messageId, updated.get().text(), session.getUsername());
    services.logger().info("EDIT " + session.getUsername() + " msg=" + messageId);
  }
}
