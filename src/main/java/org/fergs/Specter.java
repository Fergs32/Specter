package org.fergs;

import lombok.Getter;
import lombok.Setter;
import org.fergs.managers.ConfigurationManager;
import org.fergs.managers.ModuleManager;
import org.fergs.ui.forms.InitializationForm;
import org.fergs.utils.AudioPlayer;

import javax.swing.*;

/**
 * Specter is the main singleton class for the application.
 * It holds references to the ConfigurationManager, ModuleManager, and AudioPlayer.
 * It also defines the application version.
 */
@Getter @Setter
public class Specter {
    private static Specter instance;
    public final ConfigurationManager configurationManager;
    public final ModuleManager moduleManager;
    public final AudioPlayer audioPlayer;
    public final String VERSION = "v1.0.0";

    private Specter() {
        this.configurationManager = new ConfigurationManager();
        this.moduleManager = new ModuleManager();
        this.audioPlayer = new AudioPlayer();
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