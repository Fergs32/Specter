package org.fergs.modules.impl.breachdetector;

import org.fergs.modules.AbstractModule;
import org.fergs.scheduler.SpecterScheduler;
import org.fergs.ui.notifications.ToastNotification;
import org.fergs.utils.JHelper;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AvastBreachDetectionUI extends AbstractModule {
    private JPanel ui;

    public AvastBreachDetectionUI() {
        super("email-breach-detection", "Check email against Avast breaches");
        buildUI();
    }

    private void buildUI() {

        ui = new JPanel(new BorderLayout(10, 10));
        ui.setBackground(new Color(0x1E1E1E));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        top.setOpaque(false);

        JLabel emailLabel = new JLabel("Email:");
        emailLabel.setForeground(new Color(0x66FFCC));
        emailLabel.setFont(new Font("Consolas", Font.PLAIN, 14));
        top.add(emailLabel);

        JTextField emailField = new JTextField(20);
        JHelper.styleRoundedField(emailField, 10);
        top.add(emailField);


        JButton runButton = JHelper.createFancyHoverButton("Run", 14);
        top.add(runButton);

        top.add(Box.createHorizontalStrut(20));
        JLabel proxyLabel = new JLabel("Proxy Type:");
        proxyLabel.setForeground(new Color(0x66FFCC));
        proxyLabel.setFont(new Font("Consolas", Font.PLAIN, 14));
        top.add(proxyLabel);

        JToggleButton none  = new JToggleButton("None");
        JToggleButton http  = new JToggleButton("HTTP");
        JToggleButton socks4= new JToggleButton("SOCKS4");
        JToggleButton socks5= new JToggleButton("SOCKS5");

        Color normalBg  = new Color(0x141414);
        Color neon  = new Color(0x39FF14);
        Color normalFg  = new Color(0x39FF14);
        Color selectedFg = new Color(0x141414);

        JPanel toggleGrid = new JPanel(new GridLayout(2, 2, 4, 4));
        toggleGrid.setOpaque(false);
        toggleGrid.setAlignmentY(Component.TOP_ALIGNMENT);

        ButtonGroup bg = new ButtonGroup();
        for (JToggleButton tmpl : List.of(none, http, socks4, socks5)) {
            JButton fancy = JHelper.createFancyHoverButton(tmpl.getText(), 11);

            JToggleButton toggle = new JToggleButton(tmpl.getText());
            toggle.setModel(tmpl.getModel());
            toggle.setFont(fancy.getFont());
            toggle.setBackground(normalBg);
            toggle.setForeground(normalFg);
            toggle.setBorder(fancy.getBorder());
            toggle.setFocusPainted(false);

            for (MouseListener ml : fancy.getMouseListeners()) {
                toggle.addMouseListener(ml);
            }

            toggle.addItemListener(e -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    toggle.setBackground(neon);
                    toggle.setForeground(selectedFg);
                    toggle.setBorder(BorderFactory.createLineBorder(neon.darker(), 4));
                } else {
                    toggle.setBackground(normalBg);
                    toggle.setForeground(normalFg);
                    toggle.setBorder(fancy.getBorder());
                }
            });

            Dimension sz = new Dimension(80, 30);
            toggle.setPreferredSize(sz);
            toggle.setMinimumSize(sz);
            toggle.setMaximumSize(sz);

            bg.add(toggle);
            toggleGrid.add(toggle);
        }

        top.add(toggleGrid);

        ui.add(top, BorderLayout.NORTH);

        JTextArea resultsArea = new JTextArea();
        resultsArea.setEditable(false);
        resultsArea.setBackground(new Color(0x1E1E1E));
        resultsArea.setForeground(new Color(0xCCCCCC));
        resultsArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        JScrollPane sc = new JScrollPane(resultsArea);
        sc.setBorder(BorderFactory.createLineBorder(new Color(0x444444)));
        sc.getViewport().setBackground(new Color(0x1E1E1E));
        ui.add(sc, BorderLayout.CENTER);

        runButton.addActionListener(e -> {
            resultsArea.setText("Loading…\n");
            String proxyType = none.isSelected() ? "NONE"
                    : http.isSelected() ? "HTTP"
                    : socks4.isSelected() ? "SOCKS4"
                    : "SOCKS5";
            new SwingWorker<java.util.List<String>, Void>() {
                @Override
                protected java.util.List<String> doInBackground() {
                    AvastBreachDetectorImpl impl =
                            new AvastBreachDetectorImpl(proxyType, emailField.getText());
                    impl.run();
                    return java.util.List.of("Test Breach 1", "Test Breach 2", "Test Breach 3");
                }
                @Override
                protected void done() {
                    try {
                        java.util.List<String> breaches = get();
                        resultsArea.setText(String.join("\n", breaches));
                    } catch (Exception ex) {
                        resultsArea.setText("Error: " + ex.getMessage());
                    }
                }
            }.execute();
        });
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }

    @Override
    public void onLoad(JFrame frame) {
        ToastNotification.builder(frame)
                .setBackground(new Color(0x2A2A2A))
                .setTitleColor(new Color(0x00FF88))
                .setMessageColor(new Color(0xF5F5F5))
                .setTitleFont(new Font("JetBrains Mono", Font.BOLD, 16))
                .setMessageFont(new Font("JetBrains Mono", Font.PLAIN, 13))
                .setSize(255, 85)
                .setFadeInStep(25)
                .setFadeOutStep(35)
                .setDuration(3500)
                .setTitle("⚡ Module Loaded")
                .setMessage("Name: Avast Breach Detector\nStatus: ✓ Active")
                .show();
    }

    @Override
    public JPanel getUI() {
        return ui;
    }
}
