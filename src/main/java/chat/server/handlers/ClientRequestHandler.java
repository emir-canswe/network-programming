package chat.server.handlers;

import chat.server.session.ClientSession;
import java.io.IOException;

/**
 * Open/Closed: yeni komut = yeni handler sınıfı, mevcut kodu bozmadan genişleme.
 */
@FunctionalInterface
public interface ClientRequestHandler {
  void handle(ClientSession session) throws IOException;
}
