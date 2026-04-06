package chat.server;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/** Son genel sohbet mesajları (yeni bağlananlara gönderilir). */
public final class ChatHistoryBuffer {

  public record Entry(String fromUser, long epochMs, String text) {}

  private final int capacity;
  private final ArrayDeque<Entry> deque = new ArrayDeque<>();

  public ChatHistoryBuffer(int capacity) {
    this.capacity = Math.max(1, capacity);
  }

  public synchronized void add(String fromUser, long epochMs, String text) {
    deque.addLast(new Entry(fromUser, epochMs, text));
    while (deque.size() > capacity) {
      deque.removeFirst();
    }
  }

  public synchronized List<Entry> snapshot() {
    return new ArrayList<>(deque);
  }
}
