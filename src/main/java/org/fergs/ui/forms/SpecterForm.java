package org.fergs.ui.forms;

import org.fergs.Specter;
import org.fergs.managers.ModuleManager;
import org.fergs.modules.AbstractModule;
import org.fergs.modules.impl.breachdetector.AvastBreachDetectionUI;
import org.fergs.scheduler.SpecterScheduler;
import org.fergs.ui.AbstractForm;
import org.fergs.ui.panels.InitializationParticlePanel;
import org.fergs.ui.panels.SlidingPanel;
import org.fergs.utils.JHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.concurrent.TimeUnit;

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
        getRootPane().setBorder(BorderFactory.createEmptyBorder());

        final JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(10, 5, 0, 5));

        JPanel windowButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        windowButtons.setOpaque(false);

        Dimension btnSize = new Dimension(40, 40);

        JButton minimize = JHelper.createHoverButton("_", 20);
        minimize.setPreferredSize(btnSize);
        minimize.addActionListener(e -> setExtendedState(JFrame.ICONIFIED));
        windowButtons.add(minimize);

        JButton exit = JHelper.createHoverButton("X", 20);
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

        Specter.getInstance().getModuleManager().registerModule(new AvastBreachDetectionUI());

        SlidingPanel modulesPanel = new SlidingPanel(50, 220, 10, 10, (getHeight() - 50));
        modulesPanel.setRailBackground(new Color(0x2A2A2A));
        modulesPanel.setExpandedBackground(new Color(0x2A2A2A));
        modulesPanel.addModuleLabel("Enabled Modules");
        var enabled = Specter.getInstance().getModuleManager().getRegisteredModuleNames();
        if (!enabled.isEmpty()) {
            for (String name : Specter.getInstance().getModuleManager().getRegisteredModuleNames()) {
                AbstractModule mod = Specter.getInstance().getModuleManager().getModule(name);
                modulesPanel.addModuleButton(name, 14, e -> {
                    Specter.getInstance().getModuleManager().enableModule(name);
                    JPanel moduleUI = mod.getUI();
                    moduleUI.setPreferredSize(new Dimension(650, 475));
                    moduleUI.setOpaque(false);
                    moduleUI.setBorder(new EmptyBorder(3, 3, 3, 3));

                    JPanel contentRegion = getContentRegion();
                    contentRegion.removeAll();
                    contentRegion.setLayout(new GridBagLayout());
                    GridBagConstraints gridBagConstraints = new GridBagConstraints();
                    gridBagConstraints.gridx = 0;
                    gridBagConstraints.gridy = 0;
                    gridBagConstraints.weightx = 1;
                    gridBagConstraints.weighty = 1;
                    gridBagConstraints.anchor = GridBagConstraints.CENTER;
                    gridBagConstraints.fill = GridBagConstraints.NONE;
                    contentRegion.add(moduleUI, gridBagConstraints);
                    contentRegion.revalidate();
                    contentRegion.repaint();
                });

                SpecterScheduler.schedule(() -> {
                    mod.onLoad(this);
                }, 1000, TimeUnit.MILLISECONDS);
            }
        } else {
            modulesPanel.addEmptyLabel("No modules enabled.");
        }

        modulesPanel.setBounds(10, 10,
                modulesPanel.getPreferredSize().width,
                getHeight() - 50);
        getRootPane().getLayeredPane()
                .add(modulesPanel, JLayeredPane.PALETTE_LAYER);
    }

    private JPanel getContentRegion() {
        Container cp = getContentPane();
        for (Component c : cp.getComponents()) {
            if (BorderLayout.CENTER.equals(
                    ((BorderLayout)cp.getLayout())
                            .getConstraints(c))) {
                return (JPanel)c;
            }
        }

        return (JPanel)cp;
    }
}