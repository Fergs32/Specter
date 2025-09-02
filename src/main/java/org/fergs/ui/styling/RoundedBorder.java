package org.fergs.ui.styling;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/**
 * A simple rounded border implementation for Swing components.
 * <p>
 * Usage example:
 * <pre>
 * JButton button = new JButton("Click Me");
 * button.setBorder(new RoundedBorder(10, Color.BLUE));
 * </pre>
 * Note: The component should have opaque set to false to see the rounded corners properly.
 * @see Border
 * @see JButton
 *
 * @author Fergs32
 */
public final class RoundedBorder implements Border {

    private final int radius;
    private final Color color;

    public RoundedBorder(int radius, Color color) {
        this.radius = radius;
        this.color = color;
    }

    @Override public Insets getBorderInsets(Component c) {
        return new Insets(radius, radius, radius, radius);
    }

    @Override public boolean isBorderOpaque() {
        return false;
    }

    @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, w-1, h-1, radius, radius);
        g2.dispose();
    }
}
