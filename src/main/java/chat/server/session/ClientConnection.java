package chat.server.session;

import chat.protocol.ProtocolPayload;
import java.io.IOException;

/**
 * Bağlı istemciye mesaj göndermek için soyutlama (DIP).
 */
public interface ClientConnection {
  String getUsername();

  boolean isAuthenticated();

  void send(ProtocolPayload payload) throws IOException;

  void close();
}
