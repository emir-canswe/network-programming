package chat.server.registry;

import chat.server.session.ClientConnection;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Çevrimiçi kullanıcı yönetimi — arayüz (LSP: farklı depolama uygulanabilir).
 */
public interface OnlineUserRegistry {

  boolean register(String username, ClientConnection connection);

  void unregister(String username);

  Optional<ClientConnection> find(String username);

  Collection<String> usernames();

  void forEachConnection(Consumer<ClientConnection> action);
}
