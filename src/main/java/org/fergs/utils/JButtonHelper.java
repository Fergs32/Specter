package org.fergs.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class JButtonHelper {
    /**
     * Helper to create a flat button with hover effect:
     * - Slight background tint
     * - Neon outline on hover
     */
    public static JButton createHoverButton(String text, int size) {
        JButton btn = new JButton(text);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Consolas", Font.BOLD, size));
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
}
