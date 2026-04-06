package chat.server.logging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * Tek sorumluluk: zaman damgalı metin üretmek ve dinleyicilere iletmek.
 */
public final class TimestampedLogger {

  private static final DateTimeFormatter FMT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final List<LogSink> sinks = new CopyOnWriteArrayList<>();

  public void addSink(LogSink sink) {
    sinks.add(Objects.requireNonNull(sink));
  }

  public void removeSink(LogSink sink) {
    sinks.remove(sink);
  }

  public void info(String message) {
    emit("INFO", message);
  }

  public void warn(String message) {
    emit("WARN", message);
  }

  public void error(String message) {
    emit("ERROR", message);
  }

  private void emit(String level, String message) {
    String line = "[" + LocalDateTime.now().format(FMT) + "] [" + level + "] " + message;
    for (LogSink s : sinks) {
      try {
        s.append(line);
      } catch (RuntimeException ignored) {
        // UI güncellemesi vb. hatalarda sunucu çökmez
      }
    }
  }
}
