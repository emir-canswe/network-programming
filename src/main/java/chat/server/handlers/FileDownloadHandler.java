package chat.server.handlers;

import chat.protocol.OpCode;
import chat.server.ChatServerServices;
import chat.server.ServerLimits;
import chat.server.file.StoredFileRecord;
import chat.server.session.ClientSession;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;

public final class FileDownloadHandler implements ClientRequestHandler {

  private final ChatServerServices services;

  public FileDownloadHandler(ChatServerServices services) {
    this.services = services;
  }

  @Override
  public void handle(ClientSession session) throws IOException {
    String fileId = session.protocolReader().readUtf8();
    Optional<StoredFileRecord> opt = services.fileStore().find(fileId);
    if (opt.isEmpty()) {
      session.sendError("Dosya bulunamadı veya süresi doldu: " + fileId);
      return;
    }
    StoredFileRecord rec = opt.get();
    if (rec.sizeBytes() > ServerLimits.MAX_FILE_BYTES) {
      session.sendError("Dosya sunucu sınırını aşıyor.");
      return;
    }
    byte[] bytes = Files.readAllBytes(rec.storagePath());
    session.send(
        w -> {
          w.writeOpcode(OpCode.S_FILE_PAYLOAD);
          w.writeUtf8(rec.originalFilename());
          w.writeLong(rec.sizeBytes());
          w.writeFully(bytes, 0, bytes.length);
        });
    services
        .logger()
        .info(
            "Dosya indirme: "
                + session.getUsername()
                + " <- "
                + rec.originalFilename()
                + " ("
                + rec.sharedByUsername()
                + ")");
  }
}
