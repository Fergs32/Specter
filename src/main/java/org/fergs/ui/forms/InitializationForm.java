package org.fergs.ui.forms;

import org.fergs.Specter;
import org.fergs.managers.ModuleManager;
import org.fergs.ui.AbstractForm;
import org.fergs.ui.panels.InitializationParticlePanel;
import org.fergs.utils.JButtonHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class InitializationForm extends AbstractForm {

    private Point dragOffset;
    private JLabel progressLabel;
    private List<String> enabledModules;

    static {
        UIManager.put("Label.foreground", new Color(0x66FFCC));
        UIManager.put("Button.background", new Color(0x2A2A2A));
        UIManager.put("Button.foreground", new Color(0x66FFCC));
        UIManager.put("Button.border", BorderFactory.createLineBorder(new Color(0x444444), 1));
    }

    public InitializationForm() {
        super("", 500, 300);

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

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                startLoading();
            }
        });
    }

    @Override
    protected JPanel createContentPane() {
        final InitializationParticlePanel sp = new InitializationParticlePanel();
        sp.setLayout(new BorderLayout());
        return sp;
    }

    @Override
    protected void initForm() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(10, 10, 0, 10));

        JButton exit = JButtonHelper.createHoverButton("X", 30);
        exit.addActionListener(e -> System.exit(0));
        top.add(exit, BorderLayout.EAST);

        final JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Specter", SwingConstants.CENTER);
        title.setFont(new Font("Consolas", Font.BOLD, 28));
        titles.add(title);

        JLabel moto = new JLabel("Unseen, Unheard & Unstoppable", SwingConstants.CENTER);
        moto.setFont(new Font("Consolas", Font.ITALIC, 16));
        moto.setForeground(new Color(0x888888));
        titles.add(moto);
        top.add(titles, BorderLayout.CENTER);
        getContentPane().add(top, BorderLayout.NORTH);

        JPanel bot = new JPanel(new BorderLayout());
        bot.setOpaque(false);
        bot.setBorder(new EmptyBorder(0, 10, 10, 10));

        JLabel copy = new JLabel("© 2025 Specter Development");
        copy.setFont(new Font("Consolas", Font.PLAIN, 12));
        copy.setForeground(new Color(0x888888));
        bot.add(copy, BorderLayout.WEST);

        progressLabel = new JLabel("Loading modules...");
        progressLabel.setFont(new Font("Consolas", Font.PLAIN, 12));
        progressLabel.setForeground(new Color(0x66FFCC));
        progressLabel.setBorder(new EmptyBorder(0, 0, 0, 10));
        bot.add(progressLabel, BorderLayout.EAST);

        getContentPane().add(bot, BorderLayout.SOUTH);
    }

    private void startLoading() {
        final SwingWorker<Void, String> loader = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                Specter.getInstance().getConfigurationManager()
                        .loadFromClasspath("modules.yml");
                enabledModules = Specter
                        .getInstance()
                        .getConfigurationManager()
                        .getStringList("enabled-modules");

                for (final String moduleName : enabledModules) {
                    publish("Loading " + moduleName + "...");
                    Specter.getInstance().getModuleManager().loadModule(moduleName);
                }
                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                progressLabel.setText(chunks.getLast());
            }

            @Override
            protected void done() {
                try {
                    get();
                    SwingUtilities.invokeLater(() -> {
                        new SpecterForm().display();
                        dispose();
                    });
                } catch (InterruptedException | ExecutionException ex) {
                    ex.printStackTrace();
                    System.exit(1);
                }
            }
        };
        loader.execute();
    }
}