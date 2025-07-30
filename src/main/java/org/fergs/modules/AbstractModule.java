package org.fergs.modules;


import lombok.Getter;
import lombok.Setter;

@Getter @Setter
abstract class AbstractModule {
    public final String name;
    public final String description;
    public boolean enabled;
    /**
     * Constructor for AbstractModule.
     * @param name The name of the module.
     * @param description A brief description of the module.
     */
    public AbstractModule(String name, String description) {
        this.name = name;
        this.description = description;
        this.enabled = false; // Default to disabled
    }
    /**
     * Override to implement module-specific logic when enabled.
     */
    public abstract void onEnable();

    /**
     * Override to implement module-specific logic when disabled.
     */
    public abstract void onDisable();
}
