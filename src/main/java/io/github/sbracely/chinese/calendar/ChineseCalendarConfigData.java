package io.github.sbracely.chinese.calendar;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

/**
 * A configuration source description and a defensive snapshot of its string properties.
 * Stores effective string properties, including defaults; returned properties are also copied.
 * 配置来源描述及其字符串属性的防御性快照。
 * 保存包含默认值的有效字符串属性，返回的属性也会复制。
 */
final class ChineseCalendarConfigData {

    /**
     * The non-null source description used in initialization diagnostics.
     * 初始化诊断使用的非空来源描述。
     */
    private final String description;
    /**
     * The private snapshot of effective string properties, with defaults flattened.
     * 有效字符串属性的私有快照，默认属性已合并为直接属性。
     */
    private final Properties properties;

    /**
     * Creates configuration data with a diagnostic description and properties.
     * 使用诊断描述和配置属性创建配置数据。
     *
     * @param description source description
     *                    来源描述
     * @param properties configuration properties
     *                    配置属性
     * @throws NullPointerException if the description or properties are {@code null}
     *                              如果描述或属性为 {@code null}
     */
    ChineseCalendarConfigData(String description, Properties properties) {
        this.description = Objects.requireNonNull(description, "description");
        this.properties = new Properties();
        Objects.requireNonNull(properties, "properties");
        for (String name : properties.stringPropertyNames()) {
            this.properties.setProperty(name, properties.getProperty(name));
        }
    }

    /**
     * Loads properties from a file using {@link Properties#load(InputStream)}.
     * Relative paths use the current JVM working directory.
     * 使用 {@link Properties#load(InputStream)} 从文件加载属性。
     * 相对路径基于当前 JVM 工作目录。
     *
     * @param path file path
     *             文件路径
     * @return the loaded configuration data
     *         加载的配置数据
     * @throws IOException if the file cannot be read
     *                     如果文件无法读取
     * @throws NullPointerException if the path is {@code null}
     *                              如果路径为 {@code null}
     * @throws IllegalArgumentException if the file contains a malformed Unicode escape
     *                                  如果文件包含格式错误的 Unicode 转义
     */
    static ChineseCalendarConfigData fromPath(Path path) throws IOException {
        Path absolutePath = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(absolutePath)) {
            properties.load(input);
        }
        return new ChineseCalendarConfigData(absolutePath.toString(), properties);
    }

    /**
     * Returns the source description.
     * 返回来源描述。
     *
     * @return source description
     *         来源描述
     */
    String description() {
        return description;
    }

    /**
     * Returns a copy of the configuration properties.
     * 返回配置属性的副本。
     *
     * @return a copy of the properties
     *         属性副本
     */
    Properties properties() {
        Properties copy = new Properties();
        copy.putAll(properties);
        return copy;
    }
}
