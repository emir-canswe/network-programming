package chat.protocol;

/**
 * Komut tabanlı protokol işlem kodları (sunucu/istemci sözleşmesi).
 * Ağ Programlama ödevi: çok kullanıcılı sohbet + dosya paylaşımı.
 */
public final class OpCode {

  private OpCode() {}

  /* İstemci → Sunucu */
  public static final int C_LOGIN = 1;
  public static final int C_CHAT = 2;
  public static final int C_LOGOUT = 3;
  public static final int C_FILE_OFFER = 4;
  public static final int C_FILE_REQUEST = 5;
  public static final int C_LIST_USERS = 7;
  /** Büyük dosya: başlık + parçalar + commit */
  public static final int C_FILE_UPLOAD_BEGIN = 43;
  public static final int C_FILE_UPLOAD_PART = 44;
  public static final int C_FILE_UPLOAD_COMMIT = 45;
  public static final int C_FILE_CANCEL = 46;
  public static final int C_EDIT_BROADCAST = 47;
  public static final int C_DELETE_BROADCAST = 48;
  public static final int C_EDIT_PRIVATE = 49;
  public static final int C_DELETE_PRIVATE = 50;

  /* Sunucu → İstemci */
  public static final int S_ACK_LOGIN = 10;
  public static final int S_USER_LIST = 11;
  public static final int S_CHAT_BROADCAST = 12;
  public static final int S_CHAT_PRIVATE = 13;
  public static final int S_NOTIFY = 14;
  public static final int S_FILE_AVAILABLE = 15;
  public static final int S_FILE_PAYLOAD = 16;
  public static final int S_ERROR = 17;
  public static final int S_SERVER_LOG = 18;
  public static final int S_CHAT_BROADCAST_EDIT = 28;
  public static final int S_CHAT_BROADCAST_DELETE = 29;
  public static final int S_CHAT_PRIVATE_EDIT = 30;
  public static final int S_CHAT_PRIVATE_DELETE = 31;
}
