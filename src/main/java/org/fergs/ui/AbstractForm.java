package org.fergs.ui;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AbstractForm extends JFrame {
    private final JPanel header= createRegion(new BorderLayout());
    private final JPanel content = createRegion(new BorderLayout());
    private final JPanel footer = createRegion(new BorderLayout());
    private final JMenuBar menuBar = new JMenuBar();

    public AbstractForm(String title, int w, int h) {
        super(title);
        setUndecorated(true);
        setSize(w, h);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel cp = createContentPane();
        cp.setLayout(new BorderLayout());
        setContentPane(cp);

        UIManager.put("Panel.background", new Color(0x1E1E1E));

        cp.add(header, BorderLayout.NORTH);
        cp.add(content, BorderLayout.CENTER);
        cp.add(footer, BorderLayout.SOUTH);

        setJMenuBar(menuBar);
        initForm();
    }

    /**
     * Override to supply your own content pane (e.g. with animated stars).
     */
    protected JPanel createContentPane() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(true);
        p.setBackground(new Color(0x1E1E1E));
        return p;
    }

    private JPanel createRegion(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setOpaque(false);
        return p;
    }

    protected abstract void initForm();

    /**
     * Fluent menu builder.
     */
    protected AbstractForm menu(String title, Consumer<JMenu> cfg) {
        JMenu m = new JMenu(title);
        m.setForeground(new Color(0x66FFCC));
        m.setBackground(new Color(0x222222));
        cfg.accept(m);
        menuBar.add(m);
        return this;
    }

    public void display() {
        SwingUtilities.invokeLater(() -> setVisible(true));
    }
}