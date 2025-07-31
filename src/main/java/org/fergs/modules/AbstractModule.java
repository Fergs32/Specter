package org.fergs.modules;


import lombok.Getter;
import lombok.Setter;

import javax.swing.*;


@Getter @Setter
public abstract class AbstractModule {
    private final String name;
    private final String description;
    private boolean enabled;

    public AbstractModule(String name, String description) {
        this.name = name;
        this.description = description;
        this.enabled = false;
    }

    public final String getName()       { return name; }
    public final boolean isEnabled()    { return enabled; }
    public final void setEnabled(boolean b) { enabled = b; }

    /** Called when the module is turned on. */
    public abstract void onEnable();

    /** Called when the module is turned off. */
    public abstract void onDisable();

    public abstract void onLoad(JFrame frame);

    /** Return the panel that this module wants displayed in the center area. */
    public abstract JPanel getUI();
}
