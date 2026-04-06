package chat.server.handlers;

import chat.server.ChatServerServices;
import chat.server.ServerLimits;
import chat.server.UserListBroadcaster;
import chat.server.file.StoredFileRecord;
import chat.server.session.ClientSession;
import java.io.IOException;

public final class FileOfferHandler implements ClientRequestHandler {

  private final ChatServerServices services;
  private final UserListBroadcaster broadcaster;

  public FileOfferHandler(ChatServerServices services, UserListBroadcaster broadcaster) {
    this.services = services;
    this.broadcaster = broadcaster;
  }

  @Override
  public void handle(ClientSession session) throws IOException {
    String filename = session.protocolReader().readUtf8();
    if (filename.isBlank()) {
      session.sendError("Dosya adı boş olamaz.");
      return;
    }
    byte[] data;
    try {
      data = session.protocolReader().readByteBlock(ServerLimits.MAX_FILE_BYTES, null);
    } catch (IOException e) {
      session.sendError(
          "Dosya çok büyük veya geçersiz (en fazla "
              + (ServerLimits.MAX_FILE_BYTES / (1024 * 1024))
              + " MB).");
      services.logger().warn("Dosya reddedildi: " + e.getMessage());
      return;
    }
    StoredFileRecord rec;
    try {
      rec = services.fileStore().saveInMemoryAndDisk(session.getUsername(), filename, data);
    } catch (IOException e) {
      session.sendError("Dosya sunucuda saklanamadı: " + e.getMessage());
      services.logger().error("Dosya kaydı hatası: " + e.getMessage());
      return;
    }
    broadcaster.announceFile(session, session.getUsername(), rec.id(), filename, rec.sizeBytes());
    broadcaster.announceFileToSelf(
        session, session.getUsername(), rec.id(), filename, rec.sizeBytes());
    broadcaster.sendLogToAll(
        "[" + session.getUsername() + "] dosya paylaştı: " + filename + " (" + data.length + " bayt)");
    services.logger().info("Dosya paylaşımı: " + session.getUsername() + " -> " + filename);
  }
}
