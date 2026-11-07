package io.github.sbracely.chinese.calendar;

import io.github.sbracely.chinese.calendar.spi.ChineseCalendarConfigData;
import io.github.sbracely.chinese.calendar.spi.ChineseCalendarConfigProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Selects the highest-priority configuration data.
 * 选择优先级最高的配置数据。
 */
public final class ChineseCalendarConfigLoader {

    private static final String SYSTEM_PROPERTY = "chinese.calendar.config";
    private static final String ENVIRONMENT_VARIABLE = "CHINESE_CALENDAR_CONFIG";
    private static final String CONFIG_RESOURCE = "chinese-calendar-config.properties";
    private static final Path WORKING_DIRECTORY = Path.of(System.getProperty("user.dir"))
            .toAbsolutePath().normalize();
    private static volatile Path explicitPath;
    private static volatile ChineseCalendarConfigData explicitConfig;
    private static boolean loadingStarted;

    private ChineseCalendarConfigLoader() {
    }

    /**
     * Selects the explicit configuration path, ahead of all discovered sources.
     * Relative paths use the JVM working directory. Explicit configuration may be selected only
     * once, before any chronology instance starts loading data. Loading failures are cached;
     * they do not trigger fallback or allow reconfiguration.
     * 选择显式配置路径，其优先级高于所有自动发现的来源；相对路径基于 JVM 工作目录。
     * 显式配置只能选择一次，且须在任何历法实例开始加载数据前选择。
     * 加载失败会被缓存，不会回退到其他来源，也不能重新配置。
     *
     * @param path configuration path
     *             配置文件路径
     * @throws NullPointerException if the path is null
     *                              如果路径为 null
     * @throws IllegalArgumentException if the path is blank
     *                                  如果路径为空白
     * @throws IllegalStateException if configuration was already selected or data loading has started
     *                               如果已选择配置或已开始加载数据
     */
    public static synchronized void configure(Path path) {
        Objects.requireNonNull(path, "path");
        if (path.toString().isBlank()) {
            throw new IllegalArgumentException("External configuration path is blank");
        }
        ensureConfigurable();
        explicitPath = path.isAbsolute() ? path.normalize() : WORKING_DIRECTORY.resolve(path).normalize();
    }

    /**
     * Selects explicit configuration properties, ahead of all discovered sources.
     * Effective string properties, including defaults, are copied. Explicit configuration may be
     * selected only once, before any chronology instance starts loading data. Loading failures
     * are cached; they do not trigger fallback or allow reconfiguration.
     * 选择显式配置属性，其优先级高于所有自动发现的来源；复制包含默认值的有效字符串属性。
     * 显式配置只能选择一次，且须在任何历法实例开始加载数据前选择。
     * 加载失败会被缓存，不会回退到其他来源，也不能重新配置。
     *
     * @param properties configuration properties
     *                   配置属性
     * @throws NullPointerException if the properties are null
     *                              如果属性为 null
     * @throws IllegalStateException if configuration was already selected or data loading has started
     *                               如果已选择配置或已开始加载数据
     */
    public static synchronized void configure(Properties properties) {
        Objects.requireNonNull(properties, "properties");
        ensureConfigurable();
        explicitConfig = new ChineseCalendarConfigData("explicit properties", properties);
    }

    private static void ensureConfigurable() {
        if (loadingStarted) {
            throw new IllegalStateException("Chinese calendar data configuration is already fixed");
        }
        if (explicitPath != null || explicitConfig != null) {
            throw new IllegalStateException("Explicit configuration is already selected");
        }
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
    static synchronized ChineseCalendarConfigData load() throws IOException {
        loadingStarted = true;
        ChineseCalendarConfigData selectedConfig = explicitConfig;
        if (selectedConfig != null) {
            return selectedConfig;
        }
        Path selectedPath = explicitPath;
        if (selectedPath != null) {
            return ChineseCalendarConfigData.fromPath(selectedPath);
        }

        Map<Integer, ChineseCalendarConfigProvider> providers = new TreeMap<>();
        for (ChineseCalendarConfigProvider provider : ServiceLoader.load(ChineseCalendarConfigProvider.class)) {
            int order = provider.getOrder();
            if (providers.putIfAbsent(order, provider) != null) {
                throw new IllegalStateException("Multiple configuration providers have order "
                        + order);
            }
        }
        for (ChineseCalendarConfigProvider provider : providers.values()) {
            Optional<ChineseCalendarConfigData> configData = provider.findConfig();
            if (configData.isPresent()) {
                return configData.get();
            }
        }

        String systemPropertyPath = System.getProperty(SYSTEM_PROPERTY);
        if (systemPropertyPath != null) {
            return pathConfig(systemPropertyPath);
        }
        String environmentPath = System.getenv(ENVIRONMENT_VARIABLE);
        if (environmentPath != null) {
            return pathConfig(environmentPath);
        }
        Path workingDirectoryConfig = WORKING_DIRECTORY.resolve(CONFIG_RESOURCE);
        if (!Files.notExists(workingDirectoryConfig)) {
            return ChineseCalendarConfigData.fromPath(workingDirectoryConfig);
        }
        return classpathConfig();
    }

    private static Path resolvePath(String value) {
        Path path = Path.of(value);
        return path.isAbsolute() ? path.normalize() : WORKING_DIRECTORY.resolve(path).normalize();
    }

    private static ChineseCalendarConfigData pathConfig(String value) throws IOException {
        if (value.isBlank()) {
            throw new IllegalArgumentException("External configuration path is blank");
        }
        return ChineseCalendarConfigData.fromPath(resolvePath(value));
    }

    private static ChineseCalendarConfigData classpathConfig() throws IOException {
        Properties properties = new Properties();
        try (var input = ChineseCalendarConfigLoader.class.getClassLoader().getResourceAsStream(CONFIG_RESOURCE)) {
            if (input == null) {
                throw new IOException("Config resource not found: " + CONFIG_RESOURCE);
            }
            properties.load(input);
        }
        return new ChineseCalendarConfigData("classpath:" + CONFIG_RESOURCE, properties);
    }
}
