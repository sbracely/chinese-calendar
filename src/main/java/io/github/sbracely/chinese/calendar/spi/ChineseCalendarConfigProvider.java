package io.github.sbracely.chinese.calendar.spi;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/**
 * Supplies a Chinese calendar configuration path or properties.
 * 提供农历配置文件路径或属性。
 * <p>
 * Providers are ordered by ascending {@link #getOrder()}, after explicit configuration
 * but before system-property, environment-variable, working-directory, and classpath sources.
 * Equal provider orders are rejected.
 * 配置提供者按 {@link #getOrder()} 升序排列，优先级低于显式配置，
 * 高于系统属性、环境变量、工作目录和类路径来源；提供者顺序相同会报错。
 */
public interface ChineseCalendarConfigProvider {

    /**
     * Returns the provider order; lower values have higher precedence among providers.
     * 返回提供者顺序；数值越小，在提供者之间的优先级越高。
     *
     * @return provider order
     *         提供者顺序
     */
    default int getOrder() {
        return 0;
    }

    /**
     * Provides a configuration path, or empty if none is available.
     * Relative paths use the JVM working directory. Override this method or {@link #getProperties()},
     * not both with present values. Selected sources are loaded without fallback on failure.
     * 提供配置文件路径，没有可用路径时返回空；相对路径基于 JVM 工作目录。
     * 重写此方法或 {@link #getProperties()}，不能同时返回非空来源；选中来源加载失败时不回退。
     *
     * @return the configuration path, if available
     *         可用的配置文件路径
     * @throws IOException if determining the source fails
     *                     如果确定配置来源时失败
     */
    default Optional<Path> getPath() throws IOException {
        return Optional.empty();
    }

    /**
     * Provides configuration properties, or empty if none are available.
     * Effective string properties, including defaults, are copied by the loader.
     * Returning both properties and a path is rejected; returning neither skips this provider.
     * 提供配置属性，没有可用属性时返回空；加载器复制包含默认值的有效字符串属性。
     * 同时提供属性和路径会报错，两者均为空时跳过此提供者。
     *
     * @return the configuration properties, if available
     *         可用的配置属性
     * @throws IOException if determining the source fails
     *                     如果确定配置来源时失败
     */
    default Optional<Properties> getProperties() throws IOException {
        return Optional.empty();
    }
}
