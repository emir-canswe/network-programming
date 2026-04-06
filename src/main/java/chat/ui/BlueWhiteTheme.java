package chat.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.ColorUIResource;

/**
 * Mavi–beyaz ağırlıklı tutarlı Swing görünümü.
 */
public final class BlueWhiteTheme {

  public static final Color BG_WINDOW = new Color(236, 244, 252);
  public static final Color BG_CARD = Color.WHITE;
  public static final Color BG_INPUT = new Color(255, 255, 255);
  public static final Color BG_LIST = new Color(248, 251, 255);
  public static final Color PRIMARY = new Color(33, 118, 198);
  public static final Color PRIMARY_DARK = new Color(21, 87, 156);
  public static final Color SECONDARY = new Color(227, 242, 253);
  public static final Color BORDER = new Color(100, 181, 246);
  public static final Color TEXT = new Color(13, 71, 161);
  public static final Color TEXT_MUTED = new Color(66, 99, 138);

  private BlueWhiteTheme() {}

  /** Herhangi bir Swing penceresi oluşturulmadan önce çağrılmalı. */
  public static void install() {
    try {
      UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
    } catch (Exception ignored) {
    }
    UIManager.put("Panel.background", new ColorUIResource(BG_WINDOW));
    UIManager.put("Label.foreground", new ColorUIResource(TEXT));
    UIManager.put("TextField.background", new ColorUIResource(BG_INPUT));
    UIManager.put("TextField.foreground", new ColorUIResource(TEXT));
    UIManager.put("TextArea.background", new ColorUIResource(BG_INPUT));
    UIManager.put("TextArea.foreground", new ColorUIResource(TEXT));
    UIManager.put("List.background", new ColorUIResource(BG_LIST));
    UIManager.put("List.foreground", new ColorUIResource(TEXT));
    UIManager.put("List.selectionBackground", new ColorUIResource(new Color(41, 128, 220)));
    UIManager.put("List.selectionForeground", new ColorUIResource(Color.WHITE));
    UIManager.put("SplitPane.background", new ColorUIResource(BG_WINDOW));
    UIManager.put("ScrollPane.background", new ColorUIResource(BG_WINDOW));
    UIManager.put("Viewport.background", new ColorUIResource(BG_CARD));
    UIManager.put("OptionPane.background", new ColorUIResource(BG_WINDOW));
  }

  public static Border titledBorder(String title) {
    TitledBorder tb =
        BorderFactory.createTitledBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(6, 8, 8, 8)),
            title);
    tb.setTitleColor(PRIMARY_DARK);
    Font base = UIManager.getFont("Label.font");
    if (base != null) {
      tb.setTitleFont(base.deriveFont(Font.BOLD, 12f));
    }
    return tb;
  }

  public static JPanel cardPanel() {
    JPanel p = new JPanel();
    p.setBackground(BG_CARD);
    p.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(10, 14, 10, 14)));
    return p;
  }

  public static void styleTextField(JTextField f) {
    f.setBackground(BG_INPUT);
    f.setForeground(TEXT);
    f.setCaretColor(PRIMARY_DARK);
    f.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
  }

  public static void stylePasswordField(JPasswordField f) {
    f.setBackground(BG_INPUT);
    f.setForeground(TEXT);
    f.setCaretColor(PRIMARY_DARK);
    f.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
  }

  public static void styleTextArea(JTextArea a) {
    a.setBackground(BG_CARD);
    a.setForeground(TEXT);
    a.setCaretColor(PRIMARY_DARK);
    a.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1, true),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));
  }

  /** Kaydırıcı içinde kullanım: çerçeve scroll pane’de olur. */
  public static void styleTextAreaInScroll(JTextArea a) {
    a.setBackground(BG_CARD);
    a.setForeground(TEXT);
    a.setCaretColor(PRIMARY_DARK);
    a.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
  }

  public static void styleScrollPane(JScrollPane sp) {
    sp.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
    sp.getViewport().setBackground(BG_CARD);
    sp.setBackground(BG_WINDOW);
  }

  public static void stylePrimaryButton(JButton b) {
    b.setBackground(PRIMARY);
    b.setForeground(Color.WHITE);
    b.setOpaque(true);
    b.setContentAreaFilled(true);
    b.setBorderPainted(false);
    b.setFocusPainted(false);
    b.setFont(b.getFont().deriveFont(Font.BOLD));
    b.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
  }

  public static void styleOutlineButton(JButton b) {
    b.setBackground(SECONDARY);
    b.setForeground(PRIMARY_DARK);
    b.setOpaque(true);
    b.setContentAreaFilled(true);
    b.setBorderPainted(true);
    b.setBorder(
        BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PRIMARY, 1, true),
            BorderFactory.createEmptyBorder(7, 14, 7, 14)));
    b.setFocusPainted(false);
  }

  public static JPanel headerBar(String titleText) {
    JPanel p = new JPanel(new BorderLayout());
    p.setBackground(PRIMARY);
    p.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
    JLabel t = new JLabel(titleText);
    t.setForeground(Color.WHITE);
    t.setFont(t.getFont().deriveFont(Font.BOLD, 16f));
    p.add(t, BorderLayout.WEST);
    return p;
  }

  public static JLabel mutedHint(String htmlOrText) {
    JLabel l = new JLabel(htmlOrText);
    l.setForeground(TEXT_MUTED);
    Font base = UIManager.getFont("Label.font");
    if (base != null) {
      l.setFont(base.deriveFont(11f));
    }
    return l;
  }

  public static void styleList(JComponent list) {
    list.setBackground(BG_LIST);
    list.setForeground(TEXT);
  }
}
