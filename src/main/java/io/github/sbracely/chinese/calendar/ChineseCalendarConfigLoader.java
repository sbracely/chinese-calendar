package io.github.sbracely.chinese.calendar;

import io.github.sbracely.chinese.calendar.spi.ChineseCalendarConfigProvider;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Selects configuration from explicit settings, ordered SPI providers, a system property,
 * an environment variable, a working-directory file, then the built-in resource.
 * A selected source that fails does not trigger fallback to lower-priority sources.
 * 依次从显式配置、有序 SPI 提供者、系统属性、环境变量、工作目录文件和内置资源选择配置。
 * 选中的来源加载失败时，不会回退到优先级更低的来源。
 */
public final class ChineseCalendarConfigLoader {

    /**
     * The system property specifying an external configuration path.
     * 指定外部配置路径的系统属性名。
     */
    private static final String SYSTEM_PROPERTY = "chinese.calendar.config";
    /**
     * The environment variable specifying an external configuration path.
     * 指定外部配置路径的环境变量名。
     */
    private static final String ENVIRONMENT_VARIABLE = "CHINESE_CALENDAR_CONFIG";
    /**
     * The configuration filename used in the working directory and on the classpath.
     * 工作目录和类路径中使用的配置文件名。
     */
    private static final String CONFIG_RESOURCE = "chinese-calendar-config.properties";
    /**
     * The normalized JVM working directory captured when this class is initialized.
     * 此类初始化时记录的规范化 JVM 工作目录。
     */
    private static final Path WORKING_DIRECTORY = Path.of(System.getProperty("user.dir"))
            .toAbsolutePath().normalize();
    /**
     * The explicitly selected absolute configuration path, or {@code null} if unset.
     * 显式选择的绝对配置路径，未设置时为 {@code null}。
     */
    private static volatile Path explicitPath;
    /**
     * The copied explicit configuration properties, or {@code null} if unset.
     * 已复制的显式配置属性，未设置时为 {@code null}。
     */
    private static volatile ChineseCalendarConfigData explicitConfig;
    /**
     * Whether source selection has started, preventing further explicit configuration.
     * 是否已开始选择来源；开始后禁止再设置显式配置。
     */
    private static boolean loadingStarted;

    /**
     * Prevents instantiation of this static configuration utility.
     * 禁止实例化此静态配置工具类。
     */
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

    /**
     * Checks that no explicit source has been selected and loading has not started.
     * 检查是否尚未选择显式来源且尚未开始加载。
     *
     * @throws IllegalStateException if configuration is already selected or fixed
     *                               如果已选择配置或配置已固定
     */
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
     * @throws IllegalStateException if SPI providers have duplicate orders or a provider supplies both sources
     *                               如果 SPI 提供者顺序重复或某个提供者同时提供两种来源
     * @throws IllegalArgumentException if a selected external path is blank or invalid
     *                                  如果选中的外部路径为空白或无效
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
            Optional<Path> path = Objects.requireNonNull(provider.getPath(), "provider path");
            Optional<Properties> properties = Objects.requireNonNull(provider.getProperties(), "provider properties");
            if (path.isPresent() && properties.isPresent()) {
                throw new IllegalStateException("Configuration provider supplies both path and properties: "
                        + provider.getClass().getName());
            }
            if (path.isPresent()) {
                return pathConfig(path.get().toString());
            }
            if (properties.isPresent()) {
                return new ChineseCalendarConfigData("SPI:" + provider.getClass().getName(), properties.get());
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

    /**
     * Normalizes a path, resolving relative paths against the captured JVM working directory.
     * 规范化路径，并以记录的 JVM 工作目录解析相对路径。
     *
     * @param value the path text
     *              路径文本
     * @return the normalized absolute path
     *         规范化的绝对路径
     * @throws java.nio.file.InvalidPathException if the path text is invalid
     *                                           如果路径文本无效
     */
    private static Path resolvePath(String value) {
        Path path = Path.of(value);
        return path.isAbsolute() ? path.normalize() : WORKING_DIRECTORY.resolve(path).normalize();
    }

    /**
     * Loads configuration from a nonblank external path.
     * 从非空白的外部路径加载配置。
     *
     * @param value the external path text
     *              外部路径文本
     * @return the loaded configuration data
     *         加载的配置数据
     * @throws IllegalArgumentException if the path is blank or invalid
     *                                  如果路径为空白或无效
     * @throws IOException if the file cannot be read
     *                     如果文件无法读取
     */
    private static ChineseCalendarConfigData pathConfig(String value) throws IOException {
        if (value.isBlank()) {
            throw new IllegalArgumentException("External configuration path is blank");
        }
        return ChineseCalendarConfigData.fromPath(resolvePath(value));
    }

    /**
     * Loads the built-in properties resource using this class's class loader.
     * 使用此类的类加载器加载内置属性资源。
     *
     * @return the loaded built-in configuration
     *         加载的内置配置
     * @throws IOException if the resource is missing or cannot be read
     *                     如果资源不存在或无法读取
     */
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
