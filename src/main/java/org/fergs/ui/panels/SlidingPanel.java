package org.fergs.ui.panels;

import lombok.Getter;
import lombok.Setter;
import org.fergs.utils.JHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

@Getter @Setter
public final class SlidingPanel extends JPanel {
    private final Box inner;
    private final int collapsedWidth;
    private final int expandedWidth;
    private final Timer slideTimer;
    private boolean expanding;

    private final int startY;
    private final int panelHeight;

    private AWTEventListener awtMouseTracker;

    private Color railBackground  = new Color(0x2A2A2A);
    private Color expandedBackground  = new Color(0x232323);

    public SlidingPanel(int collapsedWidth, int expandedWidth, int stepSize, int startY, int panelHeight) {
        this.collapsedWidth = collapsedWidth;
        this.expandedWidth = expandedWidth;
        this.startY = startY;
        this.panelHeight = panelHeight;

        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(collapsedWidth, getPreferredSize().height));
        setBackground(railBackground);

        JLabel hamburger = new JLabel("\u2630", SwingConstants.CENTER);
        hamburger.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 24));
        hamburger.setOpaque(true);
        hamburger.setBackground(new Color(0x2A2A2A));
        hamburger.setForeground(new Color(0x66FFCC));
        hamburger.setPreferredSize(new Dimension(collapsedWidth, 40));

        add(hamburger, BorderLayout.NORTH);

        JPanel iconHolder = new JPanel(new BorderLayout());
        iconHolder.setOpaque(false);
        iconHolder.add(Box.createVerticalStrut(8), BorderLayout.NORTH);
        iconHolder.add(hamburger, BorderLayout.CENTER);

        add(iconHolder, BorderLayout.NORTH);

        inner = Box.createVerticalBox();
        inner.setOpaque(false);
        inner.setVisible(false);
        add(inner, BorderLayout.CENTER);

        slideTimer = new Timer(15, e -> animate(stepSize));
        slideTimer.setRepeats(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                expanding = true;
                setBackground(expandedBackground);
                inner.setVisible(true);
                startSlide();
                installGlobalMouseTracker();
            }
        });
    }

    private void startSlide() {
        if (!slideTimer.isRunning()) slideTimer.start();
    }

    private void installGlobalMouseTracker() {
        if (awtMouseTracker != null) {
            return;
        }

        awtMouseTracker = event -> {
            if (!(event instanceof MouseEvent me)) return;
            if (me.getID() != MouseEvent.MOUSE_MOVED) return;

            Point screenPt = me.getLocationOnScreen();
            Rectangle panelBounds = new Rectangle(getLocationOnScreen(), getSize());

            if (!panelBounds.contains(screenPt)) {
                expanding = false;
                setBackground(railBackground);
                inner.setVisible(false);
                startSlide();

                Toolkit.getDefaultToolkit()
                        .removeAWTEventListener(awtMouseTracker);
                awtMouseTracker = null;
            }
        };

        Toolkit.getDefaultToolkit().addAWTEventListener(
                awtMouseTracker,
                AWTEvent.MOUSE_MOTION_EVENT_MASK
        );
    }

    /** Tweak the collapsed‐rail color */
    public void setRailBackground(Color c) {
        this.railBackground = c;
        if (!expanding) setBackground(c);
    }
    /** Tweak the expanded panel color */
    public void setExpandedBackground(Color c) {
        this.expandedBackground = c;
        if (expanding) setBackground(c);
    }

    /** Add your “Enabled Modules” label */
    public void addModuleLabel(String text) {
        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font("Consolas", Font.BOLD, 16));
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(lbl);
        inner.add(Box.createVerticalStrut(8));
    }

    /** Add each module button */
    public void addModuleButton(String name, int fontSize, ActionListener action) {
        JButton btn = JHelper.createHoverButton(name, fontSize);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(expandedWidth - 40, 36));
        btn.addActionListener(action);
        inner.add(btn);
        inner.add(Box.createVerticalStrut(6));
    }

    /** When no modules are enabled */
    public void addEmptyLabel(String text) {
        JLabel none = new JLabel(text, SwingConstants.CENTER);
        none.setFont(new Font("Consolas", Font.ITALIC, 14));
        none.setForeground(Color.GRAY);
        none.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(none);
    }

    /** Slide‑in/slide‑out animation, and hide inner when fully collapsed */
    private void animate(int step) {
        int curW   = getWidth();
        int target = expanding ? expandedWidth : collapsedWidth;
        if (curW == target) {
            slideTimer.stop();
            if (!expanding) {
                inner.setVisible(false);
                setBackground(railBackground);
            }
            return;
        }

        int delta = expanding ? step : -step;
        int nextW = Math.min(expandedWidth, Math.max(collapsedWidth, curW + delta));

        int insetX = getX();
        setBounds(insetX, startY, nextW, panelHeight);

        getParent().revalidate();
        getParent().repaint();
    }
}