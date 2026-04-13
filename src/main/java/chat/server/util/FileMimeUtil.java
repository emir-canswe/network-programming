package chat.server.util;

/** Basit uzantı → MIME (gösterim amaçlı). */
public final class FileMimeUtil {

  private FileMimeUtil() {}

  public static String guessMime(String filename) {
    if (filename == null) {
      return "application/octet-stream";
    }
    String n = filename.toLowerCase();
    if (n.endsWith(".pdf")) {
      return "application/pdf";
    }
    if (n.endsWith(".png")) {
      return "image/png";
    }
    if (n.endsWith(".jpg") || n.endsWith(".jpeg")) {
      return "image/jpeg";
    }
    if (n.endsWith(".gif")) {
      return "image/gif";
    }
    if (n.endsWith(".webp")) {
      return "image/webp";
    }
    if (n.endsWith(".txt") || n.endsWith(".md")) {
      return "text/plain";
    }
    if (n.endsWith(".zip")) {
      return "application/zip";
    }
    if (n.endsWith(".json")) {
      return "application/json";
    }
    return "application/octet-stream";
  }
}
