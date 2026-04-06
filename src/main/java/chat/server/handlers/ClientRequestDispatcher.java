package chat.server.handlers;

import chat.server.ChatServerServices;
import chat.server.UserListBroadcaster;
import chat.server.session.ClientSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Komut → işleyici eşlemesi (SRP: sadece yönlendirme).
 */
public final class ClientRequestDispatcher {

  private final Map<Integer, ClientRequestHandler> handlers = new HashMap<>();

  public ClientRequestDispatcher(ChatServerServices services, UserListBroadcaster broadcaster) {
    register(chat.protocol.OpCode.C_LOGIN, new LoginHandler(services, broadcaster));
    register(chat.protocol.OpCode.C_CHAT, new ChatMessageHandler(services, broadcaster));
    register(chat.protocol.OpCode.C_LOGOUT, new LogoutHandler(services));
    register(chat.protocol.OpCode.C_FILE_OFFER, new FileOfferHandler(services, broadcaster));
    register(chat.protocol.OpCode.C_FILE_REQUEST, new FileDownloadHandler(services));
    register(chat.protocol.OpCode.C_PING, new PingHandler());
    register(
        chat.protocol.OpCode.C_LIST_USERS, new ListUsersHandler(services, broadcaster));
  }

  private void register(int op, ClientRequestHandler h) {
    handlers.put(op, h);
  }

  public void dispatch(int op, ClientSession session) throws IOException {
    ClientRequestHandler h = handlers.get(op);
    if (h == null) {
      session.sendError("Bilinmeyen komut: " + op);
      return;
    }
    h.handle(session);
  }
}
