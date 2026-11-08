package io.github.sbracely.chinese.calendar;

import java.io.Serializable;
import java.io.Serial;
import java.io.InvalidObjectException;
import java.io.ObjectStreamException;
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
 * All periods use {@link ChineseChronology#INSTANCE} as their chronology.
 * 所有期间均使用 {@link ChineseChronology#INSTANCE} 作为其历法。
 *
 * @param years the signed year component
 *              带符号的年分量
 * @param months the signed month component
 *               带符号的月分量
 * @param days the signed day component
 *             带符号的日分量
 */
public record ChinesePeriod(int years, int months, int days)
        implements ChronoPeriod, Serializable {

    /**
     * A period with all components set to zero.
     * 所有分量均为零的期间。
     */
    public static final ChinesePeriod ZERO = new ChinesePeriod(0, 0, 0);

    /**
     * The supported units in application order.
     * 按应用顺序排列的支持单位。
     */
    private static final List<TemporalUnit> UNITS = List.of(YEARS, MONTHS, DAYS);

    //-----------------------------------------------------------------------
    // Factory methods
    // 工厂方法
    //-----------------------------------------------------------------------

    /**
     * Creates a period without normalizing its independently signed components.
     * 创建各分量独立带符号的期间，不进行归一化。
     *
     * @param years the signed year component
     *              带符号的年分量
     * @param months the signed actual-month component, including leap months
     *               带符号的实际月分量，包含闰月
     * @param days the signed day component
     *             带符号的日分量
     * @return the Chinese period, or {@link #ZERO} if all components are zero
     *         农历期间，所有分量为零时返回 {@link #ZERO}
     */
    public static ChinesePeriod of(int years, int months, int days) {
        return (years | months | days) == 0 ? ZERO : new ChinesePeriod(years, months, days);
    }

    /**
     * Creates a period containing only years.
     * 创建仅包含年分量的期间。
     *
     * @param years the signed year component
     *              带符号的年分量
     * @return the Chinese period
     *         农历期间
     */
    public static ChinesePeriod ofYears(int years) {
        return of(years, 0, 0);
    }

    /**
     * Creates a period containing only actual months, including leap months.
     * 创建仅包含实际月分量的期间，闰月单独计数。
     *
     * @param months the signed month component
     *               带符号的月分量
     * @return the Chinese period
     *         农历期间
     */
    public static ChinesePeriod ofMonths(int months) {
        return of(0, months, 0);
    }

    /**
     * Creates a day-based period by converting each week to seven days.
     * 将每周换算为七天，创建以日为单位的期间。
     *
     * @param weeks the signed number of weeks
     *              带符号的周数
     * @return the Chinese period
     *         农历期间
     * @throws ArithmeticException if the day component overflows
     *                             如果日分量溢出
     */
    public static ChinesePeriod ofWeeks(int weeks) {
        return ofDays(Math.multiplyExact(weeks, 7));
    }

    /**
     * Creates a period containing only days.
     * 创建仅包含日分量的期间。
     *
     * @param days the signed day component
     *             带符号的日分量
     * @return the Chinese period
     *         农历期间
     */
    public static ChinesePeriod ofDays(int days) {
        return of(0, 0, days);
    }

    /**
     * Calculates a period between two Chinese dates using {@link ChineseDate#until}.
     * 使用 {@link ChineseDate#until} 计算两个农历日期之间的期间。
     *
     * @param startDateInclusive the inclusive start date
     *                           包含在内的起始日期
     * @param endDateExclusive the exclusive end date
     *                         不包含在内的结束日期
     * @return the Chinese period
     *         农历期间
     * @throws NullPointerException if either date is null
     *                              如果任一日期为空
     */
    public static ChinesePeriod between(ChineseDate startDateInclusive, ChineseDate endDateExclusive) {
        Objects.requireNonNull(startDateInclusive, "startDateInclusive");
        Objects.requireNonNull(endDateExclusive, "endDateExclusive");
        return startDateInclusive.until(endDateExclusive);
    }

    //-----------------------------------------------------------------------
    // Component operations
    // 分量操作
    //-----------------------------------------------------------------------

    /**
     * Replaces the year component, leaving months and days unchanged.
     * 替换年分量，保持月和日分量不变。
     *
     * @param years the signed replacement
     *              带符号的新分量
     * @return the resulting period, or this period if unchanged
     *         结果期间，未改变时返回此期间
     */
    public ChinesePeriod withYears(int years) {
        return years == this.years ? this : of(years, months, days);
    }

    /**
     * Replaces the actual-month component, leaving years and days unchanged.
     * 替换实际月分量，保持年和日分量不变。
     *
     * @param months the signed replacement
     *               带符号的新分量
     * @return the resulting period, or this period if unchanged
     *         结果期间，未改变时返回此期间
     */
    public ChinesePeriod withMonths(int months) {
        return months == this.months ? this : of(years, months, days);
    }

    /**
     * Replaces the day component, leaving years and months unchanged.
     * 替换日分量，保持年和月分量不变。
     *
     * @param days the signed replacement
     *             带符号的新分量
     * @return the resulting period, or this period if unchanged
     *         结果期间，未改变时返回此期间
     */
    public ChinesePeriod withDays(int days) {
        return days == this.days ? this : of(years, months, days);
    }

    /**
     * Adds to the year component without normalizing other components.
     * 增加年分量，不对其他分量进行归一化。
     *
     * @param yearsToAdd the signed amount to add
     *                   带符号的增量
     * @return the resulting period
     *         结果期间
     * @throws ArithmeticException if the year component overflows
     *                             如果年分量溢出
     */
    public ChinesePeriod plusYears(long yearsToAdd) {
        return withYears(Math.toIntExact(Math.addExact(years, yearsToAdd)));
    }

    /**
     * Adds to the actual-month component without converting months to years.
     * 增加实际月分量，不将月换算为年。
     *
     * @param monthsToAdd the signed amount to add
     *                    带符号的增量
     * @return the resulting period
     *         结果期间
     * @throws ArithmeticException if the month component overflows
     *                             如果月分量溢出
     */
    public ChinesePeriod plusMonths(long monthsToAdd) {
        return withMonths(Math.toIntExact(Math.addExact(months, monthsToAdd)));
    }

    /**
     * Adds to the day component without changing years or months.
     * 增加日分量，不改变年或月分量。
     *
     * @param daysToAdd the signed amount to add
     *                  带符号的增量
     * @return the resulting period
     *         结果期间
     * @throws ArithmeticException if the day component overflows
     *                             如果日分量溢出
     */
    public ChinesePeriod plusDays(long daysToAdd) {
        return withDays(Math.toIntExact(Math.addExact(days, daysToAdd)));
    }

    /**
     * Subtracts from the year component without normalizing other components.
     * 减少年分量，不对其他分量进行归一化。
     *
     * @param yearsToSubtract the signed amount to subtract
     *                        带符号的减量
     * @return the resulting period
     *         结果期间
     * @throws ArithmeticException if the year component overflows
     *                             如果年分量溢出
     */
    public ChinesePeriod minusYears(long yearsToSubtract) {
        return withYears(Math.toIntExact(Math.subtractExact((long) years, yearsToSubtract)));
    }

    /**
     * Subtracts from the actual-month component without converting months to years.
     * 减少实际月分量，不将月换算为年。
     *
     * @param monthsToSubtract the signed amount to subtract
     *                         带符号的减量
     * @return the resulting period
     *         结果期间
     * @throws ArithmeticException if the month component overflows
     *                             如果月分量溢出
     */
    public ChinesePeriod minusMonths(long monthsToSubtract) {
        return withMonths(Math.toIntExact(Math.subtractExact((long) months, monthsToSubtract)));
    }

    /**
     * Subtracts from the day component without changing years or months.
     * 减少日分量，不改变年或月分量。
     *
     * @param daysToSubtract the signed amount to subtract
     *                      带符号的减量
     * @return the resulting period
     *         结果期间
     * @throws ArithmeticException if the day component overflows
     *                             如果日分量溢出
     */
    public ChinesePeriod minusDays(long daysToSubtract) {
        return withDays(Math.toIntExact(Math.subtractExact((long) days, daysToSubtract)));
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
     * @throws UnsupportedTemporalTypeException if the unit is null or not years, months, or days
     *                                         如果单位为空或不是年、月或日
     */
    @Override
    public long get(TemporalUnit unit) {
        return switch (unit) {
            case ChronoUnit.YEARS -> years;
            case ChronoUnit.MONTHS -> months;
            case ChronoUnit.DAYS -> days;
            case null, default -> throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
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
     * Gets the shared Chinese chronology singleton.
     * 获取共享的农历历法单例。
     *
     * @return the Chinese chronology
     *         农历历法
     */
    @Override
    public ChineseChronology getChronology() {
        return ChineseChronology.INSTANCE;
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
        return new ChinesePeriod(
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
        return new ChinesePeriod(
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
        if (isZero() || scalar == 1) {
            return this;
        }
        return new ChinesePeriod(Math.multiplyExact(years, scalar),
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
        StringBuilder result = new StringBuilder(getChronology().getId()).append(" P");
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
        if (!getChronology().equals(period.getChronology())) {
            throw new DateTimeException("Chronology mismatch, expected: " + getChronology().getId()
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
        if (actual != null && !getChronology().equals(actual)) {
            throw new DateTimeException("Chronology mismatch, expected: " + getChronology().getId()
                    + ", actual: " + actual.getId());
        }
    }

    /**
     * Replaces this period with a proxy retaining the chronology's data fingerprint check.
     * 将此期间替换为代理，保留历法数据指纹校验。
     *
     * @return the serialization proxy
     *         序列化代理
     * @throws DateTimeException if the calendar data cannot be initialized
     *                           如果历法数据无法初始化
     */
    @Serial
    private Object writeReplace() {
        return new Ser(getChronology(), years, months, days);
    }

    /**
     * Serialization proxy preserving components and the chronology's fingerprint validation.
     * 保存分量并保留历法指纹校验的序列化代理。
     *
     * @param chronology the chronology whose proxy verifies the calendar data
     *                   其代理负责校验历法数据的历法
     * @param years the signed year component
     *              带符号的年分量
     * @param months the signed month component
     *               带符号的月分量
     * @param days the signed day component
     *             带符号的日分量
     */
    private record Ser(ChineseChronology chronology, int years, int months, int days) implements Serializable {
        /**
         * Restores the period after the chronology proxy has validated the data fingerprint.
         * 在历法代理校验数据指纹后恢复期间。
         *
         * @return the restored Chinese period
         *         恢复的农历期间
         * @throws ObjectStreamException if the chronology is missing
         *                               如果历法缺失
         */
        @Serial
        private Object readResolve() throws ObjectStreamException {
            if (chronology == null) {
                throw new InvalidObjectException("Missing Chinese chronology");
            }
            return new ChinesePeriod(years, months, days);
        }
    }
}
