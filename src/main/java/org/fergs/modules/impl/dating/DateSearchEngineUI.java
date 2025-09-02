package org.fergs.modules.impl.dating;

import org.fergs.Specter;
import org.fergs.modules.AbstractModule;
import org.fergs.objects.SearchResult;
import org.fergs.ui.notifications.ToastNotification;
import org.fergs.ui.scroll.CyberScrollPane;
import org.fergs.utils.JHelper;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * DateSearchEngineUI provides a user interface for searching dating profiles based on a given name.
 * It allows users to input a name, select a proxy type, and view the results of the search.
 *
 * @Author Fergs32
 */
public final class DateSearchEngineUI extends AbstractModule {
    private final JPanel ui;
    private final JPanel resultsPanel;

    public DateSearchEngineUI() {
        super("dating-engine", "Find dating profiles for information gathering");

        ui = new JPanel(new BorderLayout(10,10));
        ui.setBackground(new Color(0x1E1E1E));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        top.setOpaque(false);

        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setForeground(new Color(0x66FFCC));
        nameLabel.setFont(new Font("Consolas", Font.PLAIN, 14));
        top.add(nameLabel);

        JTextField nameField = new JTextField(20);
        JHelper.styleRoundedField(nameField, 10);
        top.add(nameField);

        JButton runButton = JHelper.createFancyHoverButton("Search", 14, true);
        top.add(runButton);

        top.add(Box.createHorizontalStrut(20));
        JLabel proxyLabel = new JLabel("Proxy:");
        proxyLabel.setForeground(new Color(0x66FFCC));
        proxyLabel.setFont(new Font("Consolas", Font.PLAIN, 14));
        top.add(proxyLabel);

        JToggleButton none = new JToggleButton("None");
        JToggleButton http = new JToggleButton("HTTP");
        JToggleButton socks4 = new JToggleButton("SOCKS4");
        JToggleButton socks5 = new JToggleButton("SOCKS5");
        ButtonGroup bg = new ButtonGroup();
        JPanel toggles = new JPanel(new GridLayout(2,2,4,4));
        toggles.setOpaque(false);
        Color normalBg  = new Color(0x141414);
        Color neon  = new Color(0x39FF14);
        Color normalFg  = new Color(0x39FF14);
        Color selectedFg = new Color(0x141414);
        for (JToggleButton tmpl : List.of(none, http, socks4, socks5)) {
            JButton fancy = JHelper.createFancyHoverButton(tmpl.getText(), 10, false);

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
            toggles.add(toggle);
        }
        none.setSelected(true);
        top.add(toggles);

        ui.add(top, BorderLayout.NORTH);

        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setBackground(new Color(0x1E1E1E));
        JScrollPane scrollPane = new CyberScrollPane(resultsPanel);
        ui.add(scrollPane, BorderLayout.CENTER);

        runButton.addActionListener(e -> {
            resultsPanel.removeAll();
            String name = nameField.getText().trim();
            String proxyType = none.isSelected() ? "NONE"
                    : http.isSelected() ? "HTTP"
                    : socks4.isSelected() ? "SOCKS4"
                    : "SOCKS5";

            new SwingWorker<List<SearchResult>, SearchResult>() {
                @Override
                protected List<SearchResult> doInBackground() {
                    DateSearchEngineImpl impl = new DateSearchEngineImpl(Specter.getInstance().getConfigurationManager().loadLinesFromClasspath("proxies.txt"), proxyType, name);
                    return impl.run();
                }
                @Override
                protected void done() {
                    try {
                        for (SearchResult r : get()) {
                            JPanel entry = createResultEntry(r);
                            resultsPanel.add(entry);
                            resultsPanel.add(Box.createVerticalStrut(8));
                        }
                        resultsPanel.revalidate();
                        resultsPanel.repaint();
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(ui, "Error: "+ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        });
    }

    private JPanel createResultEntry(SearchResult r) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        card.setBackground(new Color(0x1A1A1A));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x00FF88), 1),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));

        JLabel pic = new JLabel();
        pic.setPreferredSize(new Dimension(80, 80));
        pic.setOpaque(true);
        pic.setBackground(new Color(0x121212));
        pic.setHorizontalAlignment(SwingConstants.CENTER);

        if (r.thumbnail != null && !r.thumbnail.isEmpty()) {
            try {
                URL url = new URL(r.thumbnail);
                BufferedImage img = ImageIO.read(url);
                if (img != null) {
                    Image scaled = img.getScaledInstance(80, 80, Image.SCALE_SMOOTH);
                    pic.setIcon(new ImageIcon(scaled));
                }
            } catch (Exception ex) {
                System.err.println("Failed to load thumbnail: " + ex.getMessage());
            }
        } else {
            pic.setText("👤");
            pic.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        }

        pic.setUI(new javax.swing.plaf.basic.BasicLabelUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setClip(new java.awt.geom.Ellipse2D.Float(0, 0, c.getWidth(), c.getHeight()));
                super.paint(g2, c);
                g2.dispose();
            }
        });

        card.add(pic, BorderLayout.WEST);

        // Info Panel (Right)
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel title = new JLabel(r.title);
        title.setFont(new Font("JetBrains Mono", Font.BOLD, 16));
        title.setForeground(new Color(0x00FF88));

        JLabel link = new JLabel(r.url);
        link.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        link.setForeground(new Color(0xBBBBBB));

        info.add(title);
        info.add(Box.createVerticalStrut(5));
        info.add(link);

        card.add(info, BorderLayout.CENTER);

        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI(r.url));
                } catch (Exception ignored) {}
            }
        });

        return card;
    }

    @Override public void onEnable() { /* nothing */ }
    @Override public void onDisable(){ /* nothing */ }
    @Override public void onLoad(JFrame frame){
        ToastNotification.builder(frame)
                .setBackground(new Color(0x2A2A2A))
                .setTitleColor(new Color(0x00FF88))
                .setMessage("Dating Engine Loaded")
                .show();
    }
    @Override public JPanel getUI(){ return ui; }
}