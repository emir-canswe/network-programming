package chat.server.handlers;

import chat.server.ChatServerServices;
import chat.server.session.ClientSession;

/** Yarım kalan parçalı yüklemeyi temizler. */
public final class FileUploadCancelHandler implements ClientRequestHandler {

  private final ChatServerServices services;

  public FileUploadCancelHandler(ChatServerServices services) {
    this.services = services;
  }

  @Override
  public void handle(ClientSession session) {
    session.clearPendingUpload();
    services.logger().info("FILE_CANCEL " + session.getUsername());
  }
}
