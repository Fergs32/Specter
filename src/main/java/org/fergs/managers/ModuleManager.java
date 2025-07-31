package org.fergs.managers;

import lombok.Getter;
import lombok.Setter;
import org.fergs.modules.AbstractModule;
import org.fergs.modules.impl.breachdetector.AvastBreachDetectorImpl;

import java.util.*;
import java.util.stream.Collectors;


public class ModuleManager {
    private final Map<String, AbstractModule> registry = new LinkedHashMap<>();

    /** Call once at startup to register all available modules. */
    public void registerModule(AbstractModule module) {
        registry.put(module.getName(), module);
    }

    /** Return the names of modules that are registered. */
    public Set<String> getRegisteredModuleNames() {
        return Collections.unmodifiableSet(registry.keySet());
    }

    /** Return only those modules currently enabled. */
    public List<String> getEnabledModules() {
        return registry.values().stream()
                .filter(AbstractModule::isEnabled)
                .map(AbstractModule::getName)
                .collect(Collectors.toList());
    }

    /** Enable a module (if not already); calls onEnable(). */
    public void enableModule(String name) {
        AbstractModule m = registry.get(name);
        if (m != null && !m.isEnabled()) {
            m.setEnabled(true);
            m.onEnable();
        }
    }

    /** Disable a module; calls onDisable(). */
    public void disableModule(String name) {
        AbstractModule m = registry.get(name);
        if (m != null && m.isEnabled()) {
            m.setEnabled(false);
            m.onDisable();
        }
    }

    /** Get the module object for a name. */
    public AbstractModule getModule(String name) {
        return registry.get(name);
    }
}