package org.fergs;

import lombok.Getter;
import lombok.Setter;
import org.fergs.managers.ConfigurationManager;
import org.fergs.managers.ModuleManager;
import org.fergs.ui.forms.InitializationForm;

import javax.swing.*;

@Getter @Setter
public class Specter {
    private static Specter instance;
    public final ConfigurationManager configurationManager;
    public final ModuleManager moduleManager;

    private Specter() {
        this.configurationManager = new ConfigurationManager();
        this.moduleManager = new ModuleManager();
    }
    /**
     * Returns the one and only Specter instance, creating it on first call.
     */
    public static synchronized Specter getInstance() {
        if (instance == null) {
            instance = new Specter();
        }
        return instance;
    }

    public static void main(String[] args) {
        new InitializationForm().display();
    }
}