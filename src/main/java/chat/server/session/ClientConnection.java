package chat.server.session;

import chat.protocol.ProtocolPayload;
import java.io.IOException;

/**
 * Bağlı istemciye mesaj göndermek için soyutlama (DIP).
 */
public interface ClientConnection {
  String getUsername();

  boolean isAuthenticated();

  /** Sohbet odası / kanal (varsayılan: genel). */
  String getRoom();

  void setRoom(String room);

  void send(ProtocolPayload payload) throws IOException;

  void close();
}
