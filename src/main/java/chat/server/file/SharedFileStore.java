package chat.server.file;

import java.io.IOException;
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
