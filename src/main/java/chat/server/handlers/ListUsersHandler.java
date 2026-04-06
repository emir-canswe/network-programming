package chat.server.handlers;

import chat.server.ChatServerServices;
import chat.server.UserListBroadcaster;
import chat.server.session.ClientSession;
import java.io.IOException;

public final class ListUsersHandler implements ClientRequestHandler {

  private final ChatServerServices services;
  private final UserListBroadcaster broadcaster;

  public ListUsersHandler(ChatServerServices services, UserListBroadcaster broadcaster) {
    this.services = services;
    this.broadcaster = broadcaster;
  }

  @Override
  public void handle(ClientSession session) {
    try {
      broadcaster.sendUserListTo(session);
      services.logger().info("LIST_USERS: " + session.getUsername());
    } catch (IOException e) {
      services.logger().warn("Kullanıcı listesi gönderilemedi: " + e.getMessage());
    }
  }
}
