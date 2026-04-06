package chat.protocol;

import java.io.IOException;

/** İstemci bağlantısına yazılacak protokol parçası (IOException ile uyumlu). */
@FunctionalInterface
public interface ProtocolPayload {
  void write(ProtocolWriter writer) throws IOException;
}
