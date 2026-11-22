package io.github.sbracely.chinese.calendar;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DecimalStyle;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Immutable, thread-safe formatters for four lunar-date representations with Simplified Chinese fields.
 * 四种以简体中文表示农历字段的不可变、线程安全格式化器。
 * <p>
 * Use {@code date.format(ChineseDateFormatters.SEXAGENARY_YEAR_DATE)} directly,
 * or compose a preset with {@link DateTimeFormatterBuilder#append(DateTimeFormatter)}.
 * 可以直接格式化，也可在原生构建器中追加预设格式器。
 * For custom field combinations, use {@link ChineseDateText#getTextMap(TemporalField)}.
 * 自定义字段组合可使用 {@link ChineseDateText#getTextMap(TemporalField)}。
 * <p>
 * The three cycle-only presets cannot identify an absolute year for standalone date parsing.
 * 仅含干支或生肖纪年的三种预设不能独立确定绝对年份。
 * The preset with a numeric year uses {@link ChronoField#YEAR}.
 * 带数字年份的预设直接使用 {@link ChronoField#YEAR}。
 * Explicit Chinese text remains unchanged by {@link DateTimeFormatter#withLocale(Locale)}.
 * 显式中文文本不会随格式化器的语言环境变化而自动翻译。
 * The related-year preset's era text uses the default locale when this class is initialized.
 * 公历年预设的纪元文本使用本类初始化时的默认语言环境。
 * Changing the default locale or using {@code withLocale} later does not change it.
 * 之后更改默认语言环境或调用 {@code withLocale} 不会改变该文本。
 */
public final class ChineseDateFormatters {
    /**
     * Sexagenary year, numbered lunar month and day, such as 农历乙未年正月初一.
     * 干支纪年、数序纪月、数序纪日，例如“农历乙未年正月初一”。
     */
    public static final DateTimeFormatter SEXAGENARY_YEAR_DATE =
            create(ChineseTemporalField.SEXAGENARY_YEAR, ChronoField.DAY_OF_MONTH, false);

    /**
     * Zodiac year, numbered lunar month and day, such as 农历牛年闰五月十一.
     * 生肖纪年、数序纪月、数序纪日，例如“农历牛年闰五月十一”。
     */
    public static final DateTimeFormatter ZODIAC_YEAR_DATE =
            create(ChineseTemporalField.ZODIAC, ChronoField.DAY_OF_MONTH, false);

    /**
     * Sexagenary year and day with a numbered lunar month, such as 农历甲午年七月庚戌日.
     * 干支纪年、数序纪月、干支纪日，例如“农历甲午年七月庚戌日”。
     */
    public static final DateTimeFormatter SEXAGENARY_YEAR_DAY_DATE =
            create(ChineseTemporalField.SEXAGENARY_YEAR, ChineseTemporalField.SEXAGENARY_DAY, false);

    /**
     * The Gregorian year containing lunar New Year, followed by sexagenary year and numbered month/day.
     * 农历年年首所在公历年、干支纪年、数序纪月、数序纪日。
     * For example, 公元2016年农历丙申年十一月廿九 with a Simplified Chinese default locale.
     * 例如默认语言环境为简体中文时，“公元2016年农历丙申年十一月廿九”。
     * The prefix is not the Gregorian year of the date being formatted.
     * 前缀并非当前日期对应的公历年份。
     * It uses {@link ChineseDate#getYear()}, exposed as {@link ChronoField#YEAR}.
     * 此年份即 {@link ChineseDate#getYear()}，通过 {@link ChronoField#YEAR} 获取。
     * The localized era text is intended for modern common-era dates, not a BCE era convention.
     * 本地化纪元文本用于现代公元日期，不表示完整的公元前纪年规则。
     */
    public static final DateTimeFormatter RELATED_GREGORIAN_YEAR_DATE =
            create(ChineseTemporalField.SEXAGENARY_YEAR, ChronoField.DAY_OF_MONTH, true);

    private ChineseDateFormatters() {
    }

    private static DateTimeFormatter create(TemporalField year, TemporalField day, boolean relatedYear) {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
        if (relatedYear) {
            builder.appendLiteral(ResourceBundle.getBundle("cldr.chinese-calendar")
                            .getString("era.1"))
                    .appendValue(ChronoField.YEAR)
                    .appendLiteral('年');
        }
        builder.appendLiteral("农历")
                .appendText(year, ChineseDateText.getTextMap(year)).appendLiteral('年')
                .appendText(ChineseTemporalField.IS_LEAP_MONTH,
                        ChineseDateText.getTextMap(ChineseTemporalField.IS_LEAP_MONTH))
                .appendText(ChronoField.MONTH_OF_YEAR, ChineseDateText.getTextMap(ChronoField.MONTH_OF_YEAR))
                .appendText(day, ChineseDateText.getTextMap(day));
        if (day == ChineseTemporalField.SEXAGENARY_DAY) {
            builder.appendLiteral('日');
        }
        return builder.toFormatter(Locale.SIMPLIFIED_CHINESE)
                .withChronology(ChineseChronology.INSTANCE)
                .withDecimalStyle(DecimalStyle.STANDARD)
                .withResolverStyle(ResolverStyle.STRICT);
    }
}
