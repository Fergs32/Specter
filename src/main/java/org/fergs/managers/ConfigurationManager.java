package org.fergs.managers;

import lombok.Getter;
import lombok.Setter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * ConfigurationManager loads one or more YAML files into an in‑memory map,
 * supports reloading, and provides typed getters.
 */
@Getter @Setter
public class ConfigurationManager {
    public final Yaml yaml;
    public final Map<String, Object> configMap = Collections.synchronizedMap(new HashMap<>());

    public ConfigurationManager() {
        LoaderOptions yamlLoaderOptions = new LoaderOptions();
        yamlLoaderOptions.setMaxAliasesForCollections(Integer.MAX_VALUE);
        yamlLoaderOptions.setCodePointLimit(Integer.MAX_VALUE);

        this.yaml = new Yaml(new SafeConstructor(yamlLoaderOptions));
    }

    /**
     * Load (or reload) a YAML file from the classpath.
     * @param resourceName e.g. "/config.yml"
     * @throws RuntimeException if loading/parsing fails
     */
    public void loadFromClasspath(String resourceName) {
        InputStream in = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(resourceName);
        if (in == null) {
            throw new RuntimeException("Resource not found on classpath: " + resourceName);
        }
        try (in) {
            Object loaded = yaml.load(in);
            if (!(loaded instanceof Map)) {
                throw new RuntimeException("Expected a YAML mapping at root of " + resourceName);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) loaded;
            synchronized (configMap) {
                configMap.clear();
                configMap.putAll(data);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load YAML from classpath: " + resourceName, e);
        }
    }
    /**
     * Load (or reload) a YAML file from the filesystem.
     * @param filePath path to the YAML file
     * @throws RuntimeException if loading/parsing fails
     */
    public void loadFromFile(Path filePath) {
        Objects.requireNonNull(filePath, "filePath cannot be null");
        try (InputStream in = Files.newInputStream(filePath, StandardOpenOption.READ)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> data = yaml.loadAs(in, Map.class);
            synchronized (configMap) {
                configMap.clear();
                if (data != null) configMap.putAll(data);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load YAML from file: " + filePath, e);
        }
    }
    /**
     * Load (or reload) a text file as YAML.
     * This is useful for simple YAML files that do not require complex parsing.
     * @param filePath path to the text file
     * @throws RuntimeException if loading/parsing fails
     */
    public void loadTextFile(Path filePath) {
        Objects.requireNonNull(filePath, "filePath cannot be null");
        try {
            String content = Files.readString(filePath);
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            Yaml yaml = new Yaml(options);
            @SuppressWarnings("unchecked")
            Map<String, Object> data = yaml.load(content);
            synchronized (configMap) {
                configMap.clear();
                if (data != null) configMap.putAll(data);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load text file: " + filePath, e);
        }
    }

    /**
     * Retrieve a raw Object for the given key.
     * @param key top‑level YAML key
     * @return value or null if missing
     */
    public Object get(String key) {
        return configMap.get(key);
    }

    /**
     * Retrieve a value cast to the desired type.
     * @param key top‑level YAML key
     * @param type the expected Class
     * @param <T> generic type
     * @return value cast to T, or null if missing
     * @throws ClassCastException if the underlying type mismatches
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        Object val = configMap.get(key);
        return val == null ? null : (T) val;
    }

    public List<String> getStringList(String key) {
        Object o = configMap.get(key);
        if (o instanceof List<?>) {
            @SuppressWarnings("unchecked")
            List<String> lst = (List<String>) o;
            return lst;
        }
        return Collections.emptyList();
    }

    /**
     * Retrieve the entire configuration map (read‑only copy).
     */
    public Map<String, Object> asMap() {
        synchronized (configMap) {
            return Collections.unmodifiableMap(new HashMap<>(configMap));
        }
    }
}
