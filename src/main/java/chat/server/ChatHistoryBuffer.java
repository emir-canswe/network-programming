package chat.server;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** Son genel sohbet mesajları (oda bazlı filtre; mesaj kimliği ile düzenle/sil). */
public final class ChatHistoryBuffer {

  public record Entry(long messageId, String fromUser, long epochMs, String text, String room) {}

  private final int capacity;
  private final ArrayDeque<Entry> deque = new ArrayDeque<>();
  private final AtomicLong nextId = new AtomicLong(1);

  public ChatHistoryBuffer(int capacity) {
    this.capacity = Math.max(1, capacity);
  }

  public synchronized long add(String fromUser, long epochMs, String text, String room) {
    long id = nextId.getAndIncrement();
    deque.addLast(new Entry(id, fromUser, epochMs, text, room));
    while (deque.size() > capacity) {
      deque.removeFirst();
    }
    return id;
  }

  public synchronized List<Entry> snapshot() {
    return new ArrayList<>(deque);
  }

  public synchronized List<Entry> snapshotForRoom(String room) {
    String r = room == null || room.isBlank() ? "genel" : room;
    List<Entry> out = new ArrayList<>();
    for (Entry e : deque) {
      if (r.equals(e.room())) {
        out.add(e);
      }
    }
    return out;
  }

  public synchronized Optional<Entry> find(long messageId) {
    for (Entry e : deque) {
      if (e.messageId() == messageId) {
        return Optional.of(e);
      }
    }
    return Optional.empty();
  }

  /**
   * Sadece gönderen, 2 dk içindeyse düzenleme.
   *
   * @return yeni metinle giriş veya boş
   */
  public synchronized Optional<Entry> tryEdit(long messageId, String user, String newText) {
    if (newText == null || newText.isBlank()) {
      return Optional.empty();
    }
    List<Entry> copy = new ArrayList<>(deque);
    for (int i = 0; i < copy.size(); i++) {
      Entry e = copy.get(i);
      if (e.messageId() == messageId
          && e.fromUser().equals(user)
          && System.currentTimeMillis() - e.epochMs() <= 120_000) {
        Entry u = new Entry(e.messageId(), user, e.epochMs(), newText.strip(), e.room());
        copy.set(i, u);
        deque.clear();
        deque.addAll(copy);
        return Optional.of(u);
      }
    }
    return Optional.empty();
  }

  /** Sadece gönderen, 2 dk içindeyse siler. */
  public synchronized boolean tryDelete(long messageId, String user) {
    List<Entry> copy = new ArrayList<>(deque);
    boolean removed = false;
    for (Entry e : copy) {
      if (e.messageId() == messageId
          && e.fromUser().equals(user)
          && System.currentTimeMillis() - e.epochMs() <= 120_000) {
        copy.remove(e);
        removed = true;
        break;
      }
    }
    if (removed) {
      deque.clear();
      deque.addAll(copy);
    }
    return removed;
  }
}
