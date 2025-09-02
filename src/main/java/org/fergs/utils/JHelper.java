package org.fergs.utils;

import org.fergs.Specter;
import org.fergs.ui.labels.FadingLabel;
import org.fergs.ui.styling.RoundedBorder;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;

/**
 * JHelper provides static utility methods to create and style Swing components
 * with custom appearances and behaviors, such as hover effects, rounded borders,
 * fading animations, and more.
 * <p>
 * Example usage(s):
 * <pre>
 * JButton hoverBtn = JHelper.createHoverButton("Click Me", 16, true);
 * JButton imgBtn = JHelper.createImageButton("/icon.png", 32, "https://example.com");
 * JSlider fancySlider = JHelper.createFancySlider(0, 100, 50);
 * </pre>
 * <p>
 * Note: Some methods require additional classes like FadingLabel and RoundedBorder
 * from the org.fergs.ui package.
 * </p>
 * @see JButton
 * @see JSlider
 * @see FadingLabel
 * @see RoundedBorder
 * @author Fergs32
 */
public final class JHelper {
    /**
     * Helper to create a flat button with hover effect:
     * - Slight background tint
     * - Neon outline on hover
     */
    public static JButton createHoverButton(String text, int textSize, boolean hasSound) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Consolas", Font.BOLD, textSize));
        btn.setBackground(new Color(0x2A2A2A));
        btn.setForeground(new Color(0x66FFCC));
        btn.setBorder(BorderFactory.createLineBorder(new Color(0x444444), 1));
        btn.setOpaque(true);

        Color hoverBg = new Color(0x333333);
        Color normalBg = btn.getBackground();
        Color neon = new Color(0x66FFCC);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hoverBg);
                if (hasSound) {
                    Specter.getInstance().getAudioPlayer().playHoverSound();
                }
                btn.setBorder(BorderFactory.createLineBorder(neon, 2));
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(normalBg);
                btn.setBorder(BorderFactory.createLineBorder(new Color(0x444444), 1));
            }
        });

        return btn;
    }

    /**
     * Creates a transparent JButton displaying a scaled image from the classpath.
     * When clicked, it opens the given URL in the default browser.
     *
     * @param resourcePath path to the image on the classpath (e.g. "/Specter-Logo.png")
     * @param size         width and height to scale the icon to
     * @param url          URL to open when the button is clicked
     * @return the configured JButton
     */
    public static JButton createImageButton(String resourcePath, int size, String url) {
        JButton btn = new JButton();
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);

        try {
            Image img = ImageIO.read(JHelper.class.getResourceAsStream(resourcePath));
            Image scaled = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
            btn.setIcon(new ImageIcon(scaled));
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Failed to load icon: " + resourcePath);
        }

        btn.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        return btn;
    }
    /**
     * Creates a fancy hover button with rounded corners and neon effects.
     * - Rounded corners
     * - Neon border on hover
     *
     * @param text the button text
     * @param size font size for the button text
     * @param hasSound whether to play a sound on hover
     * @return a JButton with custom hover effects
     */
    public static JButton createFancyHoverButton(String text, int size, boolean hasSound) {
        JButton btn = createHoverButton(text, size, hasSound);

        Border normalFancy = new CompoundBorder(
                new RoundedBorder(10, new Color(0x444444)),
                new EmptyBorder(5, 10, 5, 10)
        );
        Border hoverFancy  = new CompoundBorder(
                new RoundedBorder(10, new Color(0x66FFCC)),
                new EmptyBorder(5, 10, 5, 10)
        );


        btn.setBorder(normalFancy);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBorder(hoverFancy);
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBorder(normalFancy);
            }
        });

        return btn;
    }
    /**
     * Creates a custom JSlider with a fancy look:
     * - Rounded track and thumb
     * - Custom colors for track, progress, and thumb
     *
     * @param min   minimum value of the slider
     * @param max   maximum value of the slider
     * @param value initial value of the slider
     * @return a JSlider with custom UI
     */
    public static JSlider createFancySlider(int min, int max, int value) {
        JSlider slider = new JSlider(min, max, value);
        slider.setOpaque(false);
        slider.setPreferredSize(new Dimension(100, 16));
        slider.setPaintTicks(false);
        slider.setPaintTrack(true);
        slider.setPaintLabels(false);
        slider.setFocusable(false);

        Color trackColor    = new Color(0x444444);
        Color progressColor = new Color(0x00FF88);
        Color thumbColor    = new Color(0x00FF88);

        slider.setUI(new BasicSliderUI(slider) {
            @Override
            public void paintTrack(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                int trackY = trackRect.y + (trackRect.height/2) - 2;
                int trackH = 4;
                g2.setColor(trackColor);
                g2.fillRoundRect(trackRect.x, trackY, trackRect.width, trackH, 4, 4);
                int fillW = thumbRect.x + thumbRect.width/2 - trackRect.x;
                g2.setColor(progressColor);
                g2.fillRoundRect(trackRect.x, trackY, fillW, trackH, 4, 4);
            }

            @Override
            public void paintThumb(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                int w = thumbRect.width;
                int x = thumbRect.x;
                int y = thumbRect.y;
                g2.setColor(thumbColor);
                g2.fillOval(x, y, w, w);
                g2.dispose();
            }

            @Override
            protected Dimension getThumbSize() {
                return new Dimension(12, 12);
            }
        });

        return slider;
    }
    /**
     * Fades in a component over a specified number of steps.
     *
     * @param lbl         the FadingLabel to fade in
     * @param delayMs     delay between each fade step in milliseconds
     * @param steps       number of steps for the fade in animation
     * @param onComplete  callback to run after fading in completes
     */
    public static void fadeInComponent(FadingLabel lbl, int delayMs, int steps, Runnable onComplete) {
        lbl.setAlpha(0f);
        Timer timer = new Timer(delayMs, null);
        timer.addActionListener(new ActionListener() {
            int count = 0;
            @Override
            public void actionPerformed(ActionEvent e) {
                count++;
                lbl.setAlpha(count / (float)steps);
                if (count >= steps) {
                    lbl.setAlpha(1f);
                    timer.stop();
                    if (onComplete != null) onComplete.run();
                }
            }
        });
        timer.start();
    }
    /**
     * Fades out a component over a specified number of steps.
     *
     * @param lbl         the FadingLabel to fade out
     * @param delayMs     delay between each fade step in milliseconds
     * @param steps       number of steps for the fade out animation
     * @param onComplete  callback to run after fading out completes
     */
    public static void fadeOutComponent(FadingLabel lbl, int delayMs, int steps, Runnable onComplete) {
        Timer t = new Timer(delayMs, null);
        t.addActionListener(new ActionListener() {
            int count = steps;
            @Override
            public void actionPerformed(ActionEvent e) {
                count--;
                lbl.setAlpha(count / (float)steps);
                if (count <= 0) {
                    t.stop();
                    lbl.setAlpha(0f);
                    if (onComplete != null) onComplete.run();
                }
            }
        });
        t.start();
    }
    /**
     * Fades in a prompt label with the specified text.
     *
     * @param text         the text to display in the prompt
     * @param delayMs      delay before starting the fade in animation
     * @param steps        number of steps for the fade in animation
     * @param promptLabel  the FadingLabel to fade in
     * @param onComplete   callback to run after fading in completes
     */
    public static void fadeInPrompt(String text, int delayMs, int steps, FadingLabel promptLabel, Runnable onComplete) {
        promptLabel.setText(text);
        promptLabel.setAlpha(0f);
        fadeInComponent(promptLabel, delayMs, steps, onComplete);
    }
    /**
     * Holds the prompt for a specified duration before fading it out.
     *
     * @param holdMs       duration to hold the prompt in milliseconds
     * @param delayMs      delay before starting the fade out in milliseconds
     * @param steps        number of steps for the fade out animation
     * @param promptLabel  the FadingLabel to fade out
     * @param onComplete   callback to run after fading out completes
     */
    public static void holdAndFadeOutPrompt(int holdMs, int delayMs, int steps, FadingLabel promptLabel, Runnable onComplete) {
        new Timer(holdMs, e -> {
            ((Timer)e.getSource()).stop();
            fadeOutComponent(promptLabel, delayMs, steps, onComplete);
        }).start();
    }
    /**
     * Styles a JTextField with rounded corners and custom colors.
     *
     * @param tf     the JTextField to style
     * @param radius the corner radius for the rounded border
     */
    public static void styleRoundedField(JTextField tf, int radius) {
        tf.setBackground(new Color(0x2A2A2A));
        tf.setForeground(new Color(0xFFFFFF));
        tf.setCaretColor(Color.WHITE);
        tf.setFont(new Font("Consolas", Font.PLAIN, 14));
        tf.setBorder(new CompoundBorder(
                new RoundedBorder(radius, new Color(0x444444)),
                new EmptyBorder(5, 10, 5, 10)
        ));
    }
}
