package chat.server;

import java.util.Optional;

/**
 * @kullanici mesaj ayrıştırması — tek sorumluluk.
 */
public final class PrivateMessageParser {

  private PrivateMessageParser() {}

  public record TargetAndBody(String targetUsername, String body) {}

  /**
   * Mesaj "@hedef bir şeyler" ile başlıyorsa hedef kullanıcı adı ve gövdeyi döner.
   */
  public static Optional<TargetAndBody> parse(String raw) {
    if (raw == null) {
      return Optional.empty();
    }
    String s = raw.stripLeading();
    if (!s.startsWith("@")) {
      return Optional.empty();
    }
    int space = s.indexOf(' ');
    if (space <= 1) {
      return Optional.empty();
    }
    String target = s.substring(1, space).trim();
    String body = s.substring(space + 1).trim();
    if (target.isEmpty() || body.isEmpty()) {
      return Optional.empty();
    }
    if (!target.matches("[a-zA-Z0-9_\\-ğüşıöçĞÜŞİÖÇ]{2,24}")) {
      return Optional.empty();
    }
    return Optional.of(new TargetAndBody(target, body));
  }
}
