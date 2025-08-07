package org.fergs.configuration;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents one YAML file, loaded into a nested Map.
 * Supports dot-path getters.
 */
public class YamlConfigFile {
    private final Map<String, Object> root;

    private YamlConfigFile(Map<String, Object> root) {
        this.root = root;
    }

    /** Load from classpath resource (e.g. "/modules.yml") */
    public static YamlConfigFile loadFromClasspath(String resourcePath) {
        LoaderOptions opts = new LoaderOptions();
        opts.setMaxAliasesForCollections(Integer.MAX_VALUE);
        Yaml yaml = new Yaml(new SafeConstructor(opts));

        String rp = resourcePath.startsWith("/")
                ? resourcePath.substring(1)
                : resourcePath;

        InputStream in = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(rp);
        if (in == null) {
            throw new RuntimeException("Resource not found: " + resourcePath);
        }
        try (in) {
            Object loaded = yaml.load(in);
            if (!(loaded instanceof Map<?,?> m)) {
                throw new RuntimeException(
                        "Expected YAML mapping in " + resourcePath
                );
            }
            @SuppressWarnings("unchecked")
            Map<String,Object> map = (Map<String,Object>) m;
            return new YamlConfigFile(map);
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to load YAML from classpath: " + resourcePath, e
            );
        }
    }

    /** Load from filesystem path */
    public static YamlConfigFile loadFromFile(Path filePath) {
        LoaderOptions opts = new LoaderOptions();
        opts.setMaxAliasesForCollections(Integer.MAX_VALUE);
        Yaml yaml = new Yaml(new SafeConstructor(opts));

        try (InputStream in = Files.newInputStream(filePath)) {
            Object loaded = yaml.load(in);
            if (!(loaded instanceof Map<?,?> m)) {
                throw new RuntimeException("Expected YAML mapping in file: " + filePath);
            }
            @SuppressWarnings("unchecked")
            Map<String,Object> map = (Map<String,Object>) m;
            return new YamlConfigFile(map);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load YAML from file: " + filePath, e);
        }
    }

    /** Retrieve raw object at dot-path, or null */
    public Object getRaw(String path) {
        String[] parts = path.split("\\.");
        Object curr = root;
        for (String key : parts) {
            if (!(curr instanceof Map<?,?> m)) return null;
            curr = m.get(key);
            if (curr == null) return null;
        }
        return curr;
    }

    /** Get a String, or null */
    public String getString(String path) {
        Object o = getRaw(path);
        return o != null ? o.toString() : null;
    }

    /** Get an int, or default */
    public int getInt(String path, int def) {
        Object o = getRaw(path);
        if (o instanceof Number n)
            return n.intValue();

        try {
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /** Get a boolean, or default */
    public boolean getBoolean(String path, boolean def) {
        Object o = getRaw(path);
        if (o instanceof Boolean b) return b;
        if (o instanceof String s) return Boolean.parseBoolean(s);
        return def;
    }

    /** Get a List<String>, or empty */
    @SuppressWarnings("unchecked")
    public List<String> getStringList(String path) {
        Object o = getRaw(path);
        if (o instanceof List<?> L) {
            List<String> out = new ArrayList<>();
            for (Object e : L) out.add(e == null ? "" : e.toString());
            return out;
        }
        return Collections.emptyList();
    }

    /** Get a sub-section as a Map (read-only), or empty */
    @SuppressWarnings("unchecked")
    public Map<String,Object> getSection(String path) {
        Object o = getRaw(path);
        if (o instanceof Map<?,?> m) return Collections.unmodifiableMap((Map<String,Object>)m);
        return Collections.emptyMap();
    }

    /** Entire contents as read-only map */
    public Map<String,Object> asMap() {
        return Collections.unmodifiableMap(root);
    }
}