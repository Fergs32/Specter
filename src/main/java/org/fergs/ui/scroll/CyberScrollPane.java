package org.fergs.ui.scroll;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

/**
 * A JScrollPane with cyber-themed scroll bars.
 */
public class CyberScrollPane extends JScrollPane {
    public CyberScrollPane(Component content) {
        super(content);
        setBorder(BorderFactory.createLineBorder(new Color(0x444444), 1));
        getViewport().setBackground(new Color(0x1E1E1E));
        initScrollBars();
    }

    private void initScrollBars() {
        final Color trackColor = new Color(0x2A2A2A);
        final Color thumbColor = new Color(0x66FFCC);
        UIManager.put("ScrollBar.width", 12);

        final JScrollBar vbar = getVerticalScrollBar();
        vbar.setUI(new CyberScrollBarUI(trackColor, thumbColor));
        vbar.setUnitIncrement(16);
        vbar.setOpaque(false);

        final JScrollBar hbar = getHorizontalScrollBar();
        hbar.setUI(new CyberScrollBarUI(trackColor, thumbColor));
        hbar.setUnitIncrement(16);
        hbar.setOpaque(false);
    }

    private static class CyberScrollBarUI extends BasicScrollBarUI {
        private Color trackColor;
        private Color thumbColor;

        CyberScrollBarUI(Color trackColor, Color thumbColor) {
            this.trackColor = trackColor;
            this.thumbColor = thumbColor;
        }

        @Override
        protected void configureScrollBarColors() {
            this.trackColor = new Color(0x2A2A2A);
            this.thumbColor = new Color(0x66FFCC);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            final JButton btn = new JButton();
            btn.setPreferredSize(new Dimension(0, 0));
            btn.setMinimumSize(new Dimension(0, 0));
            btn.setMaximumSize(new Dimension(0, 0));
            return btn;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            final Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(trackColor);
            g2.fillRect(r.x, r.y, r.width, r.height);
            g2.dispose();
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            final Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            int arc = r.width;  // roundness
            g2.fillRoundRect(r.x, r.y, r.width, r.height, arc, arc);
            g2.dispose();
        }

        @Override
        protected Dimension getMinimumThumbSize() {
            return new Dimension(12, 30);
        }
    }
}
