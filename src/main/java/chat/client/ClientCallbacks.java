package chat.client;

import java.util.List;

/**
 * Ağ iş parçacığından EDT'ye güvenli geri çağrılar (ISP: istemci UI ihtiyaçları).
 */
public interface ClientCallbacks {

  record HistoryEntry(String fromUser, long epochMs, long messageId, String text) {}

  void onAckLogin(boolean ok, String message);

  void onUserList(String csv);

  void onChatBroadcast(String from, long epochMs, long messageId, String text);

  void onChatPrivate(String from, long epochMs, long messageId, String text);

  void onNotify(String line);

  void onFileAvailable(String id, String fromUser, String filename, long size, String mime);

  void onFilePayload(String filename, long size, byte[] data);

  void onFileDownloadProgress(int percent, long current, long total);

  void onFileDownloadProgressDone();

  void onChatHistory(List<HistoryEntry> entries);

  void onUserTyping(String username, boolean started);

  void onMessageEdited(long messageId, String newText, String editedBy);

  void onMessageDeleted(long messageId);

  void onError(String message);

  void onServerLog(String line);

  void onDisconnected(String reason);
}
