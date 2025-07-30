package org.fergs.ui.forms;

import org.fergs.Specter;
import org.fergs.ui.AbstractForm;
import org.fergs.ui.panels.InitializationParticlePanel;
import org.fergs.utils.JButtonHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class SpecterForm extends AbstractForm {

    private Point dragOffset;

    static {
        UIManager.put("Label.foreground", new Color(0x66FFCC));
        UIManager.put("Button.background", new Color(0x2A2A2A));
        UIManager.put("Button.foreground", new Color(0x66FFCC));
        UIManager.put("Button.border", BorderFactory.createLineBorder(new Color(0x444444), 1));
    }

    public SpecterForm() {
        super("Specter", 800, 800);

        final MouseAdapter ma = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }

            public void mouseDragged(MouseEvent e) {
                final Point loc = getLocation();
                setLocation(loc.x + e.getX() - dragOffset.x,
                        loc.y + e.getY() - dragOffset.y);
            }
        };

        getContentPane().addMouseListener(ma);
        getContentPane().addMouseMotionListener(ma);
    }

    @Override
    protected JPanel createContentPane() {
        final InitializationParticlePanel sp = new InitializationParticlePanel();
        sp.setLayout(new BorderLayout());
        return sp;
    }
    @Override
    protected void initForm() {
        final JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(10, 5, 0, 5));

        JPanel windowButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        windowButtons.setOpaque(false);

        Dimension btnSize = new Dimension(40, 40);

        JButton minimize = JButtonHelper.createHoverButton("_", 20);
        minimize.setPreferredSize(btnSize);
        minimize.addActionListener(e -> setExtendedState(JFrame.ICONIFIED));
        windowButtons.add(minimize);

        JButton exit = JButtonHelper.createHoverButton("X", 20);
        exit.setPreferredSize(btnSize);
        exit.addActionListener(e -> System.exit(0));
        windowButtons.add(exit);

        top.add(windowButtons, BorderLayout.EAST);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;

        JLabel title = new JLabel("Specter", SwingConstants.CENTER);
        title.setFont(new Font("Consolas", Font.BOLD, 28));
        title.setForeground(new Color(0x66FFCC));
        center.add(title, gbc);

        gbc.gridy++;
        JLabel moto = new JLabel("Unseen, Unheard & Unstoppable", SwingConstants.CENTER);
        moto.setFont(new Font("Consolas", Font.ITALIC, 16));
        moto.setForeground(new Color(0x888888));
        center.add(moto, gbc);

        top.add(center, BorderLayout.CENTER);
        getContentPane().add(top, BorderLayout.NORTH);

        final JPanel bot = new JPanel(new BorderLayout());
        bot.setOpaque(false);
        bot.setBorder(new EmptyBorder(0, 10, 10, 10));
        JLabel copy = new JLabel("© 2025 Specter Development");
        copy.setFont(new Font("Consolas", Font.PLAIN, 12));
        copy.setForeground(new Color(0x888888));
        bot.add(copy, BorderLayout.WEST);
        getContentPane().add(bot, BorderLayout.SOUTH);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(new Color(0x1E1E1E));
        leftPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel modulesLabel = new JLabel("Enabled Modules");
        modulesLabel.setFont(new Font("Consolas", Font.BOLD, 18));
        modulesLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        leftPanel.add(modulesLabel);
        leftPanel.add(Box.createVerticalStrut(10));

        if (!Specter.getInstance().getModuleManager().getEnabledModules().isEmpty()) {
            for (String moduleName : Specter.getInstance().getModuleManager().getEnabledModules()) {
                JButton moduleButton = JButtonHelper.createHoverButton(moduleName, 14);
                moduleButton.setAlignmentX(Component.CENTER_ALIGNMENT);
                moduleButton.setMaximumSize(new Dimension(200, 40));
                moduleButton.addActionListener(e ->
                        JOptionPane.showMessageDialog(this,
                                "Opening module: " + moduleName,
                                "Module Selected",
                                JOptionPane.INFORMATION_MESSAGE)
                );
                leftPanel.add(moduleButton);
                leftPanel.add(Box.createVerticalStrut(8));
            }
        } else {
            final JLabel noneLabel = new JLabel("No modules enabled.");
            noneLabel.setFont(new Font("Consolas", Font.ITALIC, 14));
            noneLabel.setForeground(Color.GRAY);
            noneLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            leftPanel.add(noneLabel);
        }

        getContentPane().add(leftPanel, BorderLayout.WEST);
    }
}