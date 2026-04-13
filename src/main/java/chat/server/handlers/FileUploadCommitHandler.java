package chat.server.handlers;

import chat.server.ChatServerServices;
import chat.server.UserListBroadcaster;
import chat.server.file.StoredFileRecord;
import chat.server.session.ClientSession;
import java.io.IOException;

public final class FileUploadCommitHandler implements ClientRequestHandler {

  private final ChatServerServices services;
  private final UserListBroadcaster broadcaster;

  public FileUploadCommitHandler(ChatServerServices services, UserListBroadcaster broadcaster) {
    this.services = services;
    this.broadcaster = broadcaster;
  }

  @Override
  public void handle(ClientSession session) throws IOException {
    ClientSession.PendingFileUpload p = session.pendingUpload();
    if (p == null) {
      session.sendError("Yüklenecek dosya yok.");
      return;
    }
    if (p.buffer.size() != p.totalBytes) {
      session.clearPendingUpload();
      session.sendError(
          "Veri eksik veya fazla: beklenen "
              + p.totalBytes
              + " bayt, gelen "
              + p.buffer.size());
      return;
    }
    byte[] data = p.buffer.toByteArray();
    session.clearPendingUpload();
    StoredFileRecord rec;
    try {
      rec = services.fileStore().saveInMemoryAndDisk(session.getUsername(), p.filename, data);
    } catch (IOException e) {
      session.sendError("Dosya sunucuda saklanamadı: " + e.getMessage());
      services.logger().error("Parçalı dosya kaydı: " + e.getMessage());
      return;
    }
    String room = session.getRoom();
    services.addFileBytesStored(data.length);
    broadcaster.announceFile(
        session,
        session.getUsername(),
        rec.id(),
        p.filename,
        rec.sizeBytes(),
        p.mime,
        room);
    broadcaster.announceFileToSelf(
        session,
        session.getUsername(),
        rec.id(),
        p.filename,
        rec.sizeBytes(),
        p.mime);
    broadcaster.sendLogToRoom(
        room,
        "["
            + session.getUsername()
            + "] parçalı dosya paylaştı: "
            + p.filename
            + " ("
            + data.length
            + " bayt)");
    services.logger().info("FILE_UPLOAD_COMMIT " + session.getUsername() + " -> " + p.filename);
  }
}
