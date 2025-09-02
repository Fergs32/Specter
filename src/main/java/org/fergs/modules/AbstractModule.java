package org.fergs.modules;

import lombok.Getter;
import lombok.Setter;

import javax.swing.*;

/**
 * Abstract base class for all modules in the application.
 * Each module has a name, description, and enabled state.
 * Modules must implement methods for enabling, disabling,
 * loading with the main frame, and providing a settings UI panel.
 * <p>
 * Example usage:
 * <pre>
 * public class MyModule extends AbstractModule {
 *     public MyModule() {
 *         super("My Module", "This is a sample module.");
 *     }
 *
 *     @Override
 *     public void onEnable() {
 *         // Code to execute when the module is enabled
 *     }
 *
 *     @Override
 *     public void onDisable() {
 *         // Code to execute when the module is disabled
 *     }
 *
 *     @Override
 *     public void onLoad(JFrame frame) {
 *         // Code to execute when the main frame is loaded
 *     }
 * </pre>
 * </p>
 * @Author Fergs32
 */
public abstract class AbstractModule {
    private final String name;
    private final String description;
    private boolean enabled;
    /**
     * Constructs a new AbstractModule with the specified name and description.
     * @param name The name of the module.
     * @param description A brief description of the module.
     */
    public AbstractModule(String name, String description) {
        this.name = name;
        this.description = description;
        this.enabled = false;
    }
    /**
     * Returns the module's name.
     * @return The name of the module.
     */
    public final String getName() { return name; }
    /**
     * Returns if the module is enabled.
     * @return true if enabled, false otherwise.
     */
    public final boolean isEnabled() { return enabled; }
    /**
     * Enables or disables the module.
     * @param b true to enable, false to disable.
     */
    public final void setEnabled(boolean b) { enabled = b; }
    /**
     * Called when the module is turned on.
     */
    public abstract void onEnable();
    /**
     * Called when the module is turned off.
     */
    public abstract void onDisable();
    /**
     * Called when the main application frame is loaded.
     * @param frame The main application JFrame.
     */
    public abstract void onLoad(JFrame frame);
    /**
     * Provides the UI panel for this module's settings.
     * @return A JPanel containing the module's settings UI.
     */
    public abstract JPanel getUI();
}
