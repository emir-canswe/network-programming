package chat.server.logging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** Günlük satırlarını dosyaya ekler. */
public final class FileLogSink implements LogSink {

  private final Path path;

  public FileLogSink(Path path) {
    this.path = path;
  }

  @Override
  public void append(String line) {
    try {
      Files.writeString(
          path,
          line + System.lineSeparator(),
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND);
    } catch (IOException ignored) {
    }
  }
}
