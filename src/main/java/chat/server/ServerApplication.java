package chat.server;

import chat.server.file.SharedFileStore;
import chat.server.handlers.ClientRequestDispatcher;
import chat.server.logging.FileLogSink;
import chat.server.logging.TimestampedLogger;
import chat.server.registry.ConcurrentOnlineUserRegistry;
import chat.server.session.ClientSession;
import chat.ui.MockupTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.ColorUIResource;

/**
 * Sunucu arayüzü + TCP dinleyici (Swing). Renkler dark_chat_ui_mockup ile uyumlu.
 */
public final class ServerApplication extends JFrame {

  private final JPasswordField roomPasswordField = new JPasswordField(12);
  private final JPasswordField accountPasswordField = new JPasswordField(12);
  private final JTextField portField = new JTextField("9000", 8);
  private final JTextField banNameField = new JTextField(10);
  private final JTextField announceField = new JTextField(24);
  private final JButton announceBtn = new JButton("Tümüne duyuru");
  private final JButton startBtn = new JButton("Sunucuyu Başlat");
  private final JButton stopBtn = new JButton("Durdur");
  private final JTextArea logArea = new JTextArea(18, 60);
  private final DefaultListModel<String> userModel = new DefaultListModel<>();
  private final JList<String> usersJList = new JList<>(userModel);
  private final JLabel metricsLabel = new JLabel(" ");

  private final TimestampedLogger logger = new TimestampedLogger();
  private ChatServerServices services;
  private ClientRequestDispatcher dispatcher;
  private UserListBroadcaster broadcaster;

  private ServerSocket serverSocket;
  /** Ders örneğindeki gibi {@link java.io.DataInputStream} ile dosya kabulü (sohbet portu + 1). */
  private ServerSocket fileUploadServerSocket;

  private ExecutorService clientPool;
  private Thread acceptThread;
  private Thread fileUploadAcceptThread;

  private ServerApplication() {
    super("TCP Sohbet Sunucusu");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

    logger.addSink(new FileLogSink(Path.of("server.log")));
    logger.addSink(line -> SwingUtilities.invokeLater(() -> appendLog(line)));

    JPanel root = (JPanel) getContentPane();
    root.setLayout(new BorderLayout(0, 0));
    root.setBackground(MockupTheme.BG0);
    root.setBorder(new EmptyBorder(0, 0, 8, 0));

    logArea.setEditable(false);
    MockupTheme.styleTextAreaMono(logArea);
    logArea.setBackground(MockupTheme.BG0);

    MockupTheme.stylePassword(roomPasswordField);
    MockupTheme.stylePassword(accountPasswordField);
    MockupTheme.styleField(portField);
    MockupTheme.styleField(banNameField);
    MockupTheme.styleField(announceField);
    MockupTheme.styleAccentButton(startBtn);
    MockupTheme.styleGhostButton(stopBtn);
    MockupTheme.styleAccentButton(announceBtn);
    stopBtn.setEnabled(false);
    announceBtn.setEnabled(false);

    startBtn.addActionListener(e -> startServer());
    stopBtn.addActionListener(e -> stopServer());
    announceBtn.addActionListener(e -> sendAnnouncement());

    JPanel north = new JPanel();
    north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
    north.setOpaque(false);
    north.add(buildTopBar());

    JPanel top = cardPanel();
    top.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 8));
    JLabel lp = labelMuted("Port:");
    top.add(lp);
    top.add(portField);
    top.add(labelMuted("Oda şifresi (boş = kapalı):"));
    top.add(roomPasswordField);
    top.add(labelMuted("Hesap şifresi (boş = kapalı):"));
    top.add(accountPasswordField);
    top.add(startBtn);
    top.add(stopBtn);
    north.add(top);

    JPanel admin = cardPanel();
    admin.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 8));
    admin.add(labelMuted("Engelle / kaldır ad:"));
    admin.add(banNameField);
    JButton banBtn = new JButton("Engelle");
    JButton unbanBtn = new JButton("Kaldır");
    MockupTheme.styleGhostButton(banBtn);
    MockupTheme.styleGhostButton(unbanBtn);
    banBtn.addActionListener(
        e -> {
          if (services == null) {
            return;
          }
          String n = banNameField.getText().trim();
          if (n.length() < 2) {
            JOptionPane.showMessageDialog(this, "Geçerli kullanıcı adı girin.");
            return;
          }
          services.banUsername(n);
          services.findActiveSession(n).ifPresent(ClientSession::close);
          logger.info("BAN " + n);
        });
    unbanBtn.addActionListener(
        e -> {
          if (services == null) {
            return;
          }
          services.unbanUsername(banNameField.getText().trim());
          logger.info("UNBAN " + banNameField.getText().trim());
        });
    admin.add(banBtn);
    admin.add(unbanBtn);
    north.add(admin);

    JPanel ann = cardPanel();
    ann.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 8));
    ann.add(labelMuted("Duyuru metni:"));
    ann.add(announceField);
    ann.add(announceBtn);
    north.add(ann);

    JPanel hint = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
    hint.setOpaque(false);
    JLabel hi = new JLabel(
        "<html><div style='color:#94a3b8;font-size:11px'>Günlük <b style=\"color:#f1f5f9\">server.log</b> dosyasına da yazılır. Oda şifresi sadece sunucu başlarken uygulanır.<br/>Dosya gönderimi: <b style=\"color:#f1f5f9\">sohbet portu + 1</b> (DataInputStream / DataOutputStream akışı).</div></html>");
    hint.add(hi);
    north.add(hint);

    metricsLabel.setForeground(MockupTheme.T2);
    metricsLabel.setFont(MockupTheme.FONT_SM);
    JPanel met = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    met.setOpaque(false);
    met.add(metricsLabel);
    north.add(met);

    MockupTheme.styleList(usersJList);
    usersJList.setSelectionBackground(MockupTheme.AC);
    usersJList.setSelectionForeground(Color.WHITE);
    JScrollPane usersScroll = new JScrollPane(usersJList);
    usersScroll.setBorder(titledBorder("Çevrimiçi kullanıcılar"));
    usersScroll.getViewport().setBackground(MockupTheme.BG1);

    JScrollPane logScroll = new JScrollPane(logArea);
    logScroll.setBorder(titledBorder("Sunucu günlüğü"));
    logScroll.getViewport().setBackground(MockupTheme.BG0);

    JPanel east = new JPanel(new BorderLayout());
    east.setOpaque(false);
    east.setPreferredSize(new Dimension(220, 120));
    east.setBorder(new EmptyBorder(0, 8, 0, 0));
    JPanel eastCard = cardPanel();
    eastCard.setLayout(new BorderLayout());
    eastCard.add(usersScroll, BorderLayout.CENTER);
    JButton kickBtn = new JButton("Seçiliyi at (kick)");
    MockupTheme.styleGhostButton(kickBtn);
    kickBtn.addActionListener(
        e -> {
          if (services == null) {
            return;
          }
          String sel = usersJList.getSelectedValue();
          if (sel == null) {
            JOptionPane.showMessageDialog(this, "Listeden kullanıcı seçin.");
            return;
          }
          services.findActiveSession(sel).ifPresent(ClientSession::close);
          logger.info("KICK " + sel);
        });
    JPanel kickP = new JPanel(new FlowLayout(FlowLayout.CENTER));
    kickP.setOpaque(false);
    kickP.add(kickBtn);
    eastCard.add(kickP, BorderLayout.SOUTH);
    east.add(eastCard, BorderLayout.CENTER);

    root.add(north, BorderLayout.NORTH);
    root.add(logScroll, BorderLayout.CENTER);
    root.add(east, BorderLayout.EAST);

    Timer refresh =
        new Timer(
            500,
            e -> {
              if (services == null) {
                return;
              }
              userModel.clear();
              for (String u : services.registry().usernames()) {
                userModel.addElement(u);
              }
              metricsLabel.setText(
                  String.format(
                      "Oturum: %d | Genel mesaj: %d | Özel: %d | Dosya: %,.2f MB",
                      services.activeSessionCount(),
                      services.totalBroadcastMessages(),
                      services.totalPrivateMessages(),
                      services.totalFileBytesStored() / (1024.0 * 1024.0)));
            });
    refresh.start();

    pack();
    setLocationRelativeTo(null);
  }

  private static JPanel buildTopBar() {
    JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
    bar.setOpaque(true);
    bar.setBackground(MockupTheme.BG1);
    bar.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, MockupTheme.BD),
            new EmptyBorder(12, 16, 12, 16)));
    JLabel d1 = new JLabel("●");
    d1.setForeground(MockupTheme.RD);
    d1.setFont(d1.getFont().deriveFont(8f));
    JLabel d2 = new JLabel("●");
    d2.setForeground(MockupTheme.AM);
    d2.setFont(d2.getFont().deriveFont(8f));
    JLabel d3 = new JLabel("●");
    d3.setForeground(MockupTheme.GR);
    d3.setFont(d3.getFont().deriveFont(8f));
    JPanel dots = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    dots.setOpaque(false);
    dots.add(d1);
    dots.add(d2);
    dots.add(d3);
    JLabel logo = new JLabel("S", SwingConstants.CENTER);
    logo.setOpaque(true);
    logo.setBackground(MockupTheme.AC);
    logo.setForeground(Color.WHITE);
    logo.setFont(logo.getFont().deriveFont(Font.BOLD, 12f));
    logo.setPreferredSize(new Dimension(24, 24));
    JLabel title = new JLabel("TCP Sohbet Sunucusu");
    title.setForeground(MockupTheme.T0);
    title.setFont(title.getFont().deriveFont(Font.PLAIN, 13f));
    bar.add(dots);
    bar.add(logo);
    bar.add(title);
    return bar;
  }

  private static JPanel cardPanel() {
    JPanel p = new JPanel();
    p.setBackground(MockupTheme.BG1);
    p.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(MockupTheme.BD2, 1, true),
            new EmptyBorder(8, 12, 8, 12)));
    return p;
  }

  private static JLabel labelMuted(String t) {
    JLabel l = new JLabel(t);
    l.setForeground(MockupTheme.T1);
    l.setFont(MockupTheme.FONT_SM);
    return l;
  }

  private static javax.swing.border.Border titledBorder(String title) {
    TitledBorder tb =
        BorderFactory.createTitledBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(MockupTheme.BD2, 1, true),
                BorderFactory.createEmptyBorder(6, 8, 8, 8)),
            title);
    tb.setTitleColor(MockupTheme.T1);
    Font base = UIManager.getFont("Label.font");
    if (base != null) {
      tb.setTitleFont(base.deriveFont(Font.BOLD, 11f));
    }
    return tb;
  }

  private void appendLog(String line) {
    logArea.append(line + "\n");
    logArea.setCaretPosition(logArea.getDocument().getLength());
  }

  private void sendAnnouncement() {
    if (broadcaster == null) {
      return;
    }
    String t = announceField.getText().trim();
    if (t.isEmpty()) {
      JOptionPane.showMessageDialog(this, "Duyuru metni boş olamaz.");
      return;
    }
    broadcaster.sendLogToAll("[DUYURU] " + t);
    logger.info("Yönetici duyurusu: " + t);
    announceField.setText("");
  }

  private void startServer() {
    int port;
    try {
      port = Integer.parseInt(portField.getText().trim());
      if (port < 1 || port > 65535) {
        throw new NumberFormatException();
      }
    } catch (NumberFormatException ex) {
      JOptionPane.showMessageDialog(this, "Geçerli bir port girin (1-65535).");
      return;
    }
    try {
      Path storeDir = Path.of("server-shared-files");
      SharedFileStore files = new SharedFileStore(storeDir);
      ConcurrentOnlineUserRegistry reg = new ConcurrentOnlineUserRegistry();
      services = new ChatServerServices(reg, files, logger);
      String pw = new String(roomPasswordField.getPassword());
      services.setRoomPassword(pw);
      services.setAccountPassword(new String(accountPasswordField.getPassword()));
      broadcaster = new UserListBroadcaster(services);
      dispatcher = new ClientRequestDispatcher(services, broadcaster);
    } catch (IOException ex) {
      JOptionPane.showMessageDialog(this, "Sunucu başlatılamadı: " + ex.getMessage());
      return;
    }

    try {
      serverSocket = new ServerSocket(port);
    } catch (IOException ex) {
      JOptionPane.showMessageDialog(this, "Port dinlenemiyor: " + ex.getMessage());
      return;
    }
    if (port >= 65535) {
      try {
        serverSocket.close();
      } catch (IOException ignored) {
      }
      serverSocket = null;
      JOptionPane.showMessageDialog(this, "Dosya yükleme için sohbet portu 65534 veya daha küçük olmalı (port+1).");
      return;
    }
    try {
      fileUploadServerSocket = new ServerSocket(port + 1);
    } catch (IOException ex) {
      try {
        serverSocket.close();
      } catch (IOException ignored) {
      }
      serverSocket = null;
      JOptionPane.showMessageDialog(
          this, "Dosya yükleme portu açılamadı (" + (port + 1) + "): " + ex.getMessage());
      return;
    }

    clientPool = Executors.newCachedThreadPool();
    startBtn.setEnabled(false);
    stopBtn.setEnabled(true);
    announceBtn.setEnabled(true);
    portField.setEnabled(false);
    roomPasswordField.setEnabled(false);
    accountPasswordField.setEnabled(false);

    logger.info("Sunucu dinlemede: port " + port + " | Dosya (TCP/DataStream): port " + (port + 1));

    acceptThread =
        new Thread(
            () -> {
              while (serverSocket != null && !serverSocket.isClosed()) {
                try {
                  Socket s = serverSocket.accept();
                  logger.info("Bağlantı kabul edildi: " + s.getRemoteSocketAddress());
                  ClientSession session =
                      new ClientSession(s, services, dispatcher, broadcaster, () -> {});
                  clientPool.execute(session);
                } catch (IOException ex) {
                  if (serverSocket != null && !serverSocket.isClosed()) {
                    logger.error("Kabul döngüsü hatası: " + ex.getMessage());
                  }
                }
              }
            },
            "accept-loop");
    acceptThread.setDaemon(true);
    acceptThread.start();

    fileUploadAcceptThread =
        new Thread(
            () -> {
              while (fileUploadServerSocket != null && !fileUploadServerSocket.isClosed()) {
                try {
                  Socket s = fileUploadServerSocket.accept();
                  logger.info("Dosya yükleme bağlantısı: " + s.getRemoteSocketAddress());
                  clientPool.execute(
                      () ->
                          TcpDosyaUploadListener.handleUpload(s, services, broadcaster, logger));
                } catch (IOException ex) {
                  if (fileUploadServerSocket != null && !fileUploadServerSocket.isClosed()) {
                    logger.error("Dosya kabul döngüsü: " + ex.getMessage());
                  }
                }
              }
            },
            "file-upload-accept");
    fileUploadAcceptThread.setDaemon(true);
    fileUploadAcceptThread.start();
  }

  private void stopServer() {
    try {
      if (fileUploadServerSocket != null && !fileUploadServerSocket.isClosed()) {
        fileUploadServerSocket.close();
      }
    } catch (IOException ignored) {
    }
    fileUploadServerSocket = null;
    fileUploadAcceptThread = null;
    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
    } catch (IOException ignored) {
    }
    serverSocket = null;
    if (clientPool != null) {
      clientPool.shutdownNow();
      clientPool = null;
    }
    startBtn.setEnabled(true);
    stopBtn.setEnabled(false);
    announceBtn.setEnabled(false);
    portField.setEnabled(true);
    roomPasswordField.setEnabled(true);
    accountPasswordField.setEnabled(true);
    services = null;
    broadcaster = null;
    dispatcher = null;
    logger.info("Sunucu durduruldu.");
  }

  public static void main(String[] args) {
    MockupTheme.installSwingDefaults();
    UIManager.put("List.selectionBackground", new ColorUIResource(MockupTheme.AC));
    UIManager.put("List.selectionForeground", new ColorUIResource(Color.WHITE));
    SwingUtilities.invokeLater(() -> new ServerApplication().setVisible(true));
  }
}
