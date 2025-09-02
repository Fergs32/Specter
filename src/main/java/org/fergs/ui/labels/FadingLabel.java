package org.fergs.ui.labels;

import javax.swing.*;
import java.awt.*;

/**
 * A JLabel that can fade in and out by adjusting its alpha transparency.
 * Alpha should be set between 0.0 (fully transparent) and 1.0 (fully opaque).
 * <p>
 * Example usage:
 * <pre>
 * FadingLabel label = new FadingLabel("Hello", new Font("Arial", Font.PLAIN, 24), Color.WHITE);
 * label.setAlpha(0.5f); // Set to 50% opacity
 * </pre>
 * <p>
 * Note: To see the fading effect, the label's parent container should have a non-opaque background.
 * @see JLabel
 * @see AlphaComposite
 *
 * @author Fergs32
 */
public class FadingLabel extends JLabel {
    private float alpha = 0f;
    public FadingLabel(String text, Font font, Color fg) {
        super(text, SwingConstants.CENTER);
        setFont(font);
        setForeground(fg);
        setOpaque(false);
    }
    public void setAlpha(float a) {
        this.alpha = Math.min(1f, Math.max(0f, a));
        repaint();
    }
    @Override
    protected void paintComponent(Graphics g) {
        final Graphics2D g2 = (Graphics2D)g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        super.paintComponent(g2);
        g2.dispose();
    }
}
