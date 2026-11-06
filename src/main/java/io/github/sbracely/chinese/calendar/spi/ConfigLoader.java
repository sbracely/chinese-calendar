package io.github.sbracely.chinese.calendar.spi;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.ServiceLoader;

/**
 * Selects the highest-priority configuration data.
 * 选择优先级最高的配置数据。
 */
public final class ConfigLoader {

    private static final String SYSTEM_PROPERTY = "chinese.calendar.config";
    private static final String ENVIRONMENT_VARIABLE = "CHINESE_CALENDAR_CONFIG";
    private static final String CONFIG_RESOURCE = "chinese-calendar-config.properties";
    private static final Path WORKING_DIRECTORY = Path.of(System.getProperty("user.dir"))
            .toAbsolutePath().normalize();
    private static volatile Path explicitPath;

    private ConfigLoader() {
    }

    /**
     * Selects the explicit configuration path, ahead of all discovered sources.
     * 选择显式配置路径，其优先级高于所有自动发现的来源。
     *
     * @param path configuration path
     *             配置文件路径
     */
    public static synchronized void configure(Path path) {
        Objects.requireNonNull(path, "path");
        if (path.toString().isBlank()) {
            throw new IllegalArgumentException("External configuration path is blank");
        }
        if (explicitPath != null) {
            throw new IllegalStateException("Explicit configuration path is already selected");
        }
        Path selectedPath = path.isAbsolute() ? path.normalize() : WORKING_DIRECTORY.resolve(path).normalize();
        explicitPath = selectedPath;
    }

    /**
     * Finds the first configuration data supplied by an applicable source.
     * 查找首个适用来源提供的配置数据。
     *
     * @return the selected configuration data
     *         选中的配置数据
     * @throws IOException if a source cannot be evaluated
     *                     如果配置来源无法评估
     */
    public static ConfigData load() throws IOException {
        Path selectedPath = explicitPath;
        if (selectedPath != null) {
            return ConfigData.fromPath(selectedPath);
        }
        List<ConfigSource> sources = new ArrayList<>();
        ServiceLoader.load(ConfigSource.class).forEach(sources::add);
        sources.add(new SystemPropertySource());
        sources.add(new EnvironmentSource());
        sources.add(new WorkingDirectorySource());
        sources.add(new ClasspathSource());
        sources.sort(Comparator.comparingInt(ConfigSource::getOrder));

        for (int i = 1; i < sources.size(); i++) {
            if (sources.get(i - 1).getOrder() == sources.get(i).getOrder()) {
                throw new IllegalStateException("Multiple configuration sources have order "
                        + sources.get(i).getOrder());
            }
        }
        for (ConfigSource source : sources) {
            Optional<ConfigData> configData = source.findConfig();
            if (configData.isPresent()) {
                return configData.get();
            }
        }
        throw new IllegalStateException("No Chinese calendar configuration source found");
    }

    private static Path resolvePath(String value) {
        Path path = Path.of(value);
        return path.isAbsolute() ? path.normalize() : WORKING_DIRECTORY.resolve(path).normalize();
    }

    private static ConfigData pathConfig(String value) throws IOException {
        if (value.isBlank()) {
            throw new IllegalArgumentException("External configuration path is blank");
        }
        return ConfigData.fromPath(resolvePath(value));
    }

    private static final class SystemPropertySource implements ConfigSource {

        @Override
        public int getOrder() {
            return 100;
        }

        @Override
        public Optional<ConfigData> findConfig() throws IOException {
            String value = System.getProperty(SYSTEM_PROPERTY);
            return value == null ? Optional.empty() : Optional.of(pathConfig(value));
        }
    }

    private static final class EnvironmentSource implements ConfigSource {

        @Override
        public int getOrder() {
            return 200;
        }

        @Override
        public Optional<ConfigData> findConfig() throws IOException {
            String value = System.getenv(ENVIRONMENT_VARIABLE);
            return value == null ? Optional.empty() : Optional.of(pathConfig(value));
        }
    }

    private static final class WorkingDirectorySource implements ConfigSource {

        private final Path path = WORKING_DIRECTORY.resolve(CONFIG_RESOURCE);

        @Override
        public int getOrder() {
            return 300;
        }

        @Override
        public Optional<ConfigData> findConfig() throws IOException {
            return Files.notExists(path) ? Optional.empty() : Optional.of(ConfigData.fromPath(path));
        }
    }

    private static final class ClasspathSource implements ConfigSource {

        @Override
        public int getOrder() {
            return 400;
        }

        @Override
        public Optional<ConfigData> findConfig() throws IOException {
            Properties properties = new Properties();
            try (var input = ConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_RESOURCE)) {
                if (input == null) {
                    throw new IOException("Config resource not found: " + CONFIG_RESOURCE);
                }
                properties.load(input);
            }
            return Optional.of(new ConfigData("classpath:" + CONFIG_RESOURCE, properties));
        }
    }
}
