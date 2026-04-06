package chat.server.logging;

/**
 * Arayüz ayrımı (ISP): log tüketicileri sadece eklemeyi bilir.
 */
@FunctionalInterface
public interface LogSink {
  void append(String line);
}
