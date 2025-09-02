package org.fergs.configuration;

import lombok.Getter;
import lombok.Setter;
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
 * Can load from classpath resource or filesystem path.
 * <pre>
 * Example usage:
 * <pre>
 * YamlConfigFile config = YamlConfigFile.loadFromClasspath("/config.yml");
 * String host = config.getString("database.host");
 * int port = config.getInt("database.port", 3306);
 * boolean enabled = config.getBoolean("feature.enabled", false);
 * List<String> users = config.getStringList("users");
 * Map<String,Object> section = config.getSection("settings");
 * </pre>
 *
 * @Author Fergs32
 */
@Getter @Setter
public final class YamlConfigFile {
    private final Map<String, Object> root;
    /**
     * Private constructor to enforce use of static loaders.
     * @param root The root map of the YAML file.
     */
    private YamlConfigFile(Map<String, Object> root) {
        this.root = root;
    }

    /**
     * Load from classpath resource (e.g. "/config.yml")
     * @param resourcePath The classpath resource path.
     * @return The loaded YamlConfigFile instance.
     * @throws RuntimeException if loading fails or resource not found.
     */
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
    /**
     * Load from filesystem path.
     * @param filePath The path to the YAML file.
     * @return The loaded YamlConfigFile instance.
     * @throws RuntimeException if loading fails or file not found.
     */
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

    /**
     * Get the raw Object at the given dot-path, or null if not found.
     * Example: "database.host" -> root.get("database").get("host")
     * Returns null if any part of the path is missing or not a Map.
     * @param path The dot-separated path.
     * @return The Object at the path, or null if not found.
     */
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

    /**
     * Get a String at the given path, or null if not found.
     * @param path The dot-separated path.
     * @return The String at the path, or null if not found.
     */
    public String getString(String path) {
        Object o = getRaw(path);
        return o != null ? o.toString() : null;
    }

    /**
     * Get an int at the given path, or default if not found or not a number.
     * @param path The dot-separated path.
     * @param def The default value if not found or invalid.
     * @return The int at the path, or def if not found/invalid.
     */
    public int getInt(String path, int def) {
        Object o = getRaw(path);
        if (o instanceof Number n)
            return n.intValue();

        try {
            assert o != null;
            return Integer.parseInt(o.toString());
        } catch (NumberFormatException e) {
            return def;
        }
    }
    /**
     * Get a boolean at the given path, or default if not found or not a boolean.
     * Accepts Boolean or String ("true"/"false").
     * @param path The dot-separated path.
     * @param def The default value if not found or invalid.
     * @return The boolean at the path, or def if not found/invalid.
     */
    public boolean getBoolean(String path, boolean def) {
        Object o = getRaw(path);
        if (o instanceof Boolean b) return b;
        if (o instanceof String s) return Boolean.parseBoolean(s);
        return def;
    }
    /**
     * Get a List<String> at the given path, or empty list if not found or not a list.
     * Converts each element to String via toString().
     * @param path The dot-separated path.
     * @return The List<String> at the path, or empty list if not found/invalid.
     */
    public List<String> getStringList(String path) {
        Object o = getRaw(path);
        if (o instanceof List<?> L) {
            List<String> out = new ArrayList<>();
            for (Object e : L) out.add(e == null ? "" : e.toString());
            return out;
        }
        return Collections.emptyList();
    }
    /**
     * Get a Map<String,Object> section at the given path, or empty map if not found or not a map.
     * The returned map is unmodifiable.
     * @param path The dot-separated path.
     * @return The Map<String,Object> at the path, or empty map if not found/invalid.
     */
    public Map<String,Object> getSection(String path) {
        Object o = getRaw(path);
        if (o instanceof Map<?,?> m) return Collections.unmodifiableMap((Map<String,Object>)m);
        return Collections.emptyMap();
    }
    /**
     * Get the entire root map of the YAML file.
     * The returned map is unmodifiable.
     * @return The root Map<String,Object>.
     */
    public Map<String,Object> asMap() {
        return Collections.unmodifiableMap(root);
    }
}