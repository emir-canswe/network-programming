package chat.server.handlers;

import chat.protocol.OpCode;
import chat.server.session.ClientSession;
import java.io.IOException;

public final class PingHandler implements ClientRequestHandler {

  @Override
  public void handle(ClientSession session) throws IOException {
    session.send(w -> w.writeOpcode(OpCode.S_PONG));
  }
}
