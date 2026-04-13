package chat.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Küçük protokol doğrulaması — IDE'den veya {@code java -cp ... chat.protocol.ProtocolSelfTest}
 * ile çalıştırılabilir.
 */
public final class ProtocolSelfTest {

  private ProtocolSelfTest() {}

  public static void main(String[] args) throws IOException {
    roundTripUtf8();
    roundTripLongAndOpcode();
    System.out.println("ProtocolSelfTest: OK");
  }

  private static void roundTripUtf8() throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ProtocolWriter w = new ProtocolWriter(bos);
    w.writeUtf8("merhaba-ğüşiöç");
    w.flush();
    ProtocolReader r = new ProtocolReader(new ByteArrayInputStream(bos.toByteArray()));
    String s = r.readUtf8();
    if (!"merhaba-ğüşiöç".equals(s)) {
      throw new AssertionError("UTF-8 uyuşmazlığı");
    }
  }

  private static void roundTripLongAndOpcode() throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    ProtocolWriter w = new ProtocolWriter(bos);
    w.writeOpcode(OpCode.C_CHAT);
    w.writeLong(42L);
    w.flush();
    ProtocolReader r = new ProtocolReader(new ByteArrayInputStream(bos.toByteArray()));
    if (r.readOpcode() != OpCode.C_CHAT) {
      throw new AssertionError("opcode");
    }
    if (r.readLong() != 42L) {
      throw new AssertionError("long");
    }
  }
}
