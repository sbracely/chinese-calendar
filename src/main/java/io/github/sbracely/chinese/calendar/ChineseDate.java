package io.github.sbracely.chinese.calendar;

import java.io.Serial;
import java.io.Serializable;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoPeriod;
import java.time.temporal.*;
import java.util.Objects;

import static java.time.temporal.ChronoField.*;

/**
 * An immutable date in the Chinese calendar, including its leap-month flag.
 * 包含闰月标志的不可变农历日期。
 */
public final class ChineseDate implements ChronoLocalDate, Serializable {

    /**
     * The serialization version identifier.
     * 序列化版本标识符。
     */
    @Serial
    private static final long serialVersionUID = 1L;

    //-----------------------------------------------------------------------
    // Instance fields
    // 实例字段
    //-----------------------------------------------------------------------

    /**
     * The chronology defining this date.
     * 定义此日期的历法。
     */
    private final transient ChineseChronology chronology;

    /**
     * The proleptic year.
     * 历法年份。
     */
    private final transient int prolepticYear;

    /**
     * The month-of-year.
     * 年中的月份编号。
     */
    private final transient int monthOfYear;

    /**
     * Whether this month is a leap month.
     * 此月份是否为闰月。
     */
    private final transient boolean leapMonth;

    /**
     * The day-of-month.
     * 月中的日期。
     */
    private final transient int dayOfMonth;

    //-----------------------------------------------------------------------
    // Constructors
    // 构造器
    //-----------------------------------------------------------------------

    /**
     * Creates a date after validating its fields.
     * 校验日期字段后创建日期。
     *
     * @param chronology the chronology to use
     *                   使用的历法
     * @param prolepticYear the proleptic year
     *                      历法年份
     * @param monthOfYear the month number
     *                    月份编号
     * @param leapMonth whether the month is a leap month
     *                  是否为闰月
     * @param dayOfMonth the day of the month
     *                   月中的日期
     * @throws DateTimeException if the date or leap-month flag is invalid or outside the supported range
     *                           如果日期或闰月标志无效，或超出支持范围
     * @throws NullPointerException if the chronology is {@code null}
     *                              如果历法为 {@code null}
     */
    private ChineseDate(ChineseChronology chronology, int prolepticYear, int monthOfYear, boolean leapMonth, int dayOfMonth) {
        chronology.getEpochDay(prolepticYear, monthOfYear, leapMonth, dayOfMonth);

        this.chronology = chronology;
        this.prolepticYear = prolepticYear;
        this.monthOfYear = monthOfYear;
        this.leapMonth = leapMonth;
        this.dayOfMonth = dayOfMonth;
    }

    /**
     * Creates a date from an epoch day.
     * 根据纪元天数创建日期。
     *
     * @param chronology the chronology to use
     *                   使用的历法
     * @param epochDay the number of days since 1970-01-01 (ISO)
     *                 自 ISO 日期 1970-01-01 起的天数
     * @throws DateTimeException if the epoch day is outside the supported range
     *                           如果纪元天数超出支持范围
     * @throws NullPointerException if the chronology is {@code null}
     *                              如果历法为 {@code null}
     */
    private ChineseDate(ChineseChronology chronology, long epochDay) {
        if (epochDay < Integer.MIN_VALUE || epochDay > Integer.MAX_VALUE) {
            throw new DateTimeException("Date is out of range for Chinese calendar: " + epochDay);
        }
        int[] chineseDateInfo = chronology.getChineseDateInfo((int) epochDay);
        this.chronology = chronology;
        this.prolepticYear = chineseDateInfo[0];
        this.monthOfYear = chineseDateInfo[1];
        this.dayOfMonth = chineseDateInfo[2];
        this.leapMonth = chineseDateInfo[3] != 0;
    }

    //-----------------------------------------------------------------------
    // Public factory methods
    // 公开工厂方法
    //-----------------------------------------------------------------------

    /**
     * Gets the current date using the system clock and default time zone.
     * 使用系统时钟和默认时区获取当前日期。
     *
     * @return the current Chinese date
     *         当前农历日期
     * @throws DateTimeException if the current date is outside the supported range
     *                           如果当前日期超出支持范围
     */
    public static ChineseDate now() {
        return now(Clock.systemDefaultZone());
    }

    /**
     * Gets the current date using the specified clock.
     * 使用指定时钟获取当前日期。
     *
     * @param clock the clock to use
     *              使用的时钟
     * @return the current Chinese date
     *         当前农历日期
     * @throws NullPointerException if the clock is {@code null}
     *                              如果时钟为 {@code null}
     * @throws DateTimeException if the current date is outside the supported range
     *                           如果当前日期超出支持范围
     */
    public static ChineseDate now(Clock clock) {
        return ChineseDate.ofEpochDay(ChineseChronology.INSTANCE, LocalDate.now(clock).toEpochDay());
    }

    //-----------------------------------------------------------------------
    // Package-private factory methods
    // 包级工厂方法
    //-----------------------------------------------------------------------

    /**
     * Creates a date in a regular month.
     * 创建普通月中的日期。
     *
     * @param chrono the chronology to use
     *               使用的历法
     * @param prolepticYear the proleptic year
     *                      历法年份
     * @param monthOfYear the month number
     *                    月份编号
     * @param dayOfMonth the day of the month
     *                   月中的日期
     * @return the Chinese date
     *         农历日期
     * @throws DateTimeException if the date is invalid or outside the supported range
     *                           如果日期无效或超出支持范围
     * @throws NullPointerException if the chronology is {@code null}
     *                              如果历法为 {@code null}
     */
    static ChineseDate of(ChineseChronology chrono, int prolepticYear, int monthOfYear, int dayOfMonth) {
        return of(chrono, prolepticYear, monthOfYear, false, dayOfMonth);
    }

    /**
     * Creates a date with an explicit leap-month flag.
     * 使用明确的闰月标志创建日期。
     *
     * @param chrono the chronology to use
     *               使用的历法
     * @param prolepticYear the proleptic year
     *                      历法年份
     * @param monthOfYear the month number
     *                    月份编号
     * @param leapMonth whether the month is a leap month
     *                  是否为闰月
     * @param dayOfMonth the day of the month
     *                   月中的日期
     * @return the Chinese date
     *         农历日期
     * @throws DateTimeException if the date or leap-month flag is invalid or outside the supported range
     *                           如果日期或闰月标志无效，或超出支持范围
     * @throws NullPointerException if the chronology is {@code null}
     *                              如果历法为 {@code null}
     */
    static ChineseDate of(ChineseChronology chrono, int prolepticYear, int monthOfYear, boolean leapMonth, int dayOfMonth) {
        return new ChineseDate(chrono, prolepticYear, monthOfYear, leapMonth, dayOfMonth);
    }

    /**
     * Creates a date from an epoch day.
     * 根据纪元天数创建日期。
     *
     * @param chronology the chronology to use
     *                   使用的历法
     * @param epochDay the number of days since 1970-01-01 (ISO)
     *                 自 ISO 日期 1970-01-01 起的天数
     * @return the Chinese date
     *         农历日期
     * @throws DateTimeException if the epoch day is outside the supported range
     *                           如果纪元天数超出支持范围
     * @throws NullPointerException if the chronology is {@code null}
     *                              如果历法为 {@code null}
     */
    static ChineseDate ofEpochDay(ChineseChronology chronology, long epochDay) {
        return new ChineseDate(chronology, epochDay);
    }

    //-----------------------------------------------------------------------
    // Date properties and epoch-day conversion
    // 日期属性与纪元天数转换
    //-----------------------------------------------------------------------

    /**
     * Gets the chronology defining this date.
     * 获取定义此日期的历法。
     *
     * @return the chronology
     *         历法
     */
    @Override
    public ChineseChronology getChronology() {
        return chronology;
    }

    /**
     * Gets the length of this month, accounting for the leap-month flag.
     * 获取当前月份的天数，并考虑闰月标志。
     *
     * @return the number of days in this month
     *         当前月份的天数
     */
    @Override
    public int lengthOfMonth() {
        return chronology.getMonthLength(prolepticYear, monthOfYear, leapMonth);
    }

    /**
     * Checks whether this date is in a leap month.
     * 检查此日期是否位于闰月。
     *
     * @return {@code true} if this is a leap month
     *         如果当前月份为闰月则返回 {@code true}
     */
    public boolean isLeapMonth() {
        return leapMonth;
    }

    /**
     * Gets the proleptic year.
     * 获取历法年份。
     *
     * @return the proleptic year
     *         历法年份
     */
    int prolepticYear() {
        return prolepticYear;
    }

    /**
     * Gets the month number, without distinguishing regular and leap months.
     * 获取月份编号，不区分普通月与同名闰月。
     *
     * @return the one-based month number
     *         从一开始计数的月份编号
     */
    int monthOfYear() {
        return monthOfYear;
    }

    /**
     * Gets the day-of-year, including days in any preceding leap month.
     * 获取年内日序，包括此前闰月的天数。
     *
     * @return the one-based day-of-year
     *         从一开始计数的年内日序
     */
    public int getDayOfYear() {
        long yearStart = chronology.getEpochDay(prolepticYear, 1, false, 1);
        return Math.toIntExact(toEpochDay() - yearStart + 1);
    }

    /**
     * Gets the ISO day-of-week corresponding to this date.
     * 获取此日期对应的 ISO 星期。
     *
     * @return the day-of-week
     *         星期
     */
    public DayOfWeek getDayOfWeek() {
        int dow0 = Math.floorMod(toEpochDay() + 3, 7);
        return DayOfWeek.of(dow0 + 1);
    }

    /**
     * Gets the number of days in this year, including any leap month.
     * 获取当前年份的总天数，包括闰月。
     *
     * @return the length of this year
     *         当前年份的天数
     */
    @Override
    public int lengthOfYear() {
        return chronology.getYearLength(prolepticYear);
    }

    /**
     * Converts this date to the number of days since 1970-01-01 (ISO).
     * 将此日期转换为自 ISO 日期 1970-01-01 起的天数。
     *
     * @return the epoch day
     *         纪元天数
     */
    @Override
    public long toEpochDay() {
        return chronology.getEpochDay(prolepticYear, monthOfYear, leapMonth, dayOfMonth);
    }

    //-----------------------------------------------------------------------
    // Date adjustments and arithmetic
    // 日期调整与运算
    //-----------------------------------------------------------------------

    /**
     * Adjusts this date using the specified adjuster.
     * 使用指定调整器调整此日期。
     *
     * @param adjuster the adjuster to apply
     *                 使用的调整器
     * @return the adjusted Chinese date
     *         调整后的农历日期
     * @throws NullPointerException if the adjuster is {@code null}
     *                              如果调整器为 {@code null}
     * @throws DateTimeException if the adjustment cannot be made
     *                           如果无法完成调整
     * @throws ClassCastException if the adjuster returns an incompatible date type
     *                            如果调整器返回不兼容的日期类型
     */
    @Override
    public ChineseDate with(TemporalAdjuster adjuster) {
        return (ChineseDate) ChronoLocalDate.super.with(adjuster);
    }

    /**
     * Sets a field, falling back to a valid date where the field permits it.
     * 设置字段，并在该字段允许时回退到有效日期。
     *
     * @param field the field to set
     *              要设置的字段
     * @param newValue the new field value
     *                 新字段值
     * @return the adjusted date
     *         调整后的日期
     * @throws NullPointerException if the field is {@code null}
     *                              如果字段为 {@code null}
     * @throws UnsupportedTemporalTypeException if the field is unsupported
     *                                         如果字段不受支持
     * @throws DateTimeException if the value or resulting date is invalid
     *                           如果字段值或结果日期无效
     */
    @Override
    public ChineseDate with(TemporalField field, long newValue) {
        if (field instanceof ChronoField chronoField) {
            if (!isSupported(chronoField)) {
                throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
            }
            chronology.range(chronoField).checkValidValue(newValue, chronoField);
            return switch (chronoField) {
                case DAY_OF_WEEK -> plusDays(newValue - getDayOfWeek().getValue());
                case ALIGNED_DAY_OF_WEEK_IN_MONTH -> plusDays(newValue - getLong(ALIGNED_DAY_OF_WEEK_IN_MONTH));
                case ALIGNED_DAY_OF_WEEK_IN_YEAR -> plusDays(newValue - getLong(ALIGNED_DAY_OF_WEEK_IN_YEAR));
                case DAY_OF_MONTH -> resolvePreviousValid(prolepticYear, monthOfYear, leapMonth, (int) newValue);
                case DAY_OF_YEAR -> plusDays(Math.min(newValue, lengthOfYear()) - getDayOfYear());
                case EPOCH_DAY -> ChineseDate.ofEpochDay(chronology, newValue);
                case ALIGNED_WEEK_OF_MONTH -> plusDays((newValue - getLong(ALIGNED_WEEK_OF_MONTH)) * 7);
                case ALIGNED_WEEK_OF_YEAR -> plusDays((newValue - getLong(ALIGNED_WEEK_OF_YEAR)) * 7);
                case MONTH_OF_YEAR -> resolvePreviousValid(prolepticYear, (int) newValue, leapMonth, dayOfMonth);
                case YEAR, YEAR_OF_ERA -> plusYears(newValue - prolepticYear);
                case ERA -> this;
                default -> throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
            };
        } else if (field instanceof ChineseTemporalField chineseTemporalField) {
            chineseTemporalField.rangeRefinedBy(this).checkValidValue(newValue, chineseTemporalField);
            return switch (chineseTemporalField) {
                case IS_LEAP_MONTH -> {
                    if (newValue == 1 && chronology.getLeapMonth(prolepticYear) != monthOfYear) {
                        throw new DateTimeException("Invalid leap month: " + monthOfYear + " in year " + prolepticYear);
                    }
                    yield resolvePreviousValid(prolepticYear, monthOfYear, newValue == 1, dayOfMonth);
                }
                case MONTH_AS_ORDINAL_OF_YEAR -> {
                    int leapMonth = chronology.getLeapMonth(prolepticYear);
                    boolean isLeapMonth = false;
                    if (chronology.isLeapYear(prolepticYear)) {
                        if (newValue == leapMonth + 1) {
                            isLeapMonth = true;
                        }
                        if (newValue > leapMonth) {
                            newValue--;
                        }
                    }
                    yield resolvePreviousValid(prolepticYear, (int) newValue, isLeapMonth, dayOfMonth);
                }
            };
        }
        return (ChineseDate) ChronoLocalDate.super.with(field, newValue);
    }

    /**
     * Adds the specified amount to this date.
     * 将指定时间量加到此日期。
     *
     * @param amount the amount to add
     *               要增加的时间量
     * @return the resulting Chinese date
     *         结果农历日期
     * @throws NullPointerException if the amount is {@code null}
     *                              如果时间量为 {@code null}
     * @throws DateTimeException if the addition cannot be made
     *                           如果无法完成加法
     * @throws ArithmeticException if numeric overflow occurs
     *                             如果发生数值溢出
     */
    @Override
    public ChineseDate plus(TemporalAmount amount) {
        return (ChineseDate) ChronoLocalDate.super.plus(amount);
    }

    /**
     * Subtracts the specified amount from this date.
     * 从此日期减去指定时间量。
     *
     * @param amount the amount to subtract
     *               要减少的时间量
     * @return the resulting Chinese date
     *         结果农历日期
     * @throws NullPointerException if the amount is {@code null}
     *                              如果时间量为 {@code null}
     * @throws DateTimeException if the subtraction cannot be made
     *                           如果无法完成减法
     * @throws ArithmeticException if numeric overflow occurs
     *                             如果发生数值溢出
     */
    @Override
    public ChineseDate minus(TemporalAmount amount) {
        return (ChineseDate) ChronoLocalDate.super.minus(amount);
    }

    /**
     * Adds a quantity of the specified unit, counting leap months as separate months.
     * 增加指定单位的数量，闰月作为独立月份计数。
     *
     * @param amountToAdd the quantity to add
     *                    要增加的数量
     * @param unit the unit to use
     *             使用的单位
     * @return the resulting Chinese date
     *         结果农历日期
     * @throws NullPointerException if the unit is {@code null}
     *                              如果单位为 {@code null}
     * @throws UnsupportedTemporalTypeException if the unit is unsupported
     *                                         如果单位不受支持
     * @throws DateTimeException if the resulting date is outside the supported range
     *                           如果结果日期超出支持范围
     * @throws ArithmeticException if numeric overflow occurs
     *                             如果发生数值溢出
     */
    @Override
    public ChineseDate plus(long amountToAdd, TemporalUnit unit) {
        if (unit instanceof ChronoUnit chronoUnit) {
            return switch (chronoUnit) {
                case DAYS -> plusDays(amountToAdd);
                case WEEKS -> plusDays(Math.multiplyExact(amountToAdd, 7));
                case MONTHS -> plusMonths(amountToAdd);
                case YEARS -> plusYears(amountToAdd);
                case DECADES -> plusYears(Math.multiplyExact(amountToAdd, 10));
                case CENTURIES -> plusYears(Math.multiplyExact(amountToAdd, 100));
                case MILLENNIA -> plusYears(Math.multiplyExact(amountToAdd, 1000));
                case ERAS -> with(ERA, Math.addExact(getLong(ERA), amountToAdd));
                default -> throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
            };
        }
        return (ChineseDate) ChronoLocalDate.super.plus(amountToAdd, unit);
    }

    /**
     * Subtracts a quantity of the specified unit.
     * 减去指定单位的数量。
     *
     * @param amountToSubtract the quantity to subtract
     *                         要减少的数量
     * @param unit the unit to use
     *             使用的单位
     * @return the resulting Chinese date
     *         结果农历日期
     * @throws NullPointerException if the unit is {@code null}
     *                              如果单位为 {@code null}
     * @throws UnsupportedTemporalTypeException if the unit is unsupported
     *                                         如果单位不受支持
     * @throws DateTimeException if the resulting date is outside the supported range
     *                           如果结果日期超出支持范围
     * @throws ArithmeticException if numeric overflow occurs
     *                             如果发生数值溢出
     */
    @Override
    public ChineseDate minus(long amountToSubtract, TemporalUnit unit) {
        return (ChineseDate) ChronoLocalDate.super.minus(amountToSubtract, unit);
    }

    /**
     * Adds years, falling back to the regular month if the target leap month is absent,
     * then clipping the day to the target month's length.
     * 增加年份；若目标年份不存在对应闰月，则回退到普通月，再将日数限制在目标月份内。
     *
     * @param years the number of years to add
     *              要增加的年数
     * @return the resulting Chinese date
     *         结果农历日期
     * @throws DateTimeException if the target year is outside the supported range
     *                           如果目标年份超出支持范围
     * @throws ArithmeticException if numeric overflow occurs
     *                             如果发生数值溢出
     */
    public ChineseDate plusYears(long years) {
        if (years == 0) {
            return this;
        }
        long targetYear = Math.addExact(prolepticYear, years);
        chronology.checkValidYear(targetYear);
        int year = Math.toIntExact(targetYear);
        return resolvePreviousValid(year, monthOfYear, leapMonth, dayOfMonth);
    }

    /**
     * Adds months, counting leap months separately and clipping the day if necessary.
     * 增加月份，闰月单独计数，并在必要时截取有效日数。
     *
     * @param months the number of months to add
     *               要增加的月数
     * @return the resulting Chinese date
     *         结果农历日期
     * @throws DateTimeException if the resulting date is outside the supported range
     *                           如果结果日期超出支持范围
     * @throws ArithmeticException if numeric overflow occurs
     *                             如果发生数值溢出
     */
    public ChineseDate plusMonths(long months) {
        return months == 0 ? this : ofEpochDay(chronology, chronology.epochDayAfterMonths(this, months));
    }

    /**
     * Adds days on the continuous timeline.
     * 沿连续时间线增加天数。
     *
     * @param days the number of days to add
     *             要增加的天数
     * @return the resulting Chinese date
     *         结果农历日期
     * @throws DateTimeException if the resulting date is outside the supported range
     *                           如果结果日期超出支持范围
     * @throws ArithmeticException if numeric overflow occurs
     *                             如果发生数值溢出
     */
    public ChineseDate plusDays(long days) {
        return days == 0 ? this : ofEpochDay(chronology, Math.addExact(toEpochDay(), days));
    }

    //-----------------------------------------------------------------------
    // Temporal field access
    // 时态字段访问
    //-----------------------------------------------------------------------

    /**
     * Gets a field's range, refining month and year lengths for this date.
     * 获取字段范围，并根据此日期细化月份和年份的天数范围。
     *
     * @param field the field to query
     *              要查询的字段
     * @return the field range
     *         字段范围
     * @throws NullPointerException if the field is {@code null}
     *                              如果字段为 {@code null}
     * @throws DateTimeException if the field's range cannot be obtained
     *                           如果无法获取字段范围
     */
    @Override
    public ValueRange range(TemporalField field) {
        if (field instanceof ChronoField chronoField) {
            if (!isSupported(chronoField)) {
                throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
            }
            return switch (chronoField) {
                case DAY_OF_MONTH -> ValueRange.of(1, lengthOfMonth());
                case DAY_OF_YEAR -> ValueRange.of(1, lengthOfYear());
                case ALIGNED_WEEK_OF_YEAR -> ValueRange.of(1, (lengthOfYear() + 6) / 7);
                default -> chronology.range(chronoField);
            };
        }
        return field.rangeRefinedBy(this);
    }

    /**
     * Checks whether a field is supported by this date.
     * 检查此日期是否支持指定字段。
     *
     * @param field the field to check, or {@code null}
     *              要检查的字段，可以为 {@code null}
     * @return whether the field is supported; {@code false} for {@code null}
     *         是否支持该字段；{@code null} 返回 {@code false}
     */
    @Override
    public boolean isSupported(TemporalField field) {
        if (field instanceof ChronoField chronoField) {
            return switch (chronoField) {
                case NANO_OF_SECOND,
                     NANO_OF_DAY,
                     MICRO_OF_SECOND,
                     MICRO_OF_DAY,
                     MILLI_OF_SECOND,
                     MILLI_OF_DAY,
                     SECOND_OF_MINUTE,
                     SECOND_OF_DAY,
                     MINUTE_OF_HOUR,
                     MINUTE_OF_DAY,
                     HOUR_OF_AMPM,
                     CLOCK_HOUR_OF_AMPM,
                     HOUR_OF_DAY,
                     CLOCK_HOUR_OF_DAY,
                     AMPM_OF_DAY,
                     PROLEPTIC_MONTH,
                     INSTANT_SECONDS,
                     OFFSET_SECONDS -> false;
                case DAY_OF_WEEK,
                     ALIGNED_DAY_OF_WEEK_IN_MONTH,
                     ALIGNED_DAY_OF_WEEK_IN_YEAR,
                     DAY_OF_MONTH,
                     DAY_OF_YEAR,
                     EPOCH_DAY,
                     ALIGNED_WEEK_OF_MONTH,
                     ALIGNED_WEEK_OF_YEAR,
                     MONTH_OF_YEAR,
                     YEAR_OF_ERA,
                     YEAR,
                     ERA -> true;
            };
        } else if (field instanceof ChineseTemporalField) {
            return true;
        }
        return ChronoLocalDate.super.isSupported(field);
    }

    /**
     * Gets a field value, including Chinese-calendar-specific fields.
     * 获取字段值，支持农历专有字段。
     *
     * @param field the field to query
     *              要查询的字段
     * @return the field value
     *         字段值
     * @throws NullPointerException if the field is {@code null}
     *                              如果字段为 {@code null}
     * @throws UnsupportedTemporalTypeException if the field is unsupported
     *                                         如果字段不受支持
     * @throws DateTimeException if the field value cannot be obtained
     *                           如果无法获取字段值
     */
    @Override
    public long getLong(TemporalField field) {
        if (field instanceof ChronoField chronoField) {
            return switch (chronoField) {
                case DAY_OF_WEEK -> getDayOfWeek().getValue();
                case ALIGNED_DAY_OF_WEEK_IN_MONTH -> (dayOfMonth - 1) % 7 + 1;
                case ALIGNED_DAY_OF_WEEK_IN_YEAR -> (getDayOfYear() - 1) % 7 + 1;
                case DAY_OF_MONTH -> dayOfMonth;
                case DAY_OF_YEAR -> getDayOfYear();
                case EPOCH_DAY -> toEpochDay();
                case ALIGNED_WEEK_OF_MONTH -> (dayOfMonth - 1) / 7 + 1;
                case ALIGNED_WEEK_OF_YEAR -> (getDayOfYear() - 1) / 7 + 1;
                case MONTH_OF_YEAR -> monthOfYear;
                case YEAR_OF_ERA,
                     YEAR -> prolepticYear;
                case ERA -> ChineseEra.CHINESE_ERA.getValue();
                default -> throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
            };
        } else if (field instanceof ChineseTemporalField chineseTemporalField) {
            return switch (chineseTemporalField) {
                case IS_LEAP_MONTH -> isLeapMonth() ? 1 : 0;
                case MONTH_AS_ORDINAL_OF_YEAR -> {
                    int month = monthOfYear;
                    if (chronology.isLeapYear(prolepticYear)) {
                        int leapMonth = chronology.getLeapMonth(prolepticYear);
                        if (month > leapMonth || isLeapMonth()) {
                            month++;
                        }
                    }
                    yield month;
                }
            };
        }
        return field.getFrom(this);
    }

    //-----------------------------------------------------------------------
    // Date differences
    // 日期间隔
    //-----------------------------------------------------------------------

    /**
     * Calculates the whole number of units until another date.
     * 计算到另一日期之间的完整单位数量。
     *
     * @param endExclusive the exclusive end date, converted to this chronology
     *                     不包含在内的结束日期，将转换为当前历法
     * @param unit the unit to use
     *             使用的单位
     * @return the signed number of whole units
     *         带符号的完整单位数量
     * @throws NullPointerException if the end date or unit is {@code null}
     *                              如果结束日期或单位为 {@code null}
     * @throws UnsupportedTemporalTypeException if the unit is unsupported
     *                                         如果单位不受支持
     * @throws DateTimeException if the end date cannot be converted to a supported date
     *                           如果结束日期无法转换为受支持的日期
     */
    @Override
    public long until(Temporal endExclusive, TemporalUnit unit) {
        ChineseDate end = chronology.date(endExclusive);
        long days = end.toEpochDay() - toEpochDay();
        if (unit instanceof ChronoUnit chronoUnit) {
            return switch (chronoUnit) {
                case DAYS -> days;
                case WEEKS -> days / 7;
                case MONTHS -> chronology.monthsBetween(this, end);
                case YEARS -> yearsUntil(end);
                case DECADES -> yearsUntil(end) / 10;
                case CENTURIES -> yearsUntil(end) / 100;
                case MILLENNIA -> yearsUntil(end) / 1000;
                case ERAS -> 0;
                default -> throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
            };
        }
        return unit.between(this, end);
    }

    /**
     * Calculates a period of years, months, and days until another date.
     * 计算到另一日期之间由年、月、日组成的期间。
     *
     * @param endDateExclusive the exclusive end date, converted to this chronology
     *                         不包含在内的结束日期，将转换为当前历法
     * @return the period in this chronology
     *         当前历法的日期期间
     * @throws NullPointerException if the end date is {@code null}
     *                              如果结束日期为 {@code null}
     * @throws DateTimeException if the end date cannot be converted or the period cannot be calculated
     *                           如果结束日期无法转换或无法计算期间
     */
    @Override
    public ChronoPeriod until(ChronoLocalDate endDateExclusive) {
        ChineseDate end = chronology.date(endDateExclusive);
        long years = yearsUntil(end);
        ChineseDate afterYears = plusYears(years);
        long months = chronology.monthsBetween(afterYears, end);
        long monthDays = end.toEpochDay() - afterYears.plusMonths(months).toEpochDay();
        return chronology.period(Math.toIntExact(years), Math.toIntExact(months), Math.toIntExact(monthDays));
    }

    //-----------------------------------------------------------------------
    // Equality, hashing, and text representation
    // 相等性、哈希值与文本表示
    //-----------------------------------------------------------------------

    /**
     * Compares the chronology and all date fields, including the leap-month flag.
     * 比较历法及所有日期字段，包括闰月标志。
     *
     * @param obj the object to compare, or {@code null}
     *            要比较的对象，可以为 {@code null}
     * @return whether the objects represent the same Chinese date
     *         两个对象是否表示相同的农历日期
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ChineseDate other)) {
            return false;
        }
        return prolepticYear == other.prolepticYear && monthOfYear == other.monthOfYear && leapMonth == other.leapMonth && dayOfMonth == other.dayOfMonth && chronology.equals(other.chronology);
    }


    /**
     * Gets a hash code from the chronology and all date fields.
     * 根据历法及所有日期字段计算哈希值。
     *
     * @return the hash code
     *         哈希值
     */
    @Override
    public int hashCode() {
        return Objects.hash(chronology, prolepticYear, monthOfYear, leapMonth, dayOfMonth);
    }

    /**
     * Returns the chronology ID and date, using {@code bis} after leap months
     * and two digits for the month and day.
     * 返回历法 ID 和日期，在闰月月份后添加 {@code bis}，月和日使用两位数字。
     *
     * @return the formatted date text
     *         格式化的日期文本
     */
    @Override
    public String toString() {
        return getChronology().getId() + " " + prolepticYear
                + "-" + (monthOfYear < 10 ? "0" : "") + monthOfYear + (leapMonth ? "bis" : "")
                + "-" + (dayOfMonth < 10 ? "0" : "") + dayOfMonth;
    }

    //-----------------------------------------------------------------------
    // Private date adjustment helpers
    // 私有日期调整辅助方法
    //-----------------------------------------------------------------------

    /**
     * Falls back to a regular month if the leap month is absent, then clips the day.
     * 如果闰月不存在则回退到普通月，再截取有效日数。
     *
     * @param prolepticYear the target year
     *                      目标年份
     * @param month the target month number
     *              目标月份编号
     * @param leapMonth whether the target month is a leap month
     *                  目标月份是否为闰月
     * @param day the requested day
     *            指定的日数
     * @return the resolved date
     *         调整后的日期
     * @throws DateTimeException if the resolved date is invalid or outside the supported range
     *                           如果调整后的日期无效或超出支持范围
     */
    private ChineseDate resolvePreviousValid(int prolepticYear, int month, boolean leapMonth, int day) {
        leapMonth = leapMonth && chronology.getLeapMonth(prolepticYear) == month;
        int monthLength = chronology.getMonthLength(prolepticYear, month, leapMonth);
        return ChineseDate.of(chronology, prolepticYear, month, leapMonth, Math.min(day, monthLength));
    }

    /**
     * Counts complete years using the adjusted anniversary in the end year.
     * 使用结束年份中调整后的周年日期计算完整年数。
     *
     * @param end the end date in this chronology
     *            当前历法中的结束日期
     * @return the signed number of complete years
     *         带符号的完整年数
     * @throws DateTimeException if the anniversary is outside the supported range
     *                           如果周年日期超出支持范围
     */
    private long yearsUntil(ChineseDate end) {
        long years = (long) end.prolepticYear - prolepticYear;
        long anniversary = plusYears(years).toEpochDay();
        if (years > 0 && anniversary > end.toEpochDay()) {
            years--;
        } else if (years < 0 && anniversary < end.toEpochDay()) {
            years++;
        }
        return years;
    }

    //-----------------------------------------------------------------------
    // Serialization methods
    // 序列化相关方法
    //-----------------------------------------------------------------------

    /**
     * Replaces this date with its serialization proxy.
     * 将此日期替换为序列化代理。
     *
     * @return the serialization proxy
     *         序列化代理
     */
    @Serial
    private Object writeReplace() {
        return new Ser(chronology, prolepticYear, monthOfYear, leapMonth, dayOfMonth);
    }

    /**
     * Rejects direct deserialization in favor of the serialization proxy.
     * 拒绝直接反序列化，要求使用序列化代理。
     *
     * @param input the input stream
     *              输入流
     * @throws InvalidObjectException always, because a serialization proxy is required
     *                                始终抛出此异常，因为必须使用序列化代理
     */
    @Serial
    private void readObject(ObjectInputStream input) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    /**
     * Serialization proxy preserving the Chinese date fields and chronology.
     * 保存农历日期字段及历法的序列化代理。
     *
     * @param chronology the chronology to restore
     *                   要恢复的历法
     * @param prolepticYear the proleptic year
     *                      历法年份
     * @param monthOfYear the month number
     *                    月份编号
     * @param leapMonth whether the month is a leap month
     *                  是否为闰月
     * @param dayOfMonth the day of the month
     *                   月中的日期
     */
    private record Ser(ChineseChronology chronology, int prolepticYear, int monthOfYear,
                       boolean leapMonth, int dayOfMonth) implements Serializable {
        /**
         * Reconstructs and validates the date through its normal factory.
         * 通过正常工厂方法重建并校验日期。
         *
         * @return the restored Chinese date
         *         恢复后的农历日期
         * @throws ObjectStreamException if the chronology or date fields are invalid
         *                               如果历法或日期字段无效
         */
        @Serial
        private Object readResolve() throws ObjectStreamException {
            try {
                return ChineseDate.of(chronology, prolepticYear, monthOfYear, leapMonth, dayOfMonth);
            } catch (DateTimeException | NullPointerException ex) {
                throw new InvalidObjectException("Invalid Chinese date", ex);
            }
        }
    }
}
