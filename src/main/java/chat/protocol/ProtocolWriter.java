package chat.protocol;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Tek sorumluluk: protokol çerçevesine uygun yazma (DIP: soyut akış üzerine).
 */
public final class ProtocolWriter {

  private final DataOutputStream out;

  public ProtocolWriter(OutputStream raw) {
    this.out = new DataOutputStream(raw);
  }

  public void writeOpcode(int op) throws IOException {
    out.writeByte(op & 0xFF);
  }

  public void writeUtf8(String s) throws IOException {
    byte[] b = s.getBytes(StandardCharsets.UTF_8);
    if (b.length > 65535) {
      throw new IOException("Metin çok uzun (UTF-8 > 65535 bayt)");
    }
    out.writeShort(b.length);
    out.write(b);
  }

  public void writeLong(long v) throws IOException {
    out.writeLong(v);
  }

  public void writeInt(int v) throws IOException {
    out.writeInt(v);
  }

  public void writeBoolean(boolean v) throws IOException {
    out.writeBoolean(v);
  }

  public void writeFully(byte[] data, int off, int len) throws IOException {
    out.writeInt(len);
    out.write(data, off, len);
  }

  public void flush() throws IOException {
    out.flush();
  }

  public DataOutputStream underlying() {
    return out;
  }
}
