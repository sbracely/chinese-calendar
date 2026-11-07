package io.github.sbracely.chinese.calendar;

import java.io.Serializable;
import java.time.DateTimeException;
import java.time.chrono.ChronoPeriod;
import java.time.chrono.Chronology;
import java.time.temporal.*;
import java.util.List;
import java.util.Objects;

import static java.time.temporal.ChronoUnit.*;

/**
 * An immutable Chinese-calendar period with independently signed year, month, and day components.
 * 由可独立带符号的年、月、日分量组成的不可变农历期间。
 * <p>
 * Components are applied in year-month-day order. Leap months count as separate months;
 * changing years may fall back to a regular month and clip the day.
 * 分量按年、月、日的顺序应用；闰月单独计数，调整年份可能回退到普通月并截取有效日数。
 *
 * @param chronology the non-null Chinese chronology
 *                   不为 null 的农历历法
 * @param years the signed year component
 *              带符号的年分量
 * @param months the signed month component
 *               带符号的月分量
 * @param days the signed day component
 *             带符号的日分量
 */
public record ChinesePeriod(ChineseChronology chronology, int years, int months, int days)
        implements ChronoPeriod, Serializable {

    /**
     * The supported units in application order.
     * 按应用顺序排列的支持单位。
     */
    private static final List<TemporalUnit> UNITS = List.of(YEARS, MONTHS, DAYS);

    /**
     * Validates the chronology, leaving the signed components unnormalized.
     * 校验历法，保留未归一化的带符号分量。
     *
     * @param chronology the non-null Chinese chronology
     *                   不为 null 的农历历法
     * @param years the signed year component
     *              带符号的年分量
     * @param months the signed month component
     *               带符号的月分量
     * @param days the signed day component
     *             带符号的日分量
     * @throws NullPointerException if the chronology is {@code null}
     *                              如果历法为 {@code null}
     */
    public ChinesePeriod {
        Objects.requireNonNull(chronology, "chronology");
    }

    //-----------------------------------------------------------------------
    // Override methods from ChronoPeriod
    // 重写 ChronoPeriod 接口中的方法
    //-----------------------------------------------------------------------

    /**
     * Gets the component for a supported unit.
     * 获取支持单位对应的分量。
     *
     * @param unit the unit to query
     *             要查询的单位
     * @return the signed component
     *         带符号的分量
     * @throws NullPointerException if the unit is {@code null}
     *                              如果单位为 {@code null}
     * @throws UnsupportedTemporalTypeException if the unit is not years, months, or days
     *                                         如果单位不是年、月或日
     */
    @Override
    public long get(TemporalUnit unit) {
        Objects.requireNonNull(unit, "unit");
        return switch (unit) {
            case ChronoUnit.YEARS -> years;
            case ChronoUnit.MONTHS -> months;
            case ChronoUnit.DAYS -> days;
            default -> throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
        };
    }

    /**
     * Gets the immutable list of supported units.
     * 获取支持单位的不可变列表。
     *
     * @return years, months, and days in that order
     *         按年、月、日顺序排列的单位
     */
    @Override
    public List<TemporalUnit> getUnits() {
        return UNITS;
    }

    /**
     * Gets the chronology of this period.
     * 获取此期间所属的历法。
     *
     * @return the Chinese chronology
     *         农历历法
     */
    @Override
    public ChineseChronology getChronology() {
        return chronology;
    }

    /**
     * Adds the components of a period with the same chronology.
     * 增加相同历法期间的各分量。
     *
     * @param amount the period to add
     *               要增加的期间
     * @return the resulting Chinese period
     *         结果农历期间
     * @throws NullPointerException if the amount is {@code null}
     *                              如果时间量为 {@code null}
     * @throws DateTimeException if the amount is not a period of the same chronology
     *                           如果时间量不是相同历法的期间
     * @throws ArithmeticException if a component overflows
     *                             如果分量溢出
     */
    @Override
    public ChinesePeriod plus(TemporalAmount amount) {
        ChronoPeriod other = validateAmount(amount);
        return new ChinesePeriod(chronology,
                Math.addExact(years, Math.toIntExact(other.get(YEARS))),
                Math.addExact(months, Math.toIntExact(other.get(MONTHS))),
                Math.addExact(days, Math.toIntExact(other.get(DAYS))));
    }

    /**
     * Subtracts the components of a period with the same chronology.
     * 减去相同历法期间的各分量。
     *
     * @param amount the period to subtract
     *               要减去的期间
     * @return the resulting Chinese period
     *         结果农历期间
     * @throws NullPointerException if the amount is {@code null}
     *                              如果时间量为 {@code null}
     * @throws DateTimeException if the amount is not a period of the same chronology
     *                           如果时间量不是相同历法的期间
     * @throws ArithmeticException if a component overflows
     *                             如果分量溢出
     */
    @Override
    public ChinesePeriod minus(TemporalAmount amount) {
        ChronoPeriod other = validateAmount(amount);
        return new ChinesePeriod(chronology,
                Math.subtractExact(years, Math.toIntExact(other.get(YEARS))),
                Math.subtractExact(months, Math.toIntExact(other.get(MONTHS))),
                Math.subtractExact(days, Math.toIntExact(other.get(DAYS))));
    }

    /**
     * Multiplies every component by the specified scalar.
     * 将每个分量乘以指定倍数。
     *
     * @param scalar the multiplier
     *               倍数
     * @return the multiplied Chinese period
     *         相乘后的农历期间
     * @throws ArithmeticException if a component overflows
     *                             如果分量溢出
     */
    @Override
    public ChinesePeriod multipliedBy(int scalar) {
        return new ChinesePeriod(chronology, Math.multiplyExact(years, scalar),
                Math.multiplyExact(months, scalar), Math.multiplyExact(days, scalar));
    }

    /**
     * Negates every component of this period.
     * 将此期间的每个分量取反。
     *
     * @return the negated Chinese period
     *         取反后的农历期间
     * @throws ArithmeticException if a component cannot be negated without overflow
     *                             如果分量取反时发生溢出
     */
    @Override
    public ChinesePeriod negated() {
        return multipliedBy(-1);
    }

    /**
     * Returns this period without converting months into years.
     * 返回此期间，不将月份换算为年份。
     * <p>
     * A Chinese year has 12 or 13 actual months, so normalization requires a reference date.
     * 农历一年有 12 或 13 个实际月份，归一化需要参考日期。
     *
     * @return this period
     *         此期间
     */
    @Override
    public ChinesePeriod normalized() {
        return this;
    }

    /**
     * Adds years, then months, then days to a temporal object.
     * 依次向时态对象增加年、月、日。
     *
     * @param temporal the target temporal object
     *                 目标时态对象
     * @return the adjusted temporal object
     *         调整后的时态对象
     * @throws NullPointerException if the target is {@code null}
     *                              如果目标为 {@code null}
     * @throws DateTimeException if chronologies differ or the addition cannot be made
     *                           如果历法不一致或无法完成加法
     * @throws ArithmeticException if numeric overflow occurs
     *                             如果发生数值溢出
     */
    @Override
    public Temporal addTo(Temporal temporal) {
        validateChronology(temporal);
        if (years != 0) {
            temporal = temporal.plus(years, YEARS);
        }
        if (months != 0) {
            temporal = temporal.plus(months, MONTHS);
        }
        if (days != 0) {
            temporal = temporal.plus(days, DAYS);
        }
        return temporal;
    }

    /**
     * Subtracts years, then months, then days from a temporal object.
     * 依次从时态对象减去年、月、日。
     *
     * @param temporal the target temporal object
     *                 目标时态对象
     * @return the adjusted temporal object
     *         调整后的时态对象
     * @throws NullPointerException if the target is {@code null}
     *                              如果目标为 {@code null}
     * @throws DateTimeException if chronologies differ or the subtraction cannot be made
     *                           如果历法不一致或无法完成减法
     * @throws ArithmeticException if numeric overflow occurs
     *                             如果发生数值溢出
     */
    @Override
    public Temporal subtractFrom(Temporal temporal) {
        validateChronology(temporal);
        if (years != 0) {
            temporal = temporal.minus(years, YEARS);
        }
        if (months != 0) {
            temporal = temporal.minus(months, MONTHS);
        }
        if (days != 0) {
            temporal = temporal.minus(days, DAYS);
        }
        return temporal;
    }

    /**
     * Returns the chronology ID followed by the signed period components.
     * 返回历法 ID 及带符号的期间分量。
     *
     * @return the period text
     *         期间文本
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(chronology.getId()).append(" P");
        if (isZero()) {
            return result.append("0D").toString();
        }
        if (years != 0) {
            result.append(years).append('Y');
        }
        if (months != 0) {
            result.append(months).append('M');
        }
        if (days != 0) {
            result.append(days).append('D');
        }
        return result.toString();
    }

    //-----------------------------------------------------------------------
    // Validation helpers
    // 校验辅助方法
    //-----------------------------------------------------------------------

    /**
     * Validates that an amount is a period of the same chronology.
     * 校验时间量是否为相同历法的期间。
     *
     * @param amount the amount to validate
     *               要校验的时间量
     * @return the validated period
     *         校验后的期间
     * @throws NullPointerException if the amount is {@code null}
     *                              如果时间量为 {@code null}
     * @throws DateTimeException if the amount has an incompatible type or chronology
     *                           如果时间量的类型或历法不兼容
     */
    private ChronoPeriod validateAmount(TemporalAmount amount) {
        Objects.requireNonNull(amount, "amount");
        if (!(amount instanceof ChronoPeriod period)) {
            throw new DateTimeException("Amount must be a ChronoPeriod: " + amount);
        }
        if (!chronology.equals(period.getChronology())) {
            throw new DateTimeException("Chronology mismatch, expected: " + chronology.getId()
                    + ", actual: " + period.getChronology().getId());
        }
        return period;
    }

    /**
     * Checks the target's chronology when one is available.
     * 当目标提供历法时，校验其历法是否匹配。
     *
     * @param temporal the target temporal object
     *                 目标时态对象
     * @throws NullPointerException if the target is {@code null}
     *                              如果目标为 {@code null}
     * @throws DateTimeException if the target has a different chronology
     *                           如果目标使用不同的历法
     */
    private void validateChronology(Temporal temporal) {
        Objects.requireNonNull(temporal, "temporal");
        Chronology actual = temporal.query(TemporalQueries.chronology());
        if (actual != null && !chronology.equals(actual)) {
            throw new DateTimeException("Chronology mismatch, expected: " + chronology.getId()
                    + ", actual: " + actual.getId());
        }
    }

}
