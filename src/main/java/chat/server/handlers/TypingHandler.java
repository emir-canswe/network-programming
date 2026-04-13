package chat.server.handlers;

import chat.server.UserListBroadcaster;
import chat.server.session.ClientSession;
import java.io.IOException;

public final class TypingHandler implements ClientRequestHandler {

  private final UserListBroadcaster broadcaster;

  public TypingHandler(UserListBroadcaster broadcaster) {
    this.broadcaster = broadcaster;
  }

  @Override
  public void handle(ClientSession session) throws IOException {
    boolean started = session.protocolReader().readBoolean();
    broadcaster.sendTypingToRoom(
        session.getUsername(), session.getRoom(), session.getUsername(), started);
  }
}
