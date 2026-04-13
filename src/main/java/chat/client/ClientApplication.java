package chat.client;

import chat.client.ui.ChatRowFactory;
import chat.server.PrivateMessageParser;
import chat.ui.MockupTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.ColorUIResource;

/**
 * dark_chat_ui_mockup.html ile uyumlu koyu sohbet arayüzü (yan panel 196px, balonlar, yatay log).
 */
public final class ClientApplication extends JFrame implements ClientCallbacks {

  private static final DateTimeFormatter LOCAL_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault());
  private static final DateTimeFormatter SHORT_TIME =
      DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());
  private static final long WARN_FILE_BYTES = 20L * 1024 * 1024;

  private final JTextField hostField = new JTextField("127.0.0.1", 10);
  private final JTextField portField = new JTextField("9000", 5);
  private final JTextField userField = new JTextField(8);
  private final JPasswordField roomPasswordField = new JPasswordField(8);
  private final JTextField roomNameField = new JTextField("genel", 6);
  private final JPasswordField accountPasswordField = new JPasswordField(6);
  private final JCheckBox autoReconnectBox = new JCheckBox("Oto. yeniden bağlan", false);
  private final JSlider fontSlider = new JSlider(10, 20, 12);
  private final JButton connectBtn = new JButton("Bağlan");
  private final JButton refreshBtn = new JButton("↻");
  private final JLabel headerPortLabel = new JLabel("port —");
  private final JLabel headerStatusLabel = new JLabel("Bağlantı yok");
  private final JLabel statusDot = new JLabel("●");
  private final JLabel onlineCountBadge = new JLabel("0");
  private final JLabel filesCountBadge = new JLabel("0");
  private final JLabel chatSubtitleLabel = new JLabel("0 kişi çevrimiçi");
  private final JLabel targetPreview = new JLabel("Genel sohbet");
  private final JTextArea messageArea = new JTextArea(2, 36);
  private final JButton sendBtn = new JButton("➤");
  private final JProgressBar downloadBar = new JProgressBar(0, 100);
  private final DefaultListModel<String> userModel = new DefaultListModel<>();
  private final DefaultListModel<FileOffer> fileModel = new DefaultListModel<>();
  private final Map<String, FileOffer> filesById = new ConcurrentHashMap<>();

  private JPanel messagesInner;
  private JScrollPane messagesScroll;
  private JPanel usersListPanel;
  private JPanel filesListPanel;
  private JPanel logStripInner;
  private JScrollPane logStripScroll;
  private JLabel typingStatusLabel;

  private final ChatNetworkClient net = new ChatNetworkClient(this);
  private volatile boolean loggedIn;
  private final AtomicBoolean wantAutoReconnect = new AtomicBoolean();
  private final Map<Long, JPanel> broadcastPanels = new ConcurrentHashMap<>();
  private final Map<Long, JTextArea> broadcastBodies = new ConcurrentHashMap<>();
  private long lastTypingSignalMs;
  private Timer typingStopTimer;
  private Timer typingClearTimer;
  private volatile boolean lastLoginSucceeded;

  private ClientApplication() {
    super("ChatApp");
    setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    setMinimumSize(new Dimension(960, 640));

    typingStopTimer =
        new Timer(
            800,
            e -> {
              typingStopTimer.stop();
              if (loggedIn) {
                new Thread(
                        () -> {
                          try {
                            net.sendTyping(false);
                          } catch (IOException ignored) {
                          }
                        },
                        "typing-stop")
                    .start();
              }
            });
    typingClearTimer =
        new Timer(
            2500,
            e -> {
              typingClearTimer.stop();
              if (typingStatusLabel != null) {
                typingStatusLabel.setText(" ");
              }
            });

    getContentPane().setBackground(MockupTheme.BG0);
    ((JPanel) getContentPane()).setBorder(new EmptyBorder(0, 0, 0, 0));

    downloadBar.setStringPainted(true);
    downloadBar.setString("İndirme");
    downloadBar.setVisible(false);
    downloadBar.setForeground(MockupTheme.AC);
    downloadBar.setBackground(MockupTheme.BG2);

    MockupTheme.styleField(hostField);
    MockupTheme.styleField(portField);
    MockupTheme.styleField(userField);
    MockupTheme.stylePassword(roomPasswordField);
    MockupTheme.styleField(roomNameField);
    MockupTheme.stylePassword(accountPasswordField);
    autoReconnectBox.setForeground(MockupTheme.T2);
    autoReconnectBox.setOpaque(false);
    fontSlider.setOpaque(false);
    fontSlider.setMajorTickSpacing(2);
    fontSlider.setPaintTicks(true);
    fontSlider.addChangeListener(e -> applyFontScale());
    styleSmallButton(refreshBtn);
    MockupTheme.styleAccentButton(connectBtn);

    sendBtn.setFocusPainted(false);
    sendBtn.setBackground(MockupTheme.AC);
    sendBtn.setForeground(Color.WHITE);
    sendBtn.setBorderPainted(false);
    sendBtn.setFont(sendBtn.getFont().deriveFont(Font.BOLD, 14f));
    sendBtn.setPreferredSize(new Dimension(36, 36));
    sendBtn.setMinimumSize(new Dimension(36, 36));

    messageArea.setLineWrap(true);
    messageArea.setWrapStyleWord(true);
    messageArea.setOpaque(false);
    messageArea.setForeground(MockupTheme.T0);
    messageArea.setCaretColor(MockupTheme.AC3);
    messageArea.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
    messageArea.setBorder(null);

    targetPreview.setForeground(MockupTheme.T2);
    targetPreview.setFont(MockupTheme.FONT_XS);

    headerStatusLabel.setForeground(MockupTheme.T1);
    headerStatusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
    statusDot.setFont(statusDot.getFont().deriveFont(8f));
    statusDot.setForeground(MockupTheme.T2);

    headerPortLabel.setForeground(MockupTheme.T2);
    headerPortLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));

    disconnectBtnSetup();
    connectBtn.addActionListener(e -> doConnect());
    refreshBtn.addActionListener(e -> doRefreshUsers());
    sendBtn.addActionListener(e -> doSend());

    messageArea.setEnabled(false);
    sendBtn.setEnabled(false);
    refreshBtn.setEnabled(false);

    messageArea.addKeyListener(
        new KeyAdapter() {
          @Override
          public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
              e.consume();
              doSend();
            }
          }
        });

    messageArea
        .getDocument()
        .addDocumentListener(
            new DocumentListener() {
              void u() {
                updateTargetPreview();
                signalTyping();
              }

              @Override
              public void insertUpdate(DocumentEvent e) {
                u();
              }

              @Override
              public void removeUpdate(DocumentEvent e) {
                u();
              }

              @Override
              public void changedUpdate(DocumentEvent e) {
                u();
              }
            });

    JPanel root = new JPanel(new BorderLayout());
    root.setBackground(MockupTheme.BG0);

    root.add(buildTitleBar(), BorderLayout.NORTH);
    root.add(buildConnectStrip(), BorderLayout.SOUTH);

    JPanel body = new JPanel(new BorderLayout(0, 0));
    body.setBackground(MockupTheme.BG0);
    body.add(buildSidebar(), BorderLayout.WEST);
    body.add(buildChatColumn(), BorderLayout.CENTER);

    root.add(body, BorderLayout.CENTER);
    setContentPane(root);

    addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent e) {
            if (net.isConnected()) {
              try {
                net.sendLogout();
              } catch (IOException ignored) {
              }
              net.disconnect();
            }
          }
        });

    setLocationRelativeTo(null);
  }

  private JButton disconnectHeaderBtn;

  private void disconnectBtnSetup() {
    disconnectHeaderBtn = new JButton("Çıkış ↗");
    MockupTheme.styleDangerGhostButton(disconnectHeaderBtn);
    disconnectHeaderBtn.setEnabled(false);
    disconnectHeaderBtn.addActionListener(e -> doLogout());
  }

  private JPanel buildTitleBar() {
    JPanel bar = new JPanel(new BorderLayout());
    bar.setBackground(MockupTheme.BG1);
    bar.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, MockupTheme.BD),
            new EmptyBorder(0, 16, 0, 16)));
    bar.setPreferredSize(new Dimension(10, 48));

    JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
    left.setOpaque(false);
    JPanel dots = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    dots.setOpaque(false);
    JLabel d1 = new JLabel("●");
    d1.setForeground(MockupTheme.RD);
    d1.setFont(d1.getFont().deriveFont(8f));
    JLabel d2 = new JLabel("●");
    d2.setForeground(MockupTheme.AM);
    d2.setFont(d2.getFont().deriveFont(8f));
    JLabel d3 = new JLabel("●");
    d3.setForeground(MockupTheme.GR);
    d3.setFont(d3.getFont().deriveFont(8f));
    dots.add(d1);
    dots.add(d2);
    dots.add(d3);

    JLabel logo = new JLabel("C", SwingConstants.CENTER);
    logo.setOpaque(true);
    logo.setBackground(MockupTheme.AC);
    logo.setForeground(Color.WHITE);
    logo.setFont(logo.getFont().deriveFont(Font.PLAIN, 12f));
    logo.setPreferredSize(new Dimension(24, 24));

    JLabel brand = new JLabel("ChatApp");
    brand.setForeground(MockupTheme.T0);
    brand.setFont(brand.getFont().deriveFont(Font.PLAIN, 13f));

    JPanel sep = new JPanel();
    sep.setPreferredSize(new Dimension(1, 16));
    sep.setMaximumSize(new Dimension(1, 16));
    sep.setBackground(MockupTheme.BD);

    left.add(dots);
    left.add(logo);
    left.add(brand);
    left.add(sep);
    left.add(headerPortLabel);

    JPanel pill = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    pill.setOpaque(true);
    pill.setBackground(MockupTheme.BG3);
    pill.setBorder(
        BorderFactory.createCompoundBorder(
            new LineBorder(MockupTheme.BD, 1, true), new EmptyBorder(3, 10, 3, 10)));
    pill.add(statusDot);
    pill.add(headerStatusLabel);

    JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    right.setOpaque(false);
    right.add(pill);

    bar.add(left, BorderLayout.WEST);
    bar.add(right, BorderLayout.EAST);
    return bar;
  }

  private JPanel buildConnectStrip() {
    JPanel outer = new JPanel();
    outer.setLayout(new BoxLayout(outer, BoxLayout.Y_AXIS));
    outer.setBackground(MockupTheme.BG1);
    outer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, MockupTheme.BD));

    JPanel fields = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
    fields.setOpaque(false);
    fields.setBackground(MockupTheme.BG1);
    addMuted(fields, "Sunucu");
    fields.add(hostField);
    addMuted(fields, "Port");
    fields.add(portField);
    addMuted(fields, "Kullanıcı");
    fields.add(userField);
    addMuted(fields, "Oda şifresi");
    fields.add(roomPasswordField);
    addMuted(fields, "Oda adı");
    fields.add(roomNameField);
    addMuted(fields, "Hesap şifresi");
    fields.add(accountPasswordField);

    JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 6));
    actions.setOpaque(false);
    actions.add(connectBtn);
    actions.add(refreshBtn);

    JScrollPane fieldScroll = new JScrollPane(fields);
    fieldScroll.setBorder(null);
    fieldScroll.getViewport().setBackground(MockupTheme.BG1);
    fieldScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    fieldScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
    fieldScroll.setWheelScrollingEnabled(true);

    JPanel row1 = new JPanel(new BorderLayout(8, 0));
    row1.setOpaque(false);
    row1.setAlignmentX(Component.LEFT_ALIGNMENT);
    row1.add(fieldScroll, BorderLayout.CENTER);
    row1.add(actions, BorderLayout.EAST);

    JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
    row2.setOpaque(false);
    row2.setAlignmentX(Component.LEFT_ALIGNMENT);
    row2.add(autoReconnectBox);
    addMuted(row2, "Yazı px");
    row2.add(fontSlider);

    outer.add(row1);
    outer.add(row2);
    return outer;
  }

  private static void addMuted(JPanel strip, String text) {
    JLabel l = new JLabel(text);
    l.setForeground(MockupTheme.T2);
    l.setFont(MockupTheme.FONT_SM);
    strip.add(l);
  }

  private JPanel buildSidebar() {
    JPanel side = new JPanel(new BorderLayout());
    side.setPreferredSize(new Dimension(220, 400));
    side.setBackground(MockupTheme.BG1);
    side.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, MockupTheme.BD));

    usersListPanel = new JPanel();
    usersListPanel.setLayout(new BoxLayout(usersListPanel, BoxLayout.Y_AXIS));
    usersListPanel.setBackground(MockupTheme.BG1);
    JScrollPane us = new JScrollPane(usersListPanel);
    us.setBorder(null);
    us.getViewport().setBackground(MockupTheme.BG1);
    us.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    filesListPanel = new JPanel();
    filesListPanel.setLayout(new BoxLayout(filesListPanel, BoxLayout.Y_AXIS));
    filesListPanel.setBackground(MockupTheme.BG1);
    JScrollPane fs = new JScrollPane(filesListPanel);
    fs.setBorder(null);
    fs.getViewport().setBackground(MockupTheme.BG1);
    fs.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    JPanel north = new JPanel();
    north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
    north.setOpaque(false);
    north.add(ChatRowFactory.sectionTitleOnline("Çevrimiçi", onlineCountBadge));
    north.add(us);
    north.add(ChatRowFactory.divider());
    north.add(ChatRowFactory.sectionTitleFiles("Dosyalar", filesCountBadge));
    north.add(fs);

    side.add(north, BorderLayout.CENTER);
    return side;
  }

  private static JLabel chatHeaderIcon() {
    JLabel g = new JLabel("G", SwingConstants.CENTER);
    g.setOpaque(true);
    g.setBackground(MockupTheme.AC);
    g.setForeground(Color.WHITE);
    g.setFont(g.getFont().deriveFont(Font.PLAIN, 13f));
    Dimension d = new Dimension(32, 32);
    g.setPreferredSize(d);
    g.setMinimumSize(d);
    g.setMaximumSize(d);
    return g;
  }

  private JPanel buildChatColumn() {
    JPanel col = new JPanel(new BorderLayout(0, 0));
    col.setBackground(MockupTheme.BG0);

    JPanel chatHead = new JPanel(new BorderLayout(12, 0));
    chatHead.setBackground(MockupTheme.BG1);
    chatHead.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, MockupTheme.BD),
            new EmptyBorder(0, 16, 0, 16)));
    chatHead.setPreferredSize(new Dimension(10, 48));

    JPanel leftHead = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
    leftHead.setOpaque(false);
    leftHead.add(chatHeaderIcon());
    JPanel titles = new JPanel();
    titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
    titles.setOpaque(false);
    JLabel t1 = new JLabel("Genel Sohbet");
    t1.setForeground(MockupTheme.T0);
    t1.setFont(t1.getFont().deriveFont(Font.PLAIN, 13f));
    chatSubtitleLabel.setForeground(MockupTheme.T2);
    chatSubtitleLabel.setFont(MockupTheme.FONT_XS);
    titles.add(t1);
    titles.add(chatSubtitleLabel);
    leftHead.add(titles);

    JPanel rightHead = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
    rightHead.setOpaque(false);
    JButton attach = new JButton("📎 Dosya gönder");
    MockupTheme.styleGhostButtonTransparent(attach);
    attach.addActionListener(e -> doSendFile());
    rightHead.add(attach);
    rightHead.add(disconnectHeaderBtn);

    chatHead.add(leftHead, BorderLayout.WEST);
    chatHead.add(rightHead, BorderLayout.EAST);

    messagesInner = new JPanel();
    messagesInner.setLayout(new BoxLayout(messagesInner, BoxLayout.Y_AXIS));
    messagesInner.setBackground(MockupTheme.BG0);
    messagesInner.setBorder(new EmptyBorder(16, 16, 16, 16));
    messagesInner.add(Box.createVerticalGlue());

    messagesScroll = new JScrollPane(messagesInner);
    messagesScroll.setBorder(null);
    messagesScroll.getViewport().setBackground(MockupTheme.BG0);
    messagesScroll.getVerticalScrollBar().setUnitIncrement(16);

    typingStatusLabel = new JLabel(" ");
    typingStatusLabel.setFont(MockupTheme.FONT_XS);
    typingStatusLabel.setForeground(MockupTheme.AC3);
    typingStatusLabel.setBorder(new EmptyBorder(2, 20, 2, 20));
    JPanel scrollWrap = new JPanel(new BorderLayout());
    scrollWrap.setOpaque(false);
    scrollWrap.add(typingStatusLabel, BorderLayout.NORTH);
    scrollWrap.add(messagesScroll, BorderLayout.CENTER);

    JButton scrollBottom = new JButton("↓");
    scrollBottom.setFocusPainted(false);
    scrollBottom.setBackground(MockupTheme.AC);
    scrollBottom.setForeground(Color.WHITE);
    scrollBottom.setBorderPainted(false);
    scrollBottom.setPreferredSize(new Dimension(40, 40));
    scrollBottom.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    scrollBottom.addActionListener(e -> scrollChatToBottom());
    JPanel floatSouth = new JPanel(new FlowLayout(FlowLayout.CENTER));
    floatSouth.setOpaque(false);
    floatSouth.add(scrollBottom);
    scrollWrap.add(floatSouth, BorderLayout.SOUTH);

    JPanel inputBar = new JPanel(new BorderLayout(0, 0));
    inputBar.setBackground(MockupTheme.BG1);
    inputBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, MockupTheme.BD));
    inputBar.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, MockupTheme.BD),
            new EmptyBorder(12, 16, 12, 16)));

    JPanel inputStack = new JPanel();
    inputStack.setLayout(new BoxLayout(inputStack, BoxLayout.Y_AXIS));
    inputStack.setOpaque(false);
    inputStack.add(targetPreview);
    inputStack.add(Box.createVerticalStrut(6));

    JPanel inputRow = new JPanel(new BorderLayout(8, 0));
    inputRow.setOpaque(false);
    JPanel inputWrap = new JPanel(new BorderLayout());
    inputWrap.setBackground(MockupTheme.BG2);
    inputWrap.setBorder(
        BorderFactory.createCompoundBorder(
            new LineBorder(MockupTheme.BD2, 1, true), new EmptyBorder(8, 12, 8, 12)));
    JScrollPane msgScroll = new JScrollPane(messageArea);
    msgScroll.setBorder(null);
    msgScroll.getViewport().setOpaque(false);
    msgScroll.setOpaque(false);
    inputWrap.add(msgScroll, BorderLayout.CENTER);
    inputRow.add(inputWrap, BorderLayout.CENTER);
    inputRow.add(sendBtn, BorderLayout.EAST);
    inputStack.add(inputRow);
    inputBar.add(inputStack, BorderLayout.CENTER);

    logStripInner = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
    logStripInner.setBackground(MockupTheme.BG1);
    logStripInner.setBorder(new EmptyBorder(4, 12, 4, 12));
    logStripScroll = new JScrollPane(logStripInner);
    logStripScroll.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, MockupTheme.BD));
    logStripScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    logStripScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
    logStripScroll.getViewport().setBackground(MockupTheme.BG1);
    logStripScroll.setPreferredSize(new Dimension(10, 52));

    JPanel southStack = new JPanel();
    southStack.setLayout(new BoxLayout(southStack, BoxLayout.Y_AXIS));
    southStack.setOpaque(false);
    southStack.add(downloadBar);
    southStack.add(inputBar);
    southStack.add(logStripScroll);

    col.add(chatHead, BorderLayout.NORTH);
    col.add(scrollWrap, BorderLayout.CENTER);
    col.add(southStack, BorderLayout.SOUTH);
    return col;
  }

  private void styleSmallButton(JButton b) {
    b.setBackground(MockupTheme.BG2);
    b.setForeground(MockupTheme.T0);
    b.setFocusPainted(false);
    b.setBorder(new LineBorder(MockupTheme.BD2, 1, true));
  }

  private void applyFontScale() {
    float px = fontSlider.getValue();
    ChatRowFactory.UI_SCALE = px / 12f;
    messageArea.setFont(messageArea.getFont().deriveFont(px));
    if (messagesInner != null) {
      messagesInner.revalidate();
      messagesInner.repaint();
    }
  }

  private void signalTyping() {
    if (!loggedIn) {
      return;
    }
    long now = System.currentTimeMillis();
    if (now - lastTypingSignalMs > 2000) {
      lastTypingSignalMs = now;
      new Thread(
              () -> {
                try {
                  net.sendTyping(true);
                } catch (IOException ignored) {
                }
              },
              "typing")
          .start();
    }
    typingStopTimer.restart();
  }

  private static JTextArea findFirstTextArea(Container root) {
    for (Component c : root.getComponents()) {
      if (c instanceof JTextArea ta) {
        return ta;
      }
      if (c instanceof Container co) {
        JTextArea inner = findFirstTextArea(co);
        if (inner != null) {
          return inner;
        }
      }
    }
    return null;
  }

  private void registerBroadcastMessage(long messageId, JPanel row) {
    if (messageId <= 0) {
      return;
    }
    broadcastPanels.put(messageId, row);
    JTextArea ta = findFirstTextArea(row);
    if (ta != null) {
      broadcastBodies.put(messageId, ta);
    }
  }

  private void appendLocalChatLogFile(String line) {
    String user = userField.getText().trim();
    if (user.length() < 2) {
      return;
    }
    Path p = Path.of("chat_local_" + user + ".txt");
    String ts = LOCAL_TIME.format(Instant.now());
    try {
      Files.writeString(
          p,
          "[" + ts + "] " + line + System.lineSeparator(),
          StandardCharsets.UTF_8,
          StandardOpenOption.CREATE,
          StandardOpenOption.APPEND);
    } catch (IOException ignored) {
    }
  }

  private void reconnectLoop() {
    int delaySec = 2;
    for (int i = 0; i < 30 && autoReconnectBox.isSelected() && lastLoginSucceeded; i++) {
      try {
        TimeUnit.SECONDS.sleep(delaySec);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
      if (net.isConnected()) {
        return;
      }
      try {
        String host = hostField.getText().trim();
        int port = Integer.parseInt(portField.getText().trim());
        String user = userField.getText().trim();
        String rp = new String(roomPasswordField.getPassword());
        String rn = roomNameField.getText().trim();
        String ap = new String(accountPasswordField.getPassword());
        net.connect(host, port, user, rp, rn, ap);
        SwingUtilities.invokeLater(
            () -> {
              connectBtn.setEnabled(false);
              appendLogLocal("Yeniden bağlantı soketi açıldı; giriş yanıtı bekleniyor…");
            });
        return;
      } catch (Exception ex) {
        SwingUtilities.invokeLater(
            () -> appendLogLocal("Yeniden bağlanma denemesi: " + ex.getMessage()));
        delaySec = Math.min(30, delaySec + 2);
      }
    }
  }

  private void setConnectionStatus(boolean connected) {
    statusDot.setForeground(connected ? MockupTheme.GR : MockupTheme.T2);
    if (connected) {
      headerStatusLabel.setForeground(MockupTheme.T1);
      headerStatusLabel.setText("Bağlı");
    } else {
      headerStatusLabel.setForeground(MockupTheme.T1);
      headerStatusLabel.setText("Bağlantı yok");
    }
  }

  private void updateHeaderPort() {
    headerPortLabel.setText("port " + portField.getText().trim());
  }

  private void updateTargetPreview() {
    String t = messageArea.getText().stripLeading();
    if (t.startsWith("@")) {
      int sp = t.indexOf(' ');
      if (sp > 1) {
        targetPreview.setText("Özel → " + t.substring(1, sp));
        return;
      }
      targetPreview.setText("Özel mesaj (hedef yazılıyor…)");
      return;
    }
    targetPreview.setText("Genel sohbet");
  }

  private String shortTime(long epochMs) {
    return SHORT_TIME.format(Instant.ofEpochMilli(epochMs));
  }

  private void clearMessages() {
    broadcastPanels.clear();
    broadcastBodies.clear();
    messagesInner.removeAll();
    messagesInner.add(Box.createVerticalGlue());
    messagesInner.revalidate();
    messagesInner.repaint();
  }

  private void scrollChatToBottom() {
    SwingUtilities.invokeLater(
        () -> {
          JScrollBar bar = messagesScroll.getVerticalScrollBar();
          bar.setValue(bar.getMaximum());
        });
  }

  private void appendBubble(JPanel row) {
    messagesInner.add(row);
    messagesInner.revalidate();
    messagesInner.repaint();
    scrollChatToBottom();
  }

  private void rebuildUsersSidebar() {
    usersListPanel.removeAll();
    String me = userField.getText().trim();
    int n = userModel.size();
    onlineCountBadge.setText(String.valueOf(n));
    chatSubtitleLabel.setText(n + " kişi çevrimiçi");
    for (int i = 0; i < n; i++) {
      String u = userModel.getElementAt(i);
      usersListPanel.add(
          ChatRowFactory.userSidebarRow(
              u,
              u.equalsIgnoreCase(me),
              () -> {
                if (!loggedIn || u.equalsIgnoreCase(me)) {
                  return;
                }
                messageArea.setText("@" + u + " ");
                messageArea.requestFocusInWindow();
              }));
    }
    usersListPanel.revalidate();
    usersListPanel.repaint();
  }

  private void rebuildFilesSidebar() {
    filesListPanel.removeAll();
    int n = fileModel.size();
    filesCountBadge.setText(String.valueOf(n));
    for (int i = 0; i < n; i++) {
      FileOffer fo = fileModel.getElementAt(i);
      filesListPanel.add(
          ChatRowFactory.fileSidebarRow(
              fo.filename(),
              fo.fromUser(),
              fo.size(),
              fo.mime(),
              () -> doDownload(fo)));
    }
    filesListPanel.revalidate();
    filesListPanel.repaint();
  }

  private void appendLogLocal(String line) {
    Runnable r =
        () -> {
          String ts = LOCAL_TIME.format(Instant.now());
          JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
          chip.setOpaque(false);
          Font monoBase = new Font(Font.MONOSPACED, Font.PLAIN, 12);
          JLabel t = new JLabel("[" + ts + "]");
          t.setForeground(MockupTheme.T1);
          t.setFont(monoBase);
          JLabel m = new JLabel(line);
          boolean duyuru = line.contains("[DUYURU]");
          m.setForeground(duyuru ? MockupTheme.AM : MockupTheme.T0);
          m.setFont(monoBase.deriveFont(duyuru ? Font.BOLD : Font.PLAIN));
          chip.add(t);
          chip.add(m);
          logStripInner.add(chip);
          logStripInner.revalidate();
          JScrollBar h = logStripScroll.getHorizontalScrollBar();
          SwingUtilities.invokeLater(() -> h.setValue(h.getMaximum()));
        };
    if (SwingUtilities.isEventDispatchThread()) {
      r.run();
    } else {
      SwingUtilities.invokeLater(r);
    }
    appendLocalChatLogFile(line);
  }

  private void doConnect() {
    String host = hostField.getText().trim();
    int port;
    try {
      port = Integer.parseInt(portField.getText().trim());
    } catch (NumberFormatException ex) {
      JOptionPane.showMessageDialog(this, "Geçerli port girin.");
      return;
    }
    String user = userField.getText().trim();
    if (user.length() < 2) {
      JOptionPane.showMessageDialog(this, "Kullanıcı adı en az 2 karakter olmalı.");
      return;
    }
    String pw = new String(roomPasswordField.getPassword());
    String room = roomNameField.getText().trim();
    String acc = new String(accountPasswordField.getPassword());
    updateHeaderPort();
    connectBtn.setEnabled(false);
    new Thread(
            () -> {
              try {
                net.connect(host, port, user, pw, room, acc);
              } catch (IOException ex) {
                SwingUtilities.invokeLater(
                    () -> {
                      connectBtn.setEnabled(true);
                      appendLogLocal("Bağlantı hatası: " + ex.getMessage());
                      JOptionPane.showMessageDialog(this, "Bağlanılamadı: " + ex.getMessage());
                    });
              }
            },
            "connect")
        .start();
  }

  private void doLogout() {
    lastLoginSucceeded = false;
    new Thread(
            () -> {
              try {
                if (net.isConnected()) {
                  net.sendLogout();
                }
              } catch (IOException ignored) {
              } finally {
                net.disconnect();
              }
              SwingUtilities.invokeLater(this::resetUiAfterDisconnect);
            },
            "logout")
        .start();
  }

  private void doRefreshUsers() {
    if (!loggedIn) {
      return;
    }
    new Thread(
            () -> {
              try {
                net.sendListUsersRequest();
              } catch (IOException ex) {
                SwingUtilities.invokeLater(
                    () -> appendLogLocal("Liste isteği gönderilemedi: " + ex.getMessage()));
              }
            },
            "list-users")
        .start();
  }

  private void resetUiAfterDisconnect() {
    loggedIn = false;
    net.stopHeartbeat();
    setConnectionStatus(false);
    connectBtn.setEnabled(true);
    disconnectHeaderBtn.setEnabled(false);
    sendBtn.setEnabled(false);
    messageArea.setEnabled(false);
    refreshBtn.setEnabled(false);
    userModel.clear();
    fileModel.clear();
    filesById.clear();
    clearMessages();
    rebuildUsersSidebar();
    rebuildFilesSidebar();
    appendLogLocal("Bağlantı kapandı.");
  }

  private void doSend() {
    String t = messageArea.getText();
    if (t == null || t.isBlank()) {
      return;
    }
    Optional<PrivateMessageParser.TargetAndBody> pm = PrivateMessageParser.parse(t);
    if (pm.isPresent()) {
      long now = System.currentTimeMillis();
      String cap = pm.get().targetUsername() + "'e özel mesaj";
      SwingUtilities.invokeLater(
          () ->
              appendBubble(
                  ChatRowFactory.outgoingBubbleRow(shortTime(now), pm.get().body(), true, cap)));
    }
    messageArea.setText("");
    updateTargetPreview();
    new Thread(
            () -> {
              try {
                net.sendChat(t);
              } catch (IOException ex) {
                SwingUtilities.invokeLater(
                    () -> appendLogLocal("Gönderim hatası: " + ex.getMessage()));
              }
            },
            "send")
        .start();
  }

  private void doSendFile() {
    if (!loggedIn) {
      return;
    }
    JFileChooser ch = new JFileChooser();
    if (ch.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    Path p = ch.getSelectedFile().toPath();
    new Thread(
            () -> {
              try {
                long sz = Files.size(p);
                if (sz > WARN_FILE_BYTES) {
                  final boolean[] ok = {false};
                  try {
                    SwingUtilities.invokeAndWait(
                        () ->
                            ok[0] =
                                JOptionPane.showConfirmDialog(
                                        ClientApplication.this,
                                        String.format(
                                            "Dosya büyük (%,d MB). Yine de gönderilsin mi?",
                                            sz / (1024 * 1024)),
                                        "Onay",
                                        JOptionPane.YES_NO_OPTION)
                                    == JOptionPane.YES_OPTION);
                  } catch (Exception ignored) {
                    return;
                  }
                  if (!ok[0]) {
                    return;
                  }
                }
                byte[] data = Files.readAllBytes(p);
                if (data.length > 32 * 1024 * 1024) {
                  SwingUtilities.invokeLater(
                      () ->
                          JOptionPane.showMessageDialog(
                              ClientApplication.this,
                              "Dosya 32 MB sınırını aşıyor (sunucu reddeder)."));
                  return;
                }
                net.sendFileOffer(p.getFileName().toString(), data);
                SwingUtilities.invokeLater(
                    () -> appendLogLocal("Dosya sunucuya gönderildi: " + p.getFileName()));
              } catch (Exception ex) {
                SwingUtilities.invokeLater(
                    () -> {
                      appendLogLocal("Dosya hatası: " + ex.getMessage());
                      JOptionPane.showMessageDialog(this, ex.getMessage());
                    });
              }
            },
            "file-offer")
        .start();
  }

  private void doDownload(FileOffer offer) {
    if (offer == null || !loggedIn) {
      JOptionPane.showMessageDialog(this, "Dosya seçilemedi.");
      return;
    }
    JFileChooser ch = new JFileChooser();
    ch.setSelectedFile(new File(offer.filename()));
    if (ch.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }
    Path target = ch.getSelectedFile().toPath();
    new Thread(
            () -> {
              try {
                pendingSavePath = target;
                net.sendFileRequest(offer.id());
              } catch (IOException ex) {
                SwingUtilities.invokeLater(
                    () -> appendLogLocal("İndirme isteği gönderilemedi: " + ex.getMessage()));
              }
            },
            "file-req")
        .start();
  }

  private volatile Path pendingSavePath;

  @Override
  public void onAckLogin(boolean ok, String message) {
    appendLogLocal(message);
    if (ok) {
      lastLoginSucceeded = true;
      loggedIn = true;
      updateHeaderPort();
      setConnectionStatus(true);
      net.startHeartbeatAfterLogin();
      disconnectHeaderBtn.setEnabled(true);
      sendBtn.setEnabled(true);
      messageArea.setEnabled(true);
      refreshBtn.setEnabled(true);
    } else {
      lastLoginSucceeded = false;
      connectBtn.setEnabled(true);
      net.disconnect();
    }
  }

  @Override
  public void onUserList(String csv) {
    userModel.clear();
    if (csv != null && !csv.isBlank()) {
      for (String u : csv.split(",")) {
        if (!u.isBlank()) {
          userModel.addElement(u.trim());
        }
      }
    }
    SwingUtilities.invokeLater(this::rebuildUsersSidebar);
  }

  @Override
  public void onChatBroadcast(String from, long epochMs, long messageId, String text) {
    String me = userField.getText().trim();
    SwingUtilities.invokeLater(
        () -> {
          if (from.equalsIgnoreCase(me)) {
            JPanel row =
                ChatRowFactory.outgoingBubbleRow(
                    shortTime(epochMs),
                    text,
                    false,
                    null,
                    messageId,
                    () -> {
                      String nt =
                          JOptionPane.showInputDialog(
                              ClientApplication.this, "Yeni metin:", text);
                      if (nt != null && !nt.isBlank()) {
                        new Thread(
                                () -> {
                                  try {
                                    net.sendEditMessage(messageId, nt.strip());
                                  } catch (IOException ex) {
                                    SwingUtilities.invokeLater(
                                        () ->
                                            appendLogLocal(
                                                "Düzenleme gönderilemedi: " + ex.getMessage()));
                                  }
                                },
                                "edit-msg")
                            .start();
                      }
                    },
                    () -> {
                      int c =
                          JOptionPane.showConfirmDialog(
                              ClientApplication.this,
                              "Bu mesaj silinsin mi?",
                              "Onay",
                              JOptionPane.YES_NO_OPTION);
                      if (c == JOptionPane.YES_OPTION) {
                        new Thread(
                                () -> {
                                  try {
                                    net.sendDeleteMessage(messageId);
                                  } catch (IOException ex) {
                                    SwingUtilities.invokeLater(
                                        () ->
                                            appendLogLocal(
                                                "Silme gönderilemedi: " + ex.getMessage()));
                                  }
                                },
                                "del-msg")
                            .start();
                      }
                    });
            appendBubble(row);
            registerBroadcastMessage(messageId, row);
          } else {
            JPanel row =
                ChatRowFactory.incomingMessageGroup(
                    from, shortTime(epochMs), text, false, null);
            appendBubble(row);
            registerBroadcastMessage(messageId, row);
          }
          appendLocalChatLogFile(from + ": " + text);
        });
  }

  @Override
  public void onChatPrivate(String from, long epochMs, long messageId, String text) {
    SwingUtilities.invokeLater(
        () -> {
          appendBubble(
              ChatRowFactory.incomingMessageGroup(
                  from,
                  shortTime(epochMs),
                  text,
                  true,
                  from + "'ten özel"));
          appendLocalChatLogFile("(özel) " + from + ": " + text);
        });
  }

  @Override
  public void onNotify(String line) {
    appendLogLocal("Bildirim: " + line);
  }

  @Override
  public void onFileAvailable(
      String id, String fromUser, String filename, long size, String mime) {
    FileOffer fo = new FileOffer(id, fromUser, filename, size, mime);
    filesById.put(id, fo);
    for (int i = fileModel.size() - 1; i >= 0; i--) {
      if (fileModel.get(i).id().equals(id)) {
        fileModel.remove(i);
      }
    }
    fileModel.addElement(fo);
    SwingUtilities.invokeLater(
        () -> {
          rebuildFilesSidebar();
          appendLogLocal("Yeni dosya: " + filename + " (" + fromUser + ")");
        });
  }

  @Override
  public void onFilePayload(String filename, long size, byte[] data) {
    Path dest = pendingSavePath;
    pendingSavePath = null;
    if (dest == null) {
      dest = Path.of(filename);
    }
    Path finalDest = dest;
    new Thread(
            () -> {
              try {
                Files.write(finalDest, data);
                SwingUtilities.invokeLater(
                    () -> appendLogLocal("Dosya kaydedildi: " + finalDest.toAbsolutePath()));
              } catch (IOException ex) {
                SwingUtilities.invokeLater(
                    () -> appendLogLocal("Dosya yazılamadı: " + ex.getMessage()));
              }
            },
            "save-file")
        .start();
  }

  @Override
  public void onFileDownloadProgress(int percent, long current, long total) {
    downloadBar.setVisible(true);
    downloadBar.setValue(Math.min(100, Math.max(0, percent)));
    downloadBar.setString("İndiriliyor % " + percent);
  }

  @Override
  public void onFileDownloadProgressDone() {
    downloadBar.setVisible(false);
    downloadBar.setValue(0);
  }

  @Override
  public void onChatHistory(List<HistoryEntry> entries) {
    if (entries == null || entries.isEmpty()) {
      return;
    }
    String me = userField.getText().trim();
    SwingUtilities.invokeLater(
        () -> {
          JLabel sep = new JLabel("—— Geçmiş (" + entries.size() + " mesaj) ——");
          sep.setForeground(MockupTheme.T2);
          sep.setFont(MockupTheme.FONT_XS);
          sep.setAlignmentX(JLabel.CENTER_ALIGNMENT);
          messagesInner.add(sep);
          for (HistoryEntry e : entries) {
            long mid = e.messageId();
            if (e.fromUser().equalsIgnoreCase(me)) {
              JPanel row =
                  ChatRowFactory.outgoingBubbleRow(
                      shortTime(e.epochMs()),
                      e.text(),
                      false,
                      null,
                      mid,
                      () -> {
                        String nt =
                            JOptionPane.showInputDialog(
                                ClientApplication.this, "Yeni metin:", e.text());
                        if (nt != null && !nt.isBlank()) {
                          new Thread(
                                  () -> {
                                    try {
                                      net.sendEditMessage(mid, nt.strip());
                                    } catch (IOException ex) {
                                      SwingUtilities.invokeLater(
                                          () ->
                                              appendLogLocal(
                                                  "Düzenleme gönderilemedi: " + ex.getMessage()));
                                    }
                                  },
                                  "edit-msg")
                              .start();
                        }
                      },
                      () -> {
                        int c =
                            JOptionPane.showConfirmDialog(
                                ClientApplication.this,
                                "Bu mesaj silinsin mi?",
                                "Onay",
                                JOptionPane.YES_NO_OPTION);
                        if (c == JOptionPane.YES_OPTION) {
                          new Thread(
                                  () -> {
                                    try {
                                      net.sendDeleteMessage(mid);
                                    } catch (IOException ex) {
                                      SwingUtilities.invokeLater(
                                          () ->
                                              appendLogLocal(
                                                  "Silme gönderilemedi: " + ex.getMessage()));
                                    }
                                  },
                                  "del-msg")
                              .start();
                        }
                      });
              messagesInner.add(row);
              registerBroadcastMessage(mid, row);
            } else {
              JPanel row =
                  ChatRowFactory.incomingMessageGroup(
                      e.fromUser(), shortTime(e.epochMs()), e.text(), false, null);
              messagesInner.add(row);
              registerBroadcastMessage(mid, row);
            }
          }
          messagesInner.revalidate();
          messagesInner.repaint();
          scrollChatToBottom();
        });
  }

  @Override
  public void onUserTyping(String username, boolean started) {
    SwingUtilities.invokeLater(
        () -> {
          if (username == null) {
            return;
          }
          String me = userField.getText().trim();
          if (username.equalsIgnoreCase(me)) {
            return;
          }
          if (started) {
            typingStatusLabel.setText(username + " yazıyor…");
            typingClearTimer.restart();
          }
        });
  }

  @Override
  public void onMessageEdited(long messageId, String newText, String editedBy) {
    SwingUtilities.invokeLater(
        () -> {
          JTextArea ta = broadcastBodies.get(messageId);
          if (ta != null) {
            ta.setText(newText);
          }
          appendLogLocal("Mesaj düzenlendi: #" + messageId + " (" + editedBy + ")");
        });
  }

  @Override
  public void onMessageDeleted(long messageId) {
    SwingUtilities.invokeLater(
        () -> {
          JPanel p = broadcastPanels.remove(messageId);
          broadcastBodies.remove(messageId);
          if (p != null) {
            messagesInner.remove(p);
            messagesInner.revalidate();
            messagesInner.repaint();
          }
          appendLogLocal("Mesaj silindi: #" + messageId);
        });
  }

  @Override
  public void onError(String message) {
    appendLogLocal("Hata: " + message);
  }

  @Override
  public void onServerLog(String line) {
    appendLogLocal("Sunucu: " + line);
  }

  @Override
  public void onDisconnected(String reason) {
    appendLogLocal(reason);
    boolean tryReconnect = autoReconnectBox.isSelected() && lastLoginSucceeded;
    net.disconnect();
    resetUiAfterDisconnect();
    if (tryReconnect) {
      new Thread(this::reconnectLoop, "reconnect").start();
    }
  }

  public record FileOffer(String id, String fromUser, String filename, long size, String mime) {}

  public static void main(String[] args) {
    MockupTheme.installSwingDefaults();
    UIManager.put("ScrollBar.thumb", new ColorUIResource(MockupTheme.BG3));
    UIManager.put("ScrollBar.track", new ColorUIResource(MockupTheme.BG0));
    SwingUtilities.invokeLater(
        () -> {
          ClientApplication f = new ClientApplication();
          f.setVisible(true);
        });
  }
}
