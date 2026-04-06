package chat.ui;

import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.ColorUIResource;

/**
 * dark_chat_ui_mockup.html ile aynı renk paleti (--bg0 … --ac …).
 */
public final class MockupTheme {

  public static final Color BG0 = new Color(0x07_07_0d);
  public static final Color BG1 = new Color(0x0e_0e_18);
  public static final Color BG2 = new Color(0x13_13_1f);
  public static final Color BG3 = new Color(0x1a_1a_28);

  public static final Color BD = new Color(0x28, 0x28, 0x36);
  public static final Color BD2 = new Color(0x38, 0x38, 0x4a);

  public static final Color AC = new Color(0x7c_3a_ed);
  public static final Color AC2 = new Color(0x6d_28_d9);
  public static final Color AC3 = new Color(0x8b_5c_f6);

  public static final Color GR = new Color(0x22_c5_5e);
  public static final Color RD = new Color(0xef_44_44);
  public static final Color AM = new Color(0xf5_9e_0b);

  public static final Color T0 = new Color(0xf1_f5_f9);
  public static final Color T1 = new Color(0x94_a3_b8);
  public static final Color T2 = new Color(0x47_56_69);
  public static final Color T3 = new Color(0x2a_2a_3a);

  public static final Color PM_BG = new Color(0x1c_10_28);
  public static final Color PM_BD = new Color(0x3b_1d_6e);
  public static final Color PM_TAG = new Color(0xa7_8b_fa);
  public static final Color ME_PM_BG = new Color(0x3b_0e_6e);
  public static final Color ME_PM_TAG = new Color(0xc4_b5_fd);

  public static final Color INFO = new Color(0x60_a5_fa);

  public static final Font FONT_UI = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
  public static final Font FONT_SM = new Font(Font.SANS_SERIF, Font.PLAIN, 11);
  public static final Font FONT_XS = new Font(Font.SANS_SERIF, Font.PLAIN, 10);
  public static final Font FONT_SEC_HDR = new Font(Font.SANS_SERIF, Font.BOLD, 10);

  private MockupTheme() {}

  public static void installSwingDefaults() {
    try {
      UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
    } catch (Exception ignored) {
    }
    UIManager.put("Panel.background", new ColorUIResource(BG0));
    UIManager.put("Label.foreground", new ColorUIResource(T0));
    UIManager.put("TextField.background", new ColorUIResource(BG2));
    UIManager.put("TextField.foreground", new ColorUIResource(T0));
    UIManager.put("TextArea.background", new ColorUIResource(BG0));
    UIManager.put("TextArea.foreground", new ColorUIResource(T1));
    UIManager.put("List.background", new ColorUIResource(BG1));
    UIManager.put("List.foreground", new ColorUIResource(T0));
    UIManager.put("ScrollPane.background", new ColorUIResource(BG0));
    UIManager.put("Viewport.background", new ColorUIResource(BG0));
  }

  public static void styleField(JTextField f) {
    f.setBackground(BG2);
    f.setForeground(T0);
    f.setCaretColor(AC3);
    f.setFont(FONT_SM);
    f.setBorder(
        BorderFactory.createCompoundBorder(
            new LineBorder(BD2, 1, true), new EmptyBorder(6, 10, 6, 10)));
  }

  public static void stylePassword(JPasswordField f) {
    f.setBackground(BG2);
    f.setForeground(T0);
    f.setCaretColor(AC3);
    f.setFont(FONT_SM);
    f.setBorder(
        BorderFactory.createCompoundBorder(
            new LineBorder(BD2, 1, true), new EmptyBorder(6, 10, 6, 10)));
  }

  public static void styleTextAreaMono(JTextArea a) {
    a.setBackground(BG0);
    a.setForeground(T1);
    a.setCaretColor(AC3);
    a.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
    a.setBorder(new EmptyBorder(8, 10, 8, 10));
  }

  public static void styleAccentButton(JButton b) {
    b.setBackground(AC);
    b.setForeground(Color.WHITE);
    b.setFocusPainted(false);
    b.setBorderPainted(false);
    b.setFont(b.getFont().deriveFont(Font.BOLD, 11f));
    b.setBorder(new EmptyBorder(8, 14, 8, 14));
  }

  public static void styleGhostButton(JButton b) {
    b.setOpaque(true);
    b.setBackground(BG1);
    b.setForeground(T1);
    b.setFocusPainted(false);
    b.setBorder(
        BorderFactory.createCompoundBorder(
            new LineBorder(BD2, 1, true), new EmptyBorder(5, 10, 5, 10)));
    b.setFont(FONT_SM);
  }

  public static void styleGhostButtonTransparent(JButton b) {
    b.setOpaque(false);
    b.setContentAreaFilled(false);
    b.setForeground(T1);
    b.setFocusPainted(false);
    b.setBorder(
        BorderFactory.createCompoundBorder(
            new LineBorder(BD2, 1, true), new EmptyBorder(5, 10, 5, 10)));
    b.setFont(FONT_SM);
  }

  public static void styleDangerGhostButton(JButton b) {
    styleGhostButtonTransparent(b);
    b.addMouseListener(
        new java.awt.event.MouseAdapter() {
          @Override
          public void mouseEntered(java.awt.event.MouseEvent e) {
            b.setOpaque(true);
            b.setContentAreaFilled(true);
            b.setBackground(new Color(0x7f_1d_1d));
            b.setForeground(RD);
            b.setBorder(
                BorderFactory.createCompoundBorder(
                    new LineBorder(RD, 1, true), new EmptyBorder(5, 10, 5, 10)));
          }

          @Override
          public void mouseExited(java.awt.event.MouseEvent e) {
            b.setOpaque(false);
            b.setContentAreaFilled(false);
            b.setBackground(new Color(0, 0, 0, 0));
            b.setForeground(T1);
            b.setBorder(
                BorderFactory.createCompoundBorder(
                    new LineBorder(BD2, 1, true), new EmptyBorder(5, 10, 5, 10)));
          }
        });
  }

  public static void styleList(JComponent list) {
    list.setBackground(BG1);
    list.setForeground(T0);
  }
}
