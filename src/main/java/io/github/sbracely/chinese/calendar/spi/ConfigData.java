package io.github.sbracely.chinese.calendar.spi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/**
 * Configuration properties selected by a {@link ConfigSource}.
 * 由 {@link ConfigSource} 选中的配置属性。
 */
public final class ConfigData {

    private final String description;
    private final Properties properties;

    /**
     * Creates configuration data with a diagnostic description and properties.
     * 使用诊断描述和配置属性创建配置数据。
     *
     * @param description source description
     *                    来源描述
     * @param properties configuration properties
     *                    配置属性
     */
    public ConfigData(String description, Properties properties) {
        this.description = Objects.requireNonNull(description, "description");
        this.properties = new Properties();
        this.properties.putAll(Objects.requireNonNull(properties, "properties"));
    }

    /**
     * Loads properties from a file.
     * 从文件加载属性。
     *
     * @param path file path
     *             文件路径
     * @return the loaded configuration data
     *         加载的配置数据
     * @throws IOException if the file cannot be read
     *                     如果文件无法读取
     */
    public static ConfigData fromPath(Path path) throws IOException {
        Path absolutePath = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(absolutePath)) {
            properties.load(input);
        }
        return new ConfigData(absolutePath.toString(), properties);
    }

    /**
     * Returns the source description.
     * 返回来源描述。
     *
     * @return source description
     *         来源描述
     */
    public String description() {
        return description;
    }

    /**
     * Returns a copy of the configuration properties.
     * 返回配置属性的副本。
     *
     * @return a copy of the properties
     *         属性副本
     */
    public Properties properties() {
        Properties copy = new Properties();
        copy.putAll(properties);
        return copy;
    }
}
