package org.fergs.managers;

import lombok.Getter;
import lombok.Setter;
import org.fergs.configuration.YamlConfigFile;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;


/**
 * ConfigurationManager handles loading and accessing YAML configuration files.
 * It supports loading from classpath resources and provides access to loaded configurations.
 * Configurations are stored in a thread-safe map and can be retrieved by name.
 * <p>
 * Example usage:
 * <pre>
 * ConfigurationManager configManager = new ConfigurationManager();
 * configManager.loadFromClasspath("appConfig", "config/app.yaml");
 * YamlConfigFile appConfig = configManager.getConfig("appConfig");
 * </pre>
 * </p>
 * @Author Fergs32
 */
@Getter @Setter
public final class ConfigurationManager {
    private final Map<String, YamlConfigFile> files = Collections.synchronizedMap(new LinkedHashMap<>());
    /**
     * Load a classpath YAML into the registry under this name.
     * @param name the name to register this config under.
     * @param resourcePath the classpath resource path to load the YAML from.
     */
    public void loadFromClasspath(String name, String resourcePath) {
        YamlConfigFile file = YamlConfigFile.loadFromClasspath(resourcePath);
        files.put(name, file);
    }
    /**
     * Load lines from a classpath resource, trimming whitespace and ignoring empty lines.
     * @param resourcePath the classpath resource path to load lines from.
     * @return a list of non-empty, trimmed lines.
     */
    public List<String> loadLinesFromClasspath(String resourcePath) {
        InputStream in = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourcePath);
        if (in == null) throw new RuntimeException("Resource not found: " + resourcePath);
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
            return r.lines()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to read lines: " + resourcePath, e);
        }
    }
    /**
     * Get a loaded config by name.
     * @param name the name the config was registered under.
     * @return the YamlConfigFile, or null if not found.
     */
    public YamlConfigFile getConfig(String name) {
        return files.get(name);
    }
    /**
     * List all loaded configs.
     * @return an unmodifiable map of config names to YamlConfigFile instances.
     */
    public Map<String, YamlConfigFile> listConfigs() {
        return Collections.unmodifiableMap(files);
    }
}
