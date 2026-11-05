package io.github.sbracely.chinese.calendar.spi;

import java.util.Iterator;
import java.util.ServiceLoader;

/**
 * Loads the optional configuration provider using Java's service-provider mechanism.
 * 使用 Java 服务提供者机制加载可选配置提供者。
 */
public final class ConfigLoader {

    private static final ConfigProvider PROVIDER = loadProvider();

    private ConfigLoader() {
    }

    /**
     * Returns the path supplied by the registered provider, or {@code null} if none is registered.
     * 返回已注册提供者提供的路径；未注册提供者时返回 {@code null}。
     *
     * @return the external configuration path, or {@code null}
     *         外部配置文件路径或 {@code null}
     */
    public static String getChineseCalendarConfig() {
        return PROVIDER == null ? null : PROVIDER.getChineseCalendarConfig();
    }

    private static ConfigProvider loadProvider() {
        Iterator<ConfigProvider> providers = ServiceLoader.load(ConfigProvider.class).iterator();
        if (!providers.hasNext()) {
            return null;
        }
        ConfigProvider provider = providers.next();
        if (providers.hasNext()) {
            throw new IllegalStateException("Multiple ConfigProvider implementations found");
        }
        return provider;
    }
}
