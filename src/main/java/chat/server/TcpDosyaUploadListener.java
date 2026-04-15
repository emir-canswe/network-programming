package chat.server;

import chat.server.file.StoredFileRecord;
import chat.server.logging.TimestampedLogger;
import chat.server.util.FileMimeUtil;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Ders materyalindeki gibi {@link DataInputStream}: önce UTF-8 alanlar, ardından ham bayt akışı.
 * Sohbet protokolünden ayrı bir TCP portunda çalışır (sohbet portu + 1).
 */
public final class TcpDosyaUploadListener {

  private TcpDosyaUploadListener() {}

  public static void handleUpload(
      Socket socket, ChatServerServices services, UserListBroadcaster broadcaster, TimestampedLogger logger) {
    try (socket) {
      DataInputStream dis = new DataInputStream(socket.getInputStream());
      String username = dis.readUTF().trim();
      String roomRaw = dis.readUTF();
      String filenameRaw = dis.readUTF();
      if (username.length() < 2) {
        logger.warn("Dosya yükleme: geçersiz kullanıcı adı");
        return;
      }
      if (services.isBanned(username)) {
        logger.warn("Dosya yükleme reddedildi (engelli): " + username);
        return;
      }
      String room = ChatServerServices.sanitizeRoomName(roomRaw);
      String filename = sanitizeFilename(filenameRaw);
      if (filename.isBlank()) {
        logger.warn("Dosya yükleme: boş dosya adı");
        return;
      }
      StoredFileRecord rec =
          services
              .fileStore()
              .saveStreamToDisk(username, filename, dis, ServerLimits.MAX_FILE_BYTES);
      services.addFileBytesStored(rec.sizeBytes());
      String mime = FileMimeUtil.guessMime(filename);
      broadcaster.announceFile(null, username, rec.id(), filename, rec.sizeBytes(), mime, room);
      services
          .registry()
          .find(username)
          .ifPresent(
              c -> {
                try {
                  broadcaster.announceFileToSelf(
                      c, username, rec.id(), filename, rec.sizeBytes(), mime);
                } catch (IOException ignored) {
                }
              });
      logger.info("TCP_DOSYA " + username + " " + filename + " " + rec.sizeBytes() + " B oda=" + room);
    } catch (IOException e) {
      services.logger().warn("TCP dosya yükleme hatası: " + e.getMessage());
    }
  }

  private static String sanitizeFilename(String raw) {
    if (raw == null) {
      return "";
    }
    String s = raw.replace('\\', '/').trim();
    int slash = s.lastIndexOf('/');
    if (slash >= 0) {
      s = s.substring(slash + 1).trim();
    }
    if (s.isBlank() || s.length() > 240) {
      return "";
    }
    return s;
  }
}
