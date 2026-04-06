package chat.server.handlers;

import chat.server.ChatServerServices;
import chat.server.session.ClientSession;

/**
 * Çıkış komutu — bağlantıyı kapatır; {@link ClientSession} finally bloğu listeyi günceller.
 */
public final class LogoutHandler implements ClientRequestHandler {

  private final ChatServerServices services;

  public LogoutHandler(ChatServerServices services) {
    this.services = services;
  }

  @Override
  public void handle(ClientSession session) {
    services.logger().info("LOGOUT: " + session.getUsername());
    session.close();
  }
}
