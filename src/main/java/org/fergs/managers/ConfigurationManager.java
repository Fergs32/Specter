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
 * ConfigurationManager loads one or more YAML files into an in‑memory map,
 * supports reloading, and provides typed getters.
 */
/**
 * Manages multiple named YamlConfigFile instances.
 */
public class ConfigurationManager {
    private final Map<String, YamlConfigFile> files = Collections.synchronizedMap(new LinkedHashMap<>());

    /** Load a classpath YAML into the registry under this name. */
    public void loadFromClasspath(String name, String resourcePath) {
        YamlConfigFile file = YamlConfigFile.loadFromClasspath(resourcePath);
        files.put(name, file);
    }

    /** Load a filesystem YAML into the registry under this name. */
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

    /** Retrieve the named config file, or null if not loaded. */
    public YamlConfigFile getConfig(String name) {
        return files.get(name);
    }

    /** Read-only view of all loaded configs */
    public Map<String, YamlConfigFile> listConfigs() {
        return Collections.unmodifiableMap(files);
    }
}
