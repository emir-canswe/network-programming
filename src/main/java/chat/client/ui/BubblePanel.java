package chat.client.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/** Mockup’taki .bubble — dolgu + isteğe bağlı çerçeve. */
public final class BubblePanel extends JPanel {

  private final int radius;
  private Color fill;
  private final Color stroke;

  public BubblePanel(int radius, Color fill, Color stroke) {
    super(null);
    this.radius = radius;
    this.fill = fill;
    this.stroke = stroke;
    setOpaque(false);
  }

  public void setFill(Color c) {
    this.fill = c;
    repaint();
  }

  @Override
  protected void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setColor(fill);
    g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
    if (stroke != null) {
      g2.setColor(stroke);
      g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
    }
    g2.dispose();
    super.paintComponent(g);
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension d = super.getPreferredSize();
    return new Dimension(Math.max(d.width, 40), Math.max(d.height, 24));
  }
}
