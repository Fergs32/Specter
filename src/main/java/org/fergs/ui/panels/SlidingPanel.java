package org.fergs.ui.panels;

import lombok.Getter;
import lombok.Setter;
import org.fergs.utils.JHelper;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * A sliding side panel that expands on hover to show module buttons.
 * The panel starts as a narrow rail with a hamburger icon, and expands
 * to a wider panel displaying enabled modules when hovered over.
 * It collapses back to the rail when the mouse moves away.
 * <p>
 * Example usage:
 * <pre>
 * SlidingPanel panel = new SlidingPanel(50, 200, 10, 100, 400);
 * panel.addModuleLabel("Enabled Modules");
 * panel.addModuleButton("Module 1", 14, e -> System.out.println("Module 1 clicked"));
 * panel.addModuleButton("Module 2", 14, e -> System.out.println("Module 2 clicked"));
 * </pre>
 * </p>
 * Note: Ensure the panel is added to a container with enough space to expand.
 * @see JPanel
 * @author Fergs32
 */
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


        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(collapsedWidth, getPreferredSize().height));
        setBackground(railBackground);

        final JLabel hamburger = new JLabel("\u2630", SwingConstants.CENTER);
        hamburger.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 24));
        hamburger.setOpaque(true);
        hamburger.setBackground(new Color(0x2A2A2A));
        hamburger.setForeground(new Color(0x66FFCC));
        hamburger.setPreferredSize(new Dimension(collapsedWidth, 40));

        add(hamburger, BorderLayout.NORTH);

        final JPanel iconHolder = new JPanel(new BorderLayout());
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

    /** Draw rounded background */
    @Override
    protected void paintComponent(Graphics g) {
        final Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        final Color bg = expanding ? expandedBackground : railBackground;
        g2.setColor(bg);

        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
        g2.dispose();

        super.paintComponent(g);
    }


    private void installGlobalMouseTracker() {
        if (awtMouseTracker != null) {
            return;
        }

        awtMouseTracker = event -> {
            if (!(event instanceof MouseEvent me)) return;
            if (me.getID() != MouseEvent.MOUSE_MOVED) return;

            final Point screenPt = me.getLocationOnScreen();
            final Rectangle panelBounds = new Rectangle(getLocationOnScreen(), getSize());

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
        final JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font("Consolas", Font.BOLD, 16));
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(lbl);
        inner.add(Box.createVerticalStrut(8));
    }

    /** Add each module button */
    public void addModuleButton(String name, int fontSize, ActionListener action) {
        final JButton btn = JHelper.createFancyHoverButton(name, fontSize, true);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        btn.setMaximumSize(new Dimension(expandedWidth - 40, 36));
        btn.addActionListener(action);
        inner.add(btn);
        inner.add(Box.createVerticalStrut(6));
    }

    /** When no modules are enabled */
    public void addEmptyLabel(String text) {
        final JLabel none = new JLabel(text, SwingConstants.CENTER);
        none.setFont(new Font("Consolas", Font.ITALIC, 14));
        none.setForeground(Color.GRAY);
        none.setAlignmentX(Component.CENTER_ALIGNMENT);
        inner.add(none);
    }

    /** Slide‑in/slide‑out animation, and hide inner when fully collapsed */
    private void animate(int step) {
        final int curW = getWidth();
        final int target = expanding ? expandedWidth : collapsedWidth;
        if (curW == target) {
            slideTimer.stop();
            if (!expanding) {
                inner.setVisible(false);
                setBackground(railBackground);
            }
            return;
        }

        final int delta = expanding ? step : -step;
        final int nextW = Math.min(expandedWidth, Math.max(collapsedWidth, curW + delta));

        final int x = getX();
        final int y = getY();
        final int h = getHeight();

        setBounds(x, y, nextW, h);
        revalidate();
        repaint();
    }
}