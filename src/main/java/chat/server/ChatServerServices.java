package chat.server;

import chat.server.file.SharedFileStore;
import chat.server.logging.TimestampedLogger;
import chat.server.registry.OnlineUserRegistry;

/**
 * Sunucu alt sistemlerine tek giriş noktası (Facade + DI için uygun yapı).
 */
public final class ChatServerServices {

  private final OnlineUserRegistry registry;
  private final SharedFileStore fileStore;
  private final TimestampedLogger logger;
  private final ChatHistoryBuffer chatHistory = new ChatHistoryBuffer(50);
  private volatile String roomPassword;

  public ChatServerServices(
      OnlineUserRegistry registry, SharedFileStore fileStore, TimestampedLogger logger) {
    this.registry = registry;
    this.fileStore = fileStore;
    this.logger = logger;
  }

  public OnlineUserRegistry registry() {
    return registry;
  }

  public SharedFileStore fileStore() {
    return fileStore;
  }

  public TimestampedLogger logger() {
    return logger;
  }

  public ChatHistoryBuffer chatHistory() {
    return chatHistory;
  }

  /** Boş veya null → şifre yok. */
  public void setRoomPassword(String password) {
    roomPassword =
        (password == null || password.isBlank()) ? null : password.trim();
  }

  public String roomPassword() {
    return roomPassword;
  }
}
