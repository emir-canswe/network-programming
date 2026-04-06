package chat.server.registry;

import chat.server.session.ClientConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class ConcurrentOnlineUserRegistry implements OnlineUserRegistry {

  private final ConcurrentHashMap<String, ClientConnection> map = new ConcurrentHashMap<>();

  @Override
  public boolean register(String username, ClientConnection connection) {
    return map.putIfAbsent(username, connection) == null;
  }

  @Override
  public void unregister(String username) {
    map.remove(username);
  }

  @Override
  public Optional<ClientConnection> find(String username) {
    return Optional.ofNullable(map.get(username));
  }

  @Override
  public Collection<String> usernames() {
    return Collections.unmodifiableCollection(new ArrayList<>(map.keySet()));
  }

  @Override
  public void forEachConnection(Consumer<ClientConnection> action) {
    map.values().forEach(action);
  }
}
