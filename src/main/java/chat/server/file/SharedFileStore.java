package chat.server.file;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Paylaşılan dosyaları diskte tutar ve kimlik ile erişim sağlar (SRP).
 */
public final class SharedFileStore {

  private final Path baseDir;
  private final Map<String, StoredFileRecord> byId = new ConcurrentHashMap<>();

  public SharedFileStore(Path baseDir) throws IOException {
    this.baseDir = baseDir;
    Files.createDirectories(baseDir);
  }

  public StoredFileRecord saveInMemoryAndDisk(String fromUser, String filename, byte[] data)
      throws IOException {
    String id = UUID.randomUUID().toString();
    Path target = baseDir.resolve(id + ".bin");
    Files.write(target, data);
    StoredFileRecord rec =
        new StoredFileRecord(id, fromUser, filename, data.length, target);
    byId.put(id, rec);
    return rec;
  }

  /**
   * TCP akışından (ör. {@link java.io.DataInputStream}) dosyayı okuyup diske yazar — ders örneğindeki
   * {@code while ((n = read(buffer)) > 0) write} deseninin sunucu tarafı.
   */
  public StoredFileRecord saveStreamToDisk(
      String fromUser, String logicalFilename, InputStream in, long maxBytes) throws IOException {
    String id = UUID.randomUUID().toString();
    Path target = baseDir.resolve(id + ".bin");
    long total = 0;
    byte[] buf = new byte[4096];
    try (OutputStream os = Files.newOutputStream(target)) {
      int n;
      while ((n = in.read(buf)) > 0) {
        total += n;
        if (total > maxBytes) {
          try {
            Files.deleteIfExists(target);
          } catch (IOException ignored) {
          }
          throw new IOException("Dosya boyutu sınırı aşıldı.");
        }
        os.write(buf, 0, n);
      }
    }
    StoredFileRecord rec = new StoredFileRecord(id, fromUser, logicalFilename, total, target);
    byId.put(id, rec);
    return rec;
  }

  public Optional<StoredFileRecord> find(String id) {
    return Optional.ofNullable(byId.get(id));
  }

  public void remove(String id) {
    StoredFileRecord r = byId.remove(id);
    if (r != null) {
      try {
        Files.deleteIfExists(r.storagePath());
      } catch (IOException ignored) {
      }
    }
  }
}
