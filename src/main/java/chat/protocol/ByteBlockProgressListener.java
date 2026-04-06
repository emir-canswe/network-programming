package chat.protocol;

/** Dosya gövdesi okunurken ilerleme (yüzde 0–100). */
@FunctionalInterface
public interface ByteBlockProgressListener {
  void onProgress(int percent, long current, long total);
}
