package io.github.sbracely.chinese.calendar.spi;

import java.io.IOException;
import java.util.Optional;

/**
 * Supplies Chinese calendar configuration properties.
 * 提供农历配置属性。
 * <p>
 * Providers are ordered by ascending {@link #getOrder()} and are checked before built-in
 * configuration sources. Equal provider orders are rejected.
 * 配置提供者按 {@link #getOrder()} 升序排列，并优先于内置配置来源检查；提供者顺序相同会报错。
 */
public interface ChineseCalendarConfigProvider {

    /**
     * Returns the provider order; lower values have higher precedence among providers.
     * 返回提供者顺序；数值越小，在提供者之间的优先级越高。
     *
     * @return provider order
     *         提供者顺序
     */
    int getOrder();

    /**
     * Finds configuration data, or returns empty if this provider does not apply. Once selected,
     * invalid data causes initialization to fail without fallback.
     * 查找配置数据；来源不适用时返回空。数据一旦选中，若内容无效，初始化会失败且不会回退。
     *
     * @return the configuration data, if available
     *         找到的配置数据
     * @throws IOException if loading or determining whether configuration is available fails
     *                     如果加载配置或判断提供者是否适用时失败
     */
    Optional<ChineseCalendarConfigData> findConfig() throws IOException;
}
