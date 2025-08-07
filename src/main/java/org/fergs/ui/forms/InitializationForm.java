package org.fergs.ui.forms;

import javafx.animation.RotateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.shape.Sphere;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.fergs.Specter;
import org.fergs.configuration.YamlConfigFile;
import org.fergs.managers.ConfigurationManager;
import org.fergs.ui.AbstractForm;
import org.fergs.ui.labels.FadingLabel;
import org.fergs.ui.panels.InitializationParticlePanel;
import org.fergs.ui.panels.SpherePanel;
import org.fergs.utils.JHelper;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.fergs.utils.JHelper.*;

public class InitializationForm extends AbstractForm {

    private Point dragOffset;
    private FadingLabel titleLabel;
    private FadingLabel motoLabel;
    private FadingLabel copyLabel;
    private JLabel progressLabel;
    private FadingLabel promptLabel;
    private SpherePanel globe;

    static {
        UIManager.put("Label.foreground", new Color(0x66FFCC));
        UIManager.put("Button.background", new Color(0x2A2A2A));
        UIManager.put("Button.foreground", new Color(0x66FFCC));
        UIManager.put("Button.border", BorderFactory.createLineBorder(new Color(0x444444), 1));
    }

    public InitializationForm() {
        super("", 500, 300);

        MouseAdapter ma = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }

            public void mouseDragged(MouseEvent e) {
                Point loc = getLocation();
                setLocation(loc.x + e.getX() - dragOffset.x,
                        loc.y + e.getY() - dragOffset.y);
            }
        };
        getContentPane().addMouseListener(ma);
        getContentPane().addMouseMotionListener(ma);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                fadeInPrompt("Welcome back, Specter.", 30, 20, promptLabel, ()  ->
                        holdAndFadeOutPrompt(2000, 30, 20, promptLabel,  () ->
                                fadeInPrompt("Let me get things set up for you.", 30, 20, promptLabel, () ->
                                        holdAndFadeOutPrompt(2000, 30, 20, promptLabel, () -> {
                                            fadeInComponent(titleLabel, 30, 20, () ->
                                                    fadeInComponent(motoLabel, 30, 20, () ->
                                                            fadeInComponent(copyLabel, 30, 20, () -> {
                                                                globe.start();
                                                                globe.setVisible(true);
                                                                progressLabel.setVisible(true);
                                                                startLoading();
                                                            })
                                                    )
                                            );
                                        })
                                )
                        )
                );
            }
        });
    }

    @Override
    protected JPanel createContentPane() {
        InitializationParticlePanel sp = new InitializationParticlePanel();
        sp.setLayout(new BorderLayout());
        return sp;
    }

    @Override
    protected void initForm() {
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.setBorder(new EmptyBorder(10, 10, 0, 10));

        JButton exit = JHelper.createHoverButton("X", 30, false);
        exit.addActionListener(e -> System.exit(0));
        top.add(exit, BorderLayout.EAST);

        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));

        promptLabel = new FadingLabel("", new Font("Consolas", Font.PLAIN, 18), new Color(0x66FFCC));
        promptLabel.setAlpha(0f);
        getContentPane().add(promptLabel, BorderLayout.CENTER);

        titleLabel = new FadingLabel("Specter", new Font("Consolas", Font.BOLD, 28), new Color(0x66FFCC));
        titles.add(titleLabel);

        motoLabel = new FadingLabel("Unseen, Unheard & Unstoppable", new Font("Consolas", Font.ITALIC, 16), new Color(0x888888));
        titles.add(motoLabel);

        top.add(titles, BorderLayout.CENTER);
        getContentPane().add(top, BorderLayout.NORTH);
        JPanel bot = new JPanel(new BorderLayout());
        bot.setOpaque(false);
        bot.setBorder(new EmptyBorder(0, 10, 10, 10));

        copyLabel = new FadingLabel("© 2025 Specter Development", new Font("Consolas", Font.PLAIN, 12), new Color(0x888888));
        bot.add(copyLabel, BorderLayout.WEST);

        progressLabel = new JLabel("");
        progressLabel.setFont(new Font("Consolas", Font.PLAIN, 12));
        progressLabel.setForeground(new Color(0x66FFCC));
        progressLabel.setBorder(new EmptyBorder(0, 0, 0, 10));
        progressLabel.setVisible(false);
        bot.add(progressLabel, BorderLayout.EAST);

        globe = new SpherePanel(100);
        globe.setOpaque(false);
        globe.setVisible(false);
        globe.setBounds((500-300)/2,(300-300)/2,500,500);

        getContentPane().add(globe, BorderLayout.WEST);

        getContentPane().add(bot, BorderLayout.SOUTH);
    }

    private void startLoading() {
        new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() {
                try {
                    ConfigurationManager cfgm = Specter.getInstance().getConfigurationManager();

                    publish("Loading configuration(s)...");
                    cfgm.loadFromClasspath("modules", "modules.yml");
                    Thread.sleep(1000);
                    return null;
                } catch (Exception e) {
                    e.printStackTrace();
                    publish("Error during initialization: " + e.getMessage());
                    return null;
                }
            }


            @Override
            protected void process(List<String> chunks) {
                progressLabel.setText(chunks.get(chunks.size() - 1));
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
                    System.exit(1);
                }
            }
        }.execute();
    }
}