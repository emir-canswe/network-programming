package chat.server.file;

import java.nio.file.Path;

public record StoredFileRecord(
    String id,
    String sharedByUsername,
    String originalFilename,
    long sizeBytes,
    Path storagePath) {}
