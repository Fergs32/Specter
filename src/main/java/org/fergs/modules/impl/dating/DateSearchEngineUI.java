package org.fergs.modules.impl.dating;

import org.fergs.Specter;
import org.fergs.managers.LoggingManager;
import org.fergs.modules.AbstractModule;
import org.fergs.objects.SearchResult;
import org.fergs.ui.forms.SpecterForm;
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
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

/**
 * DateSearchEngineUI provides a user interface for searching dating profiles based on a given name.
 * It allows users to input a name, select a proxy type, and apply various filters to the search results.
 *
 * @Author Fergs32
 */
public final class DateSearchEngineUI extends AbstractModule {
    private final static LoggingManager LOGGER = LoggingManager.getInstance();
    private final JPanel ui;
    private final JPanel resultsPanel;

    // Filter components
    private JComboBox<String> ageRangeCombo;
    private JComboBox<String> locationCombo;
    private JComboBox<String> platformCombo;
    private JComboBox<String> sortByCombo;
    private JSpinner maxResultsSpinner;
    private JCheckBox profilePicsOnlyCheck;
    private JCheckBox verifiedOnlyCheck;
    private List<SearchResult> results = Collections.emptyList();

    public DateSearchEngineUI() {
        super("dating-engine", "Find dating profiles for information gathering");

        ui = new JPanel(new BorderLayout(10,10));
        ui.setBackground(new Color(0x1E1E1E));

        // Create main search panel
        JPanel searchPanel = createSearchPanel();

        // Create filters panel
        JPanel filtersPanel = createFiltersPanel();

        // Combine search and filters in a tabbed pane to save vertical space
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(new Color(0x1E1E1E));
        tabbedPane.setForeground(new Color(0x66FFCC));
        tabbedPane.setFont(new Font("Consolas", Font.PLAIN, 12));

        tabbedPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        tabbedPane.addTab("Search", searchPanel);
        tabbedPane.addTab("Filters", filtersPanel);

        ui.add(tabbedPane, BorderLayout.NORTH);

        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        resultsPanel.setBackground(new Color(0x1E1E1E));
        JScrollPane scrollPane = new CyberScrollPane(resultsPanel);
        ui.add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        searchPanel.setBackground(new Color(0x1E1E1E));
        searchPanel.setFont(new Font("Consolas", Font.PLAIN, 14));
        searchPanel.setForeground(new Color(0x66FFCC));
        //searchPanel.setOpaque(false);
        // reove the border glow
        searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));


        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setForeground(new Color(0x66FFCC));
        nameLabel.setFont(new Font("Consolas", Font.PLAIN, 14));
        searchPanel.add(nameLabel);

        JTextField nameField = new JTextField(20);
        JHelper.styleRoundedField(nameField, 10);
        searchPanel.add(nameField);

        JButton runButton = JHelper.createFancyHoverButton("Search", 14, true);
        searchPanel.add(runButton);

        searchPanel.add(Box.createHorizontalStrut(20));
        JLabel proxyLabel = new JLabel("Proxy:");
        proxyLabel.setForeground(new Color(0x66FFCC));
        proxyLabel.setFont(new Font("Consolas", Font.PLAIN, 14));
        searchPanel.add(proxyLabel);

        // Proxy toggle buttons
        JToggleButton none = new JToggleButton("None");
        JToggleButton http = new JToggleButton("HTTP");
        JToggleButton socks4 = new JToggleButton("SOCKS4");
        JToggleButton socks5 = new JToggleButton("SOCKS5");
        ButtonGroup bg = new ButtonGroup();
        JPanel toggles = new JPanel(new GridLayout(2,2,4,4));
        toggles.setBackground(new Color(0x1E1E1E));
        toggles.setOpaque(true);

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
        searchPanel.add(toggles);

        // Add search button action listener
        runButton.addActionListener(e -> {
            resultsPanel.removeAll();
            String name = nameField.getText().trim();
            String proxyType = none.isSelected() ? "NONE"
                    : http.isSelected() ? "HTTP"
                    : socks4.isSelected() ? "SOCKS4"
                    : "SOCKS5";

            performSearch(name, proxyType);
        });

        return searchPanel;
    }

    private JPanel createFiltersPanel() {
        JPanel filtersPanel = new JPanel(new GridBagLayout());
        filtersPanel.setBackground(new Color(0x1E1E1E));
        filtersPanel.setFont(new Font("Consolas", Font.PLAIN, 14));
        filtersPanel.setForeground(new Color(0x66FFCC));
        //filtersPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Age Range Filter
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel ageLabel = createFilterLabel("Age Range:");
        filtersPanel.add(ageLabel, gbc);

        gbc.gridx = 1;
        ageRangeCombo = createStyledComboBox(new String[]{
                "Any", "18-25", "26-35", "36-45", "46-55", "55+"
        });
        filtersPanel.add(ageRangeCombo, gbc);

        // Location Filter
        gbc.gridx = 2; gbc.gridy = 0;
        JLabel locationLabel = createFilterLabel("Location:");
        filtersPanel.add(locationLabel, gbc);

        gbc.gridx = 3;
        locationCombo = createStyledComboBox(new String[]{
                "Any", "Within 10 miles", "Within 25 miles", "Within 50 miles", "Same city", "Same state"
        });
        filtersPanel.add(locationCombo, gbc);

        // Platform Filter
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel platformLabel = createFilterLabel("Platform:");
        filtersPanel.add(platformLabel, gbc);

        gbc.gridx = 1;
        platformCombo = createStyledComboBox(new String[]{
                "All", "Tinder", "Bumble", "Hinge", "Match", "POF", "OkCupid", "Badoo"
        });
        filtersPanel.add(platformCombo, gbc);

        // Sort By Filter
        gbc.gridx = 2; gbc.gridy = 1;
        JLabel sortLabel = createFilterLabel("Sort By:");
        filtersPanel.add(sortLabel, gbc);

        gbc.gridx = 3;
        sortByCombo = createStyledComboBox(new String[]{
                "Relevance", "Most Recent", "Distance", "Activity", "Profile Quality"
        });
        filtersPanel.add(sortByCombo, gbc);

        // Max Results
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel maxLabel = createFilterLabel("Max Results:");
        filtersPanel.add(maxLabel, gbc);

        gbc.gridx = 1;
        maxResultsSpinner = new JSpinner(new SpinnerNumberModel(50, 10, 500, 10));
        styleSpinner(maxResultsSpinner);
        filtersPanel.add(maxResultsSpinner, gbc);

        // Profile Pictures Only
        gbc.gridx = 2; gbc.gridy = 2;
        profilePicsOnlyCheck = createStyledCheckbox("Photos Only");
        filtersPanel.add(profilePicsOnlyCheck, gbc);

        // Verified Profiles Only
        gbc.gridx = 3; gbc.gridy = 2;
        verifiedOnlyCheck = createStyledCheckbox("Verified Only");
        filtersPanel.add(verifiedOnlyCheck, gbc);

        // Clear Filters Button
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        JButton clearFiltersBtn = JHelper.createFancyHoverButton("Clear Filters", 12, false);
        clearFiltersBtn.addActionListener(e -> clearAllFilters());
        filtersPanel.add(clearFiltersBtn, gbc);

        // Apply Filters Button
        gbc.gridx = 2; gbc.gridy = 3;
        gbc.gridwidth = 2;
        JButton applyFiltersBtn = JHelper.createFancyHoverButton("Apply Filters", 12, true);
        applyFiltersBtn.addActionListener(e -> applyFiltersToResults());

        filtersPanel.add(applyFiltersBtn, gbc);

        return filtersPanel;
    }

    private JLabel createFilterLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(0x66FFCC));
        label.setFont(new Font("Consolas", Font.PLAIN, 12));
        return label;
    }

    private JComboBox<String> createStyledComboBox(String[] items) {
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setBackground(new Color(0x141414));
        combo.setForeground(new Color(0x66FFCC));
        combo.setFont(new Font("Consolas", Font.PLAIN, 11));
        combo.setBorder(BorderFactory.createLineBorder(new Color(0x39FF14), 1));
        combo.setPreferredSize(new Dimension(120, 25));
        return combo;
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setBackground(new Color(0x141414));
        spinner.setForeground(new Color(0x66FFCC));
        spinner.setFont(new Font("Consolas", Font.PLAIN, 11));
        spinner.setBorder(BorderFactory.createLineBorder(new Color(0x39FF14), 1));
        spinner.setPreferredSize(new Dimension(80, 25));

        // Style the editor
        JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
        editor.getTextField().setBackground(new Color(0x141414));
        editor.getTextField().setForeground(new Color(0x66FFCC));
        editor.getTextField().setCaretColor(new Color(0x66FFCC));
    }

    private JCheckBox createStyledCheckbox(String text) {
        JCheckBox checkbox = new JCheckBox(text);
        checkbox.setOpaque(false);
        checkbox.setForeground(new Color(0x66FFCC));
        checkbox.setFont(new Font("Consolas", Font.PLAIN, 11));
        checkbox.setFocusPainted(false);

        // Custom checkbox icon
        checkbox.setIcon(createCheckboxIcon(false));
        checkbox.setSelectedIcon(createCheckboxIcon(true));

        return checkbox;
    }

    private Icon createCheckboxIcon(boolean selected) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(new Color(0x141414));
                g2.fillRect(x, y, getIconWidth(), getIconHeight());

                g2.setColor(new Color(0x39FF14));
                g2.drawRect(x, y, getIconWidth()-1, getIconHeight()-1);

                if (selected) {
                    g2.setStroke(new BasicStroke(2));
                    g2.drawLine(x+3, y+7, x+6, y+10);
                    g2.drawLine(x+6, y+10, x+11, y+4);
                }

                g2.dispose();
            }

            @Override
            public int getIconWidth() { return 14; }

            @Override
            public int getIconHeight() { return 14; }
        };
    }

    private void clearAllFilters() {
        ageRangeCombo.setSelectedIndex(0);
        locationCombo.setSelectedIndex(0);
        platformCombo.setSelectedIndex(0);
        sortByCombo.setSelectedIndex(0);
        maxResultsSpinner.setValue(50);
        profilePicsOnlyCheck.setSelected(false);
        verifiedOnlyCheck.setSelected(false);
    }

    private void applyFiltersToResults() {
        ToastNotification.builder(SpecterForm.frame)
                .setTitle("Applying Filters")
                .setBackground(new Color(0x2A2A2A))
                .setTitleColor(new Color(0x00FF88))
                .setMessage("Filters Applied")
                .show();

        // so we've stored our original results, now we just need to re-apply the filter
    }

    private void performSearch(String name, String proxyType) {
        new SwingWorker<List<SearchResult>, SearchResult>() {
            @Override
            protected List<SearchResult> doInBackground() {
                // Get filter values
                String ageRange = (String) ageRangeCombo.getSelectedItem();
                String location = (String) locationCombo.getSelectedItem();
                String platform = (String) platformCombo.getSelectedItem();
                String sortBy = (String) sortByCombo.getSelectedItem();
                int maxResults = (Integer) maxResultsSpinner.getValue();
                boolean photosOnly = profilePicsOnlyCheck.isSelected();
                boolean verifiedOnly = verifiedOnlyCheck.isSelected();

                // Create enhanced search implementation with filters
                DateSearchEngineImpl impl = new DateSearchEngineImpl(
                        Specter.getInstance().getConfigurationManager().loadLinesFromClasspath("proxies.txt"),
                        proxyType,
                        name,
                        ageRange,
                        location,
                        platform,
                        sortBy,
                        maxResults,
                        photosOnly,
                        verifiedOnly
                );

                LOGGER.info("Starting search for name: " + name + " using proxy type: " + proxyType);

                // You would modify DateSearchEngineImpl to accept these filter parameters
                // For now, using the existing implementation
                return impl.run();
            }

            @Override
            protected void done() {
                try {
                    results = get();
                    int maxResults = (Integer) maxResultsSpinner.getValue();

                    // Apply client-side filtering
                    results = results.stream()
                            .limit(maxResults)
                            .filter(r -> !profilePicsOnlyCheck.isSelected() ||
                                    (r.thumbnail != null && !r.thumbnail.isEmpty()))
                            .toList();

                    for (SearchResult r : results) {
                        JPanel entry = createResultEntry(r);
                        resultsPanel.add(entry);
                        resultsPanel.add(Box.createVerticalStrut(8));
                    }
                    resultsPanel.revalidate();
                    resultsPanel.repaint();

                    ToastNotification.builder(SpecterForm.frame)
                            .setBackground(new Color(0x2A2A2A))
                            .setTitleColor(new Color(0x00FF88))
                            .setMessage("Found " + results.size() + " results")
                            .show();

                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error during search: ", ex.getMessage());
                }
            }
        }.execute();
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
                LOGGER.log(Level.WARNING, "Failed to load image: ", ex.getMessage());
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