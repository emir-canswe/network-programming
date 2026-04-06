package chat.server;

import java.util.ArrayDeque;

/** Kayan pencere: pencere içinde en fazla maxEvents olayı. */
public final class RateLimiter {

  private final int maxEvents;
  private final long windowMillis;
  private final ArrayDeque<Long> times = new ArrayDeque<>();

  public RateLimiter(int maxEvents, long windowMillis) {
    this.maxEvents = maxEvents;
    this.windowMillis = windowMillis;
  }

  public synchronized boolean tryAcquire() {
    long now = System.currentTimeMillis();
    while (!times.isEmpty() && now - times.peekFirst() > windowMillis) {
      times.pollFirst();
    }
    if (times.size() >= maxEvents) {
      return false;
    }
    times.addLast(now);
    return true;
  }
}
