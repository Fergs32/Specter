package org.fergs.ui;


import lombok.Getter;
import lombok.Setter;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;

/**
 * Abstract base class for creating custom forms in the Specter application.
 * This class sets up a standard JFrame with header, content, and footer regions,
 * as well as a menu bar. Subclasses should implement the initForm method to
 * initialize their specific components.
 *
 * Example usage:
 * <pre>
 * public class MyForm extends AbstractForm {
 *     public MyForm() {
 *         super("My Form", 800, 600);
 *     }
 *
 *     @Override
 *     public void initForm() {
 *         // Initialize form components here
 *     }
 * }
 * </pre>
 * </p>
 *
 * @Author Fergs32
 */
public abstract class AbstractForm extends JFrame {
    private final JPanel header = createRegion(new BorderLayout());
    private final JPanel content = createRegion(new BorderLayout());
    private final JPanel footer = createRegion(new BorderLayout());
    private final JMenuBar menuBar = new JMenuBar();

    public AbstractForm(String title, int w, int h) {
        super(title);

        try {
            var img = ImageIO.read(getClass().getResourceAsStream("/Specter-Icon.png"));
            setIconImage(img);
        } catch (Exception e) {
            System.err.println("Warning: Specter-Logo.png was not found in resources folder, idk how that happened.");
        }

        setUndecorated(true);
        setSize(w, h);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        final JPanel cp = createContentPane();
        cp.setLayout(new BorderLayout());
        setContentPane(cp);

        UIManager.put("Panel.background", new Color(0x1E1E1E));

        cp.add(header, BorderLayout.NORTH);
        cp.add(content, BorderLayout.CENTER);
        cp.add(footer, BorderLayout.SOUTH);

        setJMenuBar(menuBar);
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
    /**
     * Creates a JPanel with the specified layout manager and sets it to be non-opaque.
     * This is used for header, content, and footer regions of the form.
     *
     * @param lm the layout manager to use for the panel
     * @return a new JPanel with the specified layout manager
     */
    private JPanel createRegion(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setOpaque(false);
        return p;
    }
    /**
     * Override this method to set up the form's components.
     * This method is called when the form is displayed.
     */
    protected abstract void initForm();
    /**
     * Displays the form by initializing it and making it visible.
     * This method should be called after all components are set up.
     *
     */
    public void display() {
        initForm();
        SwingUtilities.invokeLater(() -> setVisible(true));
    }
}