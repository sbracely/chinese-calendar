package io.github.sbracely.chinese.calendar;

import java.time.DateTimeException;
import java.time.temporal.*;
import java.util.Locale;
import java.util.Objects;

/**
 * Chinese-calendar fields supported by {@link ChineseDate}, not date-time objects.
 * 由 {@link ChineseDate} 支持的农历字段，不支持日期时间对象。
 * <p>
 * Parsing is resolved by {@link ChineseChronology#resolveDate}.
 * 解析由 {@link ChineseChronology#resolveDate} 负责。
 */
public enum ChineseTemporalField implements TemporalField {
    /**
     * The leap-month flag: 0 for a regular month and 1 for a leap month.
     * 闰月标志：普通月为 0，闰月为 1。
     */
    IS_LEAP_MONTH("IsLeapMonth", ChronoUnit.MONTHS, ChronoUnit.MONTHS, ValueRange.of(0, 1)),
    /**
     * The one-based month position in a year, counting leap months separately.
     * 从一开始计数的年内月份序号，闰月单独计数。
     */
    MONTH_AS_ORDINAL_OF_YEAR("MonthAsOrdinalOfYear", ChronoUnit.MONTHS, ChronoUnit.YEARS, ValueRange.of(1, 12, 13));

    /**
     * The non-localized display name.
     * 未本地化的显示名称。
     */
    private final String name;
    /**
     * The base unit of the field.
     * 字段的基本单位。
     */
    private final TemporalUnit baseUnit;
    /**
     * The unit defining the field's range.
     * 定义字段范围的单位。
     */
    private final TemporalUnit rangeUnit;
    /**
     * The general range before refinement for a particular year.
     * 按具体年份细化之前的通用范围。
     */
    private final ValueRange range;

    /**
     * Creates a field with its name, units, and general range.
     * 使用名称、单位及通用范围创建字段。
     *
     * @param name the display name
     *             显示名称
     * @param baseUnit the base unit
     *                 基本单位
     * @param rangeUnit the range unit
     *                  范围单位
     * @param range the general range
     *              通用范围
     */
    ChineseTemporalField(String name, TemporalUnit baseUnit, TemporalUnit rangeUnit, ValueRange range) {
        this.name = name;
        this.baseUnit = baseUnit;
        this.rangeUnit = rangeUnit;
        this.range = range;
    }

    /**
     * Gets the field name, currently identical for all locales.
     * 获取字段名称，目前所有语言环境返回相同名称。
     *
     * @param locale the requested locale
     *               请求的语言环境
     * @return the display name
     *         显示名称
     * @throws NullPointerException if the locale is {@code null}
     *                              如果语言环境为 {@code null}
     */
    @Override
    public String getDisplayName(Locale locale) {
        Objects.requireNonNull(locale, "locale");
        return name;
    }

    /**
     * Gets the base unit.
     * 获取基本单位。
     *
     * @return the base unit
     *         基本单位
     */
    @Override
    public TemporalUnit getBaseUnit() {
        return baseUnit;
    }

    /**
     * Gets the range unit.
     * 获取范围单位。
     *
     * @return the range unit
     *         范围单位
     */
    @Override
    public TemporalUnit getRangeUnit() {
        return rangeUnit;
    }

    /**
     * Gets the general range across common and leap years.
     * 获取适用于平年与闰年的通用范围。
     *
     * @return the general range
     *         通用范围
     */
    @Override
    public ValueRange range() {
        return range;
    }

    /**
     * Checks whether this is a date-based field.
     * 检查此字段是否基于日期。
     *
     * @return always {@code true}
     *         始终为 {@code true}
     */
    @Override
    public boolean isDateBased() {
        return true;
    }

    /**
     * Checks whether this is a time-based field.
     * 检查此字段是否基于时间。
     *
     * @return always {@code false}
     *         始终为 {@code false}
     */
    @Override
    public boolean isTimeBased() {
        return false;
    }

    /**
     * Checks whether a temporal object is a supported Chinese date.
     * 检查时态对象是否为受支持的农历日期。
     *
     * @param temporal the temporal object to check
     *                 要检查的时态对象
     * @return whether the object is supported
     *         是否支持该对象
     * @throws NullPointerException if the temporal object is {@code null}
     *                              如果时态对象为 {@code null}
     */
    @Override
    public boolean isSupportedBy(TemporalAccessor temporal) {
        Objects.requireNonNull(temporal, "temporal");
        return temporal instanceof ChineseDate date && date.isSupported(this);
    }

    /**
     * Refines the range for the date's year: common years have twelve ordinal months
     * and a leap flag fixed at zero; leap years allow thirteen ordinal months and flags 0 or 1.
     * 根据日期所属年份细化范围：平年的月份序号为 1 至 12，闰月标志仅为零；
     * 闰年的月份序号为 1 至 13，闰月标志可为 0 或 1。
     *
     * @param temporal the Chinese date
     *                 农历日期
     * @return the range for the year
     *         该年份的范围
     * @throws NullPointerException if the temporal object is {@code null}
     *                              如果时态对象为 {@code null}
     * @throws UnsupportedTemporalTypeException if the object is not a Chinese date
     *                                         如果对象不是农历日期
     */
    @Override
    public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
        Objects.requireNonNull(temporal, "temporal");
        if (!(temporal instanceof ChineseDate date)) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + this);
        }
        int year = date.getYear();
        return switch (this) {
            case IS_LEAP_MONTH -> ValueRange.of(0, date.getChronology().isLeapYear(year) ? 1 : 0);
            case MONTH_AS_ORDINAL_OF_YEAR ->
                    ValueRange.of(1, date.getChronology().isLeapYear(year) ? 13 : 12);
        };
    }

    /**
     * Reads this field from a Chinese date.
     * 从农历日期读取此字段。
     *
     * @param temporal the Chinese date
     *                 农历日期
     * @return the field value
     *         字段值
     * @throws NullPointerException if the temporal object is {@code null}
     *                              如果时态对象为 {@code null}
     * @throws UnsupportedTemporalTypeException if the object is not a Chinese date
     *                                         如果对象不是农历日期
     */
    @Override
    public long getFrom(TemporalAccessor temporal) {
        if (!isSupportedBy(temporal)) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + this);
        }
        return temporal.getLong(this);
    }

    /**
     * Sets this field on a Chinese date, validating the value for its year.
     * 设置农历日期的此字段，并按所属年份校验数值。
     *
     * @param <R> the temporal type
     *            时态类型
     * @param temporal the Chinese date to adjust
     *                 要调整的农历日期
     * @param newValue the new field value
     *                 新字段值
     * @return the adjusted date
     *         调整后的日期
     * @throws NullPointerException if the temporal object is {@code null}
     *                              如果时态对象为 {@code null}
     * @throws UnsupportedTemporalTypeException if the object is not a Chinese date
     *                                         如果对象不是农历日期
     * @throws DateTimeException if the value is invalid or the leap month does not exist
     *                           如果数值无效或闰月不存在
     */
    @Override
    @SuppressWarnings("unchecked")
    public <R extends Temporal> R adjustInto(R temporal, long newValue) {
        if (!isSupportedBy(temporal)) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + this);
        }
        rangeRefinedBy(temporal).checkValidValue(newValue, this);
        return (R) temporal.with(this, newValue);
    }

}
