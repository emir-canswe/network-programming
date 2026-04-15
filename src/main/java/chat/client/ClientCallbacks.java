package chat.client;

/**
 * Ağ iş parçacığından EDT'ye güvenli geri çağrılar (ISP: istemci UI ihtiyaçları).
 */
public interface ClientCallbacks {

  void onAckLogin(boolean ok, String message);

  void onUserList(String csv);

  void onChatBroadcast(String from, long epochMs, long messageId, String text);

  void onChatPrivate(String from, long epochMs, long messageId, String text);

  default void onChatBroadcastEdit(String from, long epochMs, long messageId, String newText) {}

  default void onChatBroadcastDelete(long messageId) {}

  default void onChatPrivateEdit(String from, long epochMs, long messageId, String newText) {}

  default void onChatPrivateDelete(long messageId) {}

  void onNotify(String line);

  void onFileAvailable(String id, String fromUser, String filename, long size, String mime);

  void onFilePayload(String filename, long size, byte[] data);

  void onFileDownloadProgress(int percent, long current, long total);

  void onFileDownloadProgressDone();

  void onError(String message);

  void onServerLog(String line);

  void onDisconnected(String reason);
}
