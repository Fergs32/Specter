package org.fergs.ui.labels;

import javax.swing.*;
import java.awt.*;

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
        Graphics2D g2 = (Graphics2D)g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        super.paintComponent(g2);
        g2.dispose();
    }
}
