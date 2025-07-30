package org.fergs.managers;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;


@Getter @Setter
public class ModuleManager {
    public Set<String> enabledModules = new HashSet<>();

    public ModuleManager() {

    }


    public void loadModule(String name) {
        try {
            enabledModules.add(name);
            Thread.sleep(500);
        } catch (InterruptedException ignored) {}
    }

    public void unloadModule(String name) {
        try {
            enabledModules.remove(name);
            Thread.sleep(500);
        } catch (InterruptedException ignored) {}
    }
}