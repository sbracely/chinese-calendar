package io.github.sbracely.chinese.calendar.spi;

/**
 * Provides the path to an external Chinese calendar configuration file.
 * 提供外部农历配置文件的路径。
 * <p>
 * Implementations are discovered and instantiated by {@link java.util.ServiceLoader};
 * they must be constructible without Spring dependency injection.
 * 实现由 {@link java.util.ServiceLoader} 发现并实例化，不能依赖 Spring 构造器注入。
 */
@FunctionalInterface
public interface ConfigProvider {

    /**
     * Returns the configuration path, or {@code null} if this provider has no path.
     * 返回配置文件路径；提供者未配置路径时返回 {@code null}。
     *
     * @return the external configuration path, or {@code null}
     *         外部配置文件路径或 {@code null}
     */
    String getChineseCalendarConfig();
}
