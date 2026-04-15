package chat.server;

import chat.server.file.SharedFileStore;
import chat.server.logging.TimestampedLogger;
import chat.server.registry.OnlineUserRegistry;
import chat.server.session.ClientSession;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Sunucu alt sistemlerine tek giriş noktası (Facade + DI için uygun yapı).
 */
public final class ChatServerServices {

  private final OnlineUserRegistry registry;
  private final SharedFileStore fileStore;
  private final TimestampedLogger logger;
  private volatile String roomPassword;
  /** Boş değilse girişte 4. alan ile eşleşmeli (opsiyonel hesap şifresi). */
  private volatile String accountPassword;

  private final Set<String> bannedUsernames = ConcurrentHashMap.newKeySet();
  private final ConcurrentHashMap<String, ClientSession> activeSessions = new ConcurrentHashMap<>();
  private final AtomicLong totalBroadcastMessages = new AtomicLong();
  private final AtomicLong totalPrivateMessages = new AtomicLong();
  private final AtomicLong totalFileBytesStored = new AtomicLong();
  private final AtomicLong nextPrivateMessageId = new AtomicLong(1);
  private final AtomicLong nextBroadcastMessageId = new AtomicLong(1);

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

  /** Genel sohbet mesajları için artan kimlik (ödev: mesajda kullanıcı adı + zaman). */
  public long nextBroadcastMessageId() {
    return nextBroadcastMessageId.getAndIncrement();
  }

  public void setRoomPassword(String password) {
    roomPassword =
        (password == null || password.isBlank()) ? null : password.trim();
  }

  public String roomPassword() {
    return roomPassword;
  }

  public void setAccountPassword(String password) {
    accountPassword =
        (password == null || password.isBlank()) ? null : password.trim();
  }

  public String accountPassword() {
    return accountPassword;
  }

  public boolean isBanned(String username) {
    return username != null && bannedUsernames.contains(username.trim().toLowerCase());
  }

  public void banUsername(String username) {
    if (username != null && !username.isBlank()) {
      bannedUsernames.add(username.trim().toLowerCase());
    }
  }

  public void unbanUsername(String username) {
    if (username != null) {
      bannedUsernames.remove(username.trim().toLowerCase());
    }
  }

  public void registerActiveSession(String username, ClientSession session) {
    if (username != null && !username.isEmpty()) {
      activeSessions.put(username, session);
    }
  }

  public void unregisterActiveSession(String username) {
    if (username != null) {
      activeSessions.remove(username);
    }
  }

  public Optional<ClientSession> findActiveSession(String username) {
    if (username == null) {
      return Optional.empty();
    }
    String key = username.trim();
    if (key.isEmpty()) {
      return Optional.empty();
    }
    ClientSession direct = activeSessions.get(key);
    if (direct != null) {
      return Optional.of(direct);
    }
    for (Map.Entry<String, ClientSession> e : activeSessions.entrySet()) {
      if (e.getKey().equalsIgnoreCase(key)) {
        return Optional.of(e.getValue());
      }
    }
    return Optional.empty();
  }

  public long nextPrivateMessageId() {
    return nextPrivateMessageId.getAndIncrement();
  }

  public void incBroadcastCount() {
    totalBroadcastMessages.incrementAndGet();
  }

  public void incPrivateCount() {
    totalPrivateMessages.incrementAndGet();
  }

  public void addFileBytesStored(long n) {
    if (n > 0) {
      totalFileBytesStored.addAndGet(n);
    }
  }

  public long totalBroadcastMessages() {
    return totalBroadcastMessages.get();
  }

  public long totalPrivateMessages() {
    return totalPrivateMessages.get();
  }

  public long totalFileBytesStored() {
    return totalFileBytesStored.get();
  }

  public int activeSessionCount() {
    return activeSessions.size();
  }

  public static String sanitizeRoomName(String raw) {
    if (raw == null || raw.isBlank()) {
      return "genel";
    }
    String t = raw.trim();
    if (t.length() > 32) {
      t = t.substring(0, 32);
    }
    if (!t.matches("[a-zA-Z0-9_\\-ğüşıöçĞÜŞİÖÇ]+")) {
      return "genel";
    }
    return t;
  }
}
