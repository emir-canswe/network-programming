package chat.client.ui;

import chat.ui.MockupTheme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.border.EmptyBorder;

/** dark_chat_ui_mockup.html yapısına uygun satırlar. */
public final class ChatRowFactory {

  /** Yazı boyutu ölçeği (istemci ayarı). */
  public static volatile float UI_SCALE = 1f;

  private ChatRowFactory() {}

  private static float scaled(float pt) {
    return pt * UI_SCALE;
  }

  public static JLabel avatarSidebar(String name) {
    return avatar(name, 28, 11);
  }

  public static JLabel avatarChat(String name) {
    return avatar(name, 26, 10);
  }

  private static JLabel avatar(String name, int size, float fontSize) {
    String initial =
        name == null || name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
    JLabel a = new JLabel(initial, SwingConstants.CENTER);
    a.setOpaque(true);
    Color bg = tintBg(name);
    a.setBackground(bg);
    a.setForeground(contrastText(bg));
    a.setFont(a.getFont().deriveFont(Font.BOLD, fontSize));
    Dimension d = new Dimension(size, size);
    a.setPreferredSize(d);
    a.setMinimumSize(d);
    a.setMaximumSize(d);
    return a;
  }

  private static Color tintBg(String name) {
    if (name == null || name.isEmpty()) {
      return MockupTheme.PM_BG;
    }
    int h = Math.abs(name.hashCode() % 360);
    return Color.getHSBColor(h / 360f, 0.55f, 0.22f);
  }

  private static Color contrastText(Color bg) {
    float[] hsb = Color.RGBtoHSB(bg.getRed(), bg.getGreen(), bg.getBlue(), null);
    return Color.getHSBColor(hsb[0], Math.min(1f, hsb[1] + 0.2f), 0.85f);
  }

  /** Gönderen adı + gelen balon (genel sohbet). */
  public static JPanel incomingMessageGroup(
      String from, String time, String body, boolean isPrivate, String privateCaption) {
    JPanel group = new JPanel();
    group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
    group.setOpaque(false);
    if (!isPrivate) {
      JLabel sn = new JLabel(from);
      sn.setFont(MockupTheme.FONT_XS);
      sn.setForeground(contrastText(tintBg(from)));
      sn.setBorder(new EmptyBorder(4, 34, 2, 0));
      group.add(sn);
    }
    group.add(incomingBubbleRow(from, time, body, isPrivate, privateCaption));
    return group;
  }

  public static JPanel userSidebarRow(String username, boolean isSelf, Runnable onDoubleClick) {
    JPanel row = new JPanel(new BorderLayout(8, 0));
    row.setOpaque(true);
    row.setBackground(isSelf ? MockupTheme.BG3 : MockupTheme.BG1);
    row.setBorder(new EmptyBorder(5, 14, 5, 10));
    row.setMaximumSize(new Dimension(Short.MAX_VALUE, 48));

    JLabel av = avatarSidebar(username);
    JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    left.setOpaque(false);
    left.add(av);

    JPanel textCol = new JPanel();
    textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
    textCol.setOpaque(false);
    JLabel name = new JLabel(username);
    name.setForeground(MockupTheme.T0);
    name.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
    JLabel sub = new JLabel(isSelf ? "Sen" : "Üye");
    sub.setFont(MockupTheme.FONT_XS);
    sub.setForeground(MockupTheme.T2);
    textCol.add(name);
    textCol.add(sub);

    JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
    right.setOpaque(false);
    JLabel dot = new JLabel(" ");
    dot.setPreferredSize(new Dimension(8, 8));
    dot.setOpaque(true);
    dot.setBackground(isSelf ? MockupTheme.AC3 : MockupTheme.GR);
    row.add(left, BorderLayout.WEST);
    row.add(textCol, BorderLayout.CENTER);
    right.add(dot);
    row.add(right, BorderLayout.EAST);

    row.addMouseListener(
        new java.awt.event.MouseAdapter() {
          @Override
          public void mouseClicked(java.awt.event.MouseEvent e) {
            if (e.getClickCount() == 2) {
              onDoubleClick.run();
            }
          }
        });
    return row;
  }

  public static JPanel fileSidebarRow(
      String filename,
      String from,
      long sizeBytes,
      String mime,
      Runnable onDownload) {
    JPanel row = new JPanel();
    row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
    row.setOpaque(true);
    row.setBackground(MockupTheme.BG1);
    row.setBorder(new EmptyBorder(6, 10, 6, 10));
    row.setMaximumSize(new Dimension(Short.MAX_VALUE, 120));
    row.setAlignmentX(Component.LEFT_ALIGNMENT);

    JPanel top = new JPanel(new BorderLayout(6, 0));
    top.setOpaque(false);
    top.setAlignmentX(Component.LEFT_ALIGNMENT);
    top.setMaximumSize(new Dimension(Short.MAX_VALUE, 48));

    JPanel iconWrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    iconWrap.setOpaque(false);
    iconWrap.setPreferredSize(new Dimension(28, 28));
    JLabel icon = new JLabel("📄", SwingConstants.CENTER);
    icon.setOpaque(true);
    icon.setBackground(tintBg(from));
    icon.setPreferredSize(new Dimension(28, 28));
    icon.setFont(icon.getFont().deriveFont(12f));
    iconWrap.add(icon);

    JPanel mid = new JPanel();
    mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
    mid.setOpaque(false);
    JLabel fn = new JLabel("<html><div style='width:140px'>" + escapeHtml(filename) + "</div></html>");
    fn.setForeground(MockupTheme.T0);
    fn.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
    String mimeBit =
        mime != null
                && !mime.isBlank()
                && !mime.equals("application/octet-stream")
            ? " · " + mime
            : "";
    JLabel meta = new JLabel("<html><div style='width:140px'>" + escapeHtml(from + " · " + formatSize(sizeBytes) + mimeBit) + "</div></html>");
    meta.setForeground(MockupTheme.T2);
    meta.setFont(MockupTheme.FONT_XS);
    mid.add(fn);
    mid.add(meta);

    top.add(iconWrap, BorderLayout.WEST);
    top.add(mid, BorderLayout.CENTER);

    JButton dl = new JButton("⬇ İndir");
    dl.setAlignmentX(Component.LEFT_ALIGNMENT);
    dl.setMaximumSize(new Dimension(Short.MAX_VALUE, 30));
    dl.setFocusPainted(false);
    dl.setBackground(MockupTheme.AC);
    dl.setForeground(Color.WHITE);
    dl.setBorderPainted(false);
    dl.setFont(dl.getFont().deriveFont(Font.BOLD, 10f));
    dl.setBorder(new EmptyBorder(6, 8, 6, 8));
    dl.addActionListener(e -> onDownload.run());
    dl.addMouseListener(
        new java.awt.event.MouseAdapter() {
          @Override
          public void mouseEntered(java.awt.event.MouseEvent e) {
            dl.setBackground(MockupTheme.AC2);
          }

          @Override
          public void mouseExited(java.awt.event.MouseEvent e) {
            dl.setBackground(MockupTheme.AC);
          }
        });

    row.add(top);
    row.add(Box.createVerticalStrut(6));
    row.add(dl);
    return row;
  }

  private static String escapeHtml(String s) {
    if (s == null) {
      return "";
    }
    return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }

  private static String formatSize(long b) {
    if (b < 1024) {
      return b + " B";
    }
    if (b < 1024 * 1024) {
      return String.format("%,d KB", b / 1024);
    }
    return String.format("%,.1f MB", b / (1024.0 * 1024.0));
  }

  public static JPanel incomingBubbleRow(
      String from, String time, String body, boolean isPrivate, String privateCaption) {
    JPanel row = new JPanel(new BorderLayout(0, 4));
    row.setOpaque(false);
    row.setBorder(new EmptyBorder(2, 12, 8, 40));

    JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    left.setOpaque(false);
    left.add(avatarChat(from));

    Color fill = isPrivate ? MockupTheme.PM_BG : MockupTheme.BG2;
    Color stroke = isPrivate ? MockupTheme.PM_BD : MockupTheme.BD;
    BubblePanel bubble = new BubblePanel(12, fill, stroke);
    bubble.setLayout(new BorderLayout());
    bubble.setBorder(new EmptyBorder(8, 12, 8, 12));

    if (isPrivate) {
      JLabel cap = new JLabel("🔒 " + privateCaption);
      cap.setForeground(MockupTheme.PM_TAG);
      cap.setFont(MockupTheme.FONT_XS);
      bubble.add(cap, BorderLayout.NORTH);
    }

    JTextArea ta = new JTextArea(body);
    ta.setEditable(false);
    ta.setOpaque(false);
    ta.setForeground(MockupTheme.T0);
    ta.setFont(ta.getFont().deriveFont(scaled(12)));
    ta.setLineWrap(true);
    ta.setWrapStyleWord(true);
    ta.setBorder(null);
    ta.setColumns(32);

    JLabel timeL = new JLabel(time);
    timeL.setForeground(MockupTheme.T2);
    timeL.setFont(MockupTheme.FONT_XS);

    JPanel inner = new JPanel();
    inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
    inner.setOpaque(false);
    inner.add(ta);
    inner.add(Box.createVerticalStrut(4));
    inner.add(timeL);

    bubble.add(inner, BorderLayout.CENTER);
    left.add(bubble);
    row.add(left, BorderLayout.WEST);
    return row;
  }

  /**
   * Genel sohbette kendi mesajınız için sağ tık: düzenle / sil (sunucu 2 dk penceresi).
   */
  public static JPanel outgoingBubbleRow(
      String time,
      String body,
      boolean isPrivate,
      String privateCaption,
      long broadcastMessageId,
      Runnable onEdit,
      Runnable onDelete) {
    JPanel row = outgoingBubbleRow(time, body, isPrivate, privateCaption);
    if (!isPrivate && broadcastMessageId >= 0 && onEdit != null && onDelete != null) {
      attachBroadcastPopup(row, onEdit, onDelete);
    }
    return row;
  }

  private static void attachBroadcastPopup(JPanel row, Runnable onEdit, Runnable onDelete) {
    row.addMouseListener(
        new java.awt.event.MouseAdapter() {
          private void maybe(java.awt.event.MouseEvent e) {
            if (!e.isPopupTrigger()) {
              return;
            }
            JPopupMenu m = new JPopupMenu();
            JMenuItem ed = new JMenuItem("Düzenle (2 dk)");
            ed.addActionListener(a -> onEdit.run());
            JMenuItem del = new JMenuItem("Sil (2 dk)");
            del.addActionListener(a -> onDelete.run());
            m.add(ed);
            m.add(del);
            m.show(e.getComponent(), e.getX(), e.getY());
          }

          @Override
          public void mousePressed(java.awt.event.MouseEvent e) {
            maybe(e);
          }

          @Override
          public void mouseReleased(java.awt.event.MouseEvent e) {
            maybe(e);
          }
        });
  }

  public static JPanel outgoingBubbleRow(String time, String body, boolean isPrivate, String privateCaption) {
    JPanel row = new JPanel(new BorderLayout());
    row.setOpaque(false);
    row.setBorder(new EmptyBorder(2, 40, 8, 12));

    JPanel wrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
    wrap.setOpaque(false);

    Color fill = isPrivate ? MockupTheme.ME_PM_BG : MockupTheme.AC;
    BubblePanel bubble = new BubblePanel(12, fill, null);
    bubble.setLayout(new BorderLayout());
    bubble.setBorder(new EmptyBorder(8, 12, 8, 12));

    if (isPrivate) {
      JLabel cap = new JLabel("🔒 " + privateCaption);
      cap.setForeground(MockupTheme.ME_PM_TAG);
      cap.setFont(MockupTheme.FONT_XS);
      bubble.add(cap, BorderLayout.NORTH);
    }

    JTextArea ta = new JTextArea(body);
    ta.setEditable(false);
    ta.setOpaque(false);
    ta.setForeground(Color.WHITE);
    ta.setFont(ta.getFont().deriveFont(scaled(12)));
    ta.setLineWrap(true);
    ta.setWrapStyleWord(true);
    ta.setBorder(null);
    ta.setColumns(32);

    JPanel foot = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
    foot.setOpaque(false);
    JLabel timeL = new JLabel(time + "  ✓✓");
    timeL.setForeground(isPrivate ? MockupTheme.ME_PM_TAG : new Color(0xe0, 0xe0, 0xff));
    timeL.setFont(MockupTheme.FONT_XS);
    foot.add(timeL);

    JPanel inner = new JPanel();
    inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
    inner.setOpaque(false);
    inner.add(ta);
    inner.add(Box.createVerticalStrut(4));
    inner.add(foot);

    bubble.add(inner, BorderLayout.CENTER);
    wrap.add(bubble);
    row.add(wrap, BorderLayout.EAST);
    return row;
  }

  public static JPanel sectionTitleOnline(String title, JLabel countLabel) {
    JPanel p = new JPanel(new BorderLayout());
    p.setOpaque(true);
    p.setBackground(MockupTheme.BG1);
    p.setBorder(new EmptyBorder(10, 12, 6, 12));
    JLabel t = new JLabel(title.toUpperCase());
    t.setForeground(MockupTheme.T2);
    t.setFont(MockupTheme.FONT_SEC_HDR);
    countLabel.setForeground(MockupTheme.AC3);
    countLabel.setFont(MockupTheme.FONT_XS);
    p.add(t, BorderLayout.WEST);
    p.add(countLabel, BorderLayout.EAST);
    return p;
  }

  public static JPanel sectionTitleFiles(String title, JLabel countLabel) {
    JPanel p = new JPanel(new BorderLayout());
    p.setOpaque(true);
    p.setBackground(MockupTheme.BG1);
    p.setBorder(new EmptyBorder(10, 12, 6, 12));
    JLabel t = new JLabel(title.toUpperCase());
    t.setForeground(MockupTheme.T2);
    t.setFont(MockupTheme.FONT_SEC_HDR);
    countLabel.setForeground(MockupTheme.T2);
    countLabel.setFont(MockupTheme.FONT_XS);
    p.add(t, BorderLayout.WEST);
    p.add(countLabel, BorderLayout.EAST);
    return p;
  }

  public static JPanel divider() {
    JPanel d = new JPanel();
    d.setPreferredSize(new Dimension(10, 1));
    d.setMaximumSize(new Dimension(Short.MAX_VALUE, 1));
    d.setBackground(MockupTheme.BD);
    return d;
  }
}
