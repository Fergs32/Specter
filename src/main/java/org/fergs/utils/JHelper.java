package org.fergs.utils;

import org.fergs.ui.labels.FadingLabel;
import org.fergs.ui.styling.RoundedBorder;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class JHelper {
    /**
     * Helper to create a flat button with hover effect:
     * - Slight background tint
     * - Neon outline on hover
     */
    public static JButton createHoverButton(String text, int textSize) {
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

    public static JButton createFancyHoverButton(String text, int size) {
        JButton btn = createHoverButton(text, size);

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

    public static void fadeInPrompt(String text, int delayMs, int steps, FadingLabel promptLabel, Runnable onComplete) {
        promptLabel.setText(text);
        promptLabel.setAlpha(0f);
        fadeInComponent(promptLabel, delayMs, steps, onComplete);
    }

    public static void holdAndFadeOutPrompt(int holdMs, int delayMs, int steps, FadingLabel promptLabel, Runnable onComplete) {
        new Timer(holdMs, e -> {
            ((Timer)e.getSource()).stop();
            fadeOutComponent(promptLabel, delayMs, steps, onComplete);
        }).start();
    }

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
