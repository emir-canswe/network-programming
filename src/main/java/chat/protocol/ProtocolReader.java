package chat.protocol;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Tek sorumluluk: protokolden okuma.
 */
public final class ProtocolReader {

  public static final int ABSOLUTE_MAX_BLOCK = 256 * 1024 * 1024;

  private final DataInputStream in;

  public ProtocolReader(InputStream raw) {
    this.in = new DataInputStream(raw);
  }

  public int readOpcode() throws IOException {
    return in.readUnsignedByte();
  }

  public String readUtf8() throws IOException {
    int n = in.readUnsignedShort();
    byte[] b = new byte[n];
    in.readFully(b);
    return new String(b, StandardCharsets.UTF_8);
  }

  public long readLong() throws IOException {
    return in.readLong();
  }

  public int readInt() throws IOException {
    return in.readInt();
  }

  public boolean readBoolean() throws IOException {
    return in.readBoolean();
  }

  public byte[] readByteBlock() throws IOException {
    return readByteBlock(ABSOLUTE_MAX_BLOCK, null);
  }

  public byte[] readByteBlock(int maxBytesInclusive, ByteBlockProgressListener progress)
      throws IOException {
    int len = in.readInt();
    if (len < 0 || len > maxBytesInclusive) {
      throw new IOException("Geçersiz veya çok büyük blok boyutu: " + len);
    }
    byte[] buf = new byte[len];
    int pos = 0;
    int lastPct = -1;
    while (pos < len) {
      int n = in.read(buf, pos, len - pos);
      if (n < 0) {
        throw new EOFException();
      }
      pos += n;
      if (progress != null && len > 0) {
        int pct = (int) ((100L * pos) / len);
        if (pct != lastPct) {
          progress.onProgress(pct, pos, len);
          lastPct = pct;
        }
      }
    }
    return buf;
  }

  public DataInputStream underlying() {
    return in;
  }
}
