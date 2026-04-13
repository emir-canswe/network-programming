package chat.server;

public final class ServerLimits {
  private ServerLimits() {}

  /** Sunucunun kabul ettiği tek seferde dosya üst sınırı (bayt). */
  public static final int MAX_FILE_BYTES = 32 * 1024 * 1024;

  /** Parçalı yüklemede tek parça üst sınırı. */
  public static final int MAX_UPLOAD_CHUNK_BYTES = 512 * 1024;
}
