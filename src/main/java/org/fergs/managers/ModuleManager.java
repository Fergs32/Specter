package org.fergs.managers;

import lombok.Getter;
import lombok.Setter;
import org.fergs.modules.AbstractModule;

import java.util.*;
import java.util.stream.Collectors;


/**
 * ModuleManager handles the registration, enabling, and disabling of modules.
 * It maintains a registry of available modules and provides methods to interact with them.
 * <p>
 * Example usage:
 * <pre>
 * ModuleManager manager = new ModuleManager();
 * AbstractModule myModule = new MyModule();
 * manager.registerModule(myModule);
 * manager.enableModule("My Module");
 * List<String> enabledModules = manager.getEnabledModules();
 * </pre>
 * </p>
 * @Author Fergs32
 */
@Getter @Setter
public final class ModuleManager {
    private final Map<String, AbstractModule> registry = new LinkedHashMap<>();

    /**
     * Register a module with the manager.
     * @param module The module to register.
     */
    public void registerModule(AbstractModule module) {
        registry.put(module.getName(), module);
    }
    /**
     * Get a set of all registered module names.
     * @return A set of registered module names.
     */
    public Set<String> getRegisteredModuleNames() {
        return Collections.unmodifiableSet(registry.keySet());
    }
    /**
     * Get a list of names of all enabled modules.
     * @return A list of enabled module names.
     */
    public List<String> getEnabledModules() {
        return registry.values().stream()
                .filter(AbstractModule::isEnabled)
                .map(AbstractModule::getName)
                .collect(Collectors.toList());
    }

    /**
     * Enable a module by name; calls onEnable().
     * @param name The name of the module to enable.
     */
    public void enableModule(String name) {
        AbstractModule m = registry.get(name);
        if (m != null && !m.isEnabled()) {
            m.setEnabled(true);
            m.onEnable();
        }
    }
    /**
     * Disable a module by name; calls onDisable().
     * @param name The name of the module to disable.
     */
    public void disableModule(String name) {
        AbstractModule m = registry.get(name);
        if (m != null && m.isEnabled()) {
            m.setEnabled(false);
            m.onDisable();
        }
    }
    /**
     * Get a module by name.
     * @param name The name of the module to retrieve.
     * @return The module instance, or null if not found.
     */
    public AbstractModule getModule(String name) {
        return registry.get(name);
    }
}