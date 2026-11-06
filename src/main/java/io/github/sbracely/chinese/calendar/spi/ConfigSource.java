package io.github.sbracely.chinese.calendar.spi;

import java.io.IOException;
import java.util.Optional;

/**
 * Supplies Chinese calendar configuration properties.
 * 提供农历配置属性。
 * <p>
 * Sources are ordered by ascending {@link #getOrder()}; equal orders are rejected.
 * The built-in sources use orders 100 through 400. Choose a unique order to position a custom
 * source before, between, or after them.
 * 配置来源按 {@link #getOrder()} 升序排列；相同顺序会报错。
 * 内置来源使用 100 到 400；自定义来源可通过唯一顺序值排在内置来源之前、之间或之后。
 */
public interface ConfigSource {

    /**
     * Returns the source order; lower values have higher precedence.
     * 返回来源顺序；数值越小，优先级越高。
     *
     * @return source order
     *         来源顺序
     */
    int getOrder();

    /**
     * Finds configuration data, or returns empty if this source does not apply. Once selected,
     * invalid data causes initialization to fail without fallback.
     * 查找配置数据；来源不适用时返回空。数据一旦选中，若内容无效，初始化会失败且不会回退。
     *
     * @return the configuration data, if available
     *         找到的配置数据
     * @throws IOException if loading or determining whether configuration is available fails
     *                     如果加载配置或判断配置是否可用时失败
     */
    Optional<ConfigData> findConfig() throws IOException;
}
