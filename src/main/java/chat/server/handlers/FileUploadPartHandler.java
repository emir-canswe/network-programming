package chat.server.handlers;

import chat.server.ChatServerServices;
import chat.server.ServerLimits;
import chat.server.session.ClientSession;
import java.io.IOException;

public final class FileUploadPartHandler implements ClientRequestHandler {

  private final ChatServerServices services;

  public FileUploadPartHandler(ChatServerServices services) {
    this.services = services;
  }

  @Override
  public void handle(ClientSession session) throws IOException {
    ClientSession.PendingFileUpload p = session.pendingUpload();
    if (p == null) {
      session.sendError("Önce C_FILE_UPLOAD_BEGIN gönderin.");
      return;
    }
    session.protocolReader().readInt(); /* sıra numarası — şimdilik yok sayılıyor */
    byte[] chunk;
    try {
      chunk =
          session.protocolReader().readByteBlock(ServerLimits.MAX_UPLOAD_CHUNK_BYTES, null);
    } catch (IOException e) {
      session.clearPendingUpload();
      session.sendError("Parça okunamadı: " + e.getMessage());
      return;
    }
    if (p.buffer.size() + chunk.length > p.totalBytes) {
      session.clearPendingUpload();
      session.sendError("Toplam boyut aşıldı.");
      return;
    }
    p.buffer.write(chunk, 0, chunk.length);
  }
}
