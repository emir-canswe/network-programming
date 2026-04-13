package chat.server.handlers;

import chat.server.ChatServerServices;
import chat.server.ServerLimits;
import chat.server.session.ClientSession;
import chat.server.util.FileMimeUtil;
import java.io.IOException;

public final class FileUploadBeginHandler implements ClientRequestHandler {

  private final ChatServerServices services;

  public FileUploadBeginHandler(ChatServerServices services) {
    this.services = services;
  }

  @Override
  public void handle(ClientSession session) throws IOException {
    if (session.pendingUpload() != null) {
      session.sendError("Devam eden dosya yüklemesi var. Önce iptal (C_FILE_CANCEL) veya tamamlayın.");
      return;
    }
    String filename = session.protocolReader().readUtf8().trim();
    long total = session.protocolReader().readLong();
    String mime = session.protocolReader().readUtf8();
    if (filename.isBlank() || total <= 0 || total > ServerLimits.MAX_FILE_BYTES) {
      session.sendError("Geçersiz dosya başlığı.");
      return;
    }
    ClientSession.PendingFileUpload p = new ClientSession.PendingFileUpload();
    p.filename = filename;
    p.totalBytes = total;
    p.mime =
        mime != null && !mime.isBlank() ? mime.trim() : FileMimeUtil.guessMime(filename);
    session.setPendingUpload(p);
    services.logger().info("FILE_UPLOAD_BEGIN " + session.getUsername() + " " + filename + " " + total);
  }
}
