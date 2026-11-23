package io.github.sbracely.chinese.calendar;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DecimalStyle;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.util.Locale;
import java.util.ResourceBundle;

import static io.github.sbracely.chinese.calendar.ChineseTemporalField.*;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;

/**
 * Immutable, thread-safe formatters for four lunar-date representations with locale-specific CLDR text.
 * 四种使用区域化 CLDR 文本表示农历日期的不可变、线程安全格式化器。
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
 * CLDR-backed text maps, calendar name and related-year era text use the default locale at initialization.
 * CLDR 文本映射、历法名称和公历年预设的纪元文本使用类初始化时的默认语言环境。
 * Chinese literals and day names remain unchanged when the formatter locale changes.
 * 格式化器的语言环境变化时，中文固定文字和日名不会随之变化。
 * Changing the default locale or using {@code withLocale} later does not change them.
 * 之后更改默认语言环境或调用 {@code withLocale} 不会改变这些文本。
 */
public final class ChineseDateFormatters {

    private static final ResourceBundle CLDR = ResourceBundle.getBundle("cldr.chinese-calendar");

    private ChineseDateFormatters() {
    }

    /**
     * Sexagenary year, numbered lunar month and day, such as 农历乙未年正月初一 with a Chinese default locale.
     * 干支纪年、数序纪月、数序纪日，例如默认语言环境为中文时“农历乙未年正月初一”。
     */
    public static final DateTimeFormatter SEXAGENARY_YEAR_DATE =
            ChineseDateText.appendLunarMonth(new DateTimeFormatterBuilder()
                    .appendLiteral(CLDR.getString("name"))
                    .appendText(SEXAGENARY_YEAR, ChineseDateText.getTextMap(SEXAGENARY_YEAR))
                    .appendLiteral('年'))
                    .appendText(DAY_OF_MONTH, ChineseDateText.getTextMap(DAY_OF_MONTH))
                    .toFormatter(Locale.SIMPLIFIED_CHINESE)
                    .withChronology(ChineseChronology.INSTANCE)
                    .withDecimalStyle(DecimalStyle.STANDARD)
                    .withResolverStyle(ResolverStyle.STRICT);

    /**
     * Zodiac year, numbered lunar month and day, such as 农历牛年闰五月十一 with a Chinese default locale.
     * 生肖纪年、数序纪月、数序纪日，例如默认语言环境为中文时“农历牛年闰五月十一”。
     */
    public static final DateTimeFormatter ZODIAC_YEAR_DATE =
            ChineseDateText.appendLunarMonth(new DateTimeFormatterBuilder()
                    .appendLiteral(CLDR.getString("name"))
                    .appendText(ZODIAC, ChineseDateText.getTextMap(ZODIAC))
                    .appendLiteral('年'))
                    .appendText(DAY_OF_MONTH, ChineseDateText.getTextMap(DAY_OF_MONTH))
                    .toFormatter(Locale.SIMPLIFIED_CHINESE)
                    .withChronology(ChineseChronology.INSTANCE)
                    .withDecimalStyle(DecimalStyle.STANDARD)
                    .withResolverStyle(ResolverStyle.STRICT);

    /**
     * Sexagenary year and day with a numbered lunar month, such as 农历甲午年七月庚戌日 with a Chinese default locale.
     * 干支纪年、数序纪月、干支纪日，例如默认语言环境为中文时“农历甲午年七月庚戌日”。
     */
    public static final DateTimeFormatter SEXAGENARY_YEAR_DAY_DATE =
            ChineseDateText.appendLunarMonth(new DateTimeFormatterBuilder()
                    .appendLiteral(CLDR.getString("name"))
                    .appendText(SEXAGENARY_YEAR, ChineseDateText.getTextMap(SEXAGENARY_YEAR))
                    .appendLiteral('年'))
                    .appendText(SEXAGENARY_DAY, ChineseDateText.getTextMap(SEXAGENARY_DAY))
                    .appendLiteral('日')
                    .toFormatter(Locale.SIMPLIFIED_CHINESE)
                    .withChronology(ChineseChronology.INSTANCE)
                    .withDecimalStyle(DecimalStyle.STANDARD)
                    .withResolverStyle(ResolverStyle.STRICT);

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
            ChineseDateText.appendLunarMonth(new DateTimeFormatterBuilder()
                    .appendLiteral(CLDR.getString("era.1"))
                    .appendValue(ChronoField.YEAR)
                    .appendLiteral('年')
                    .appendLiteral(CLDR.getString("name"))
                    .appendText(SEXAGENARY_YEAR, ChineseDateText.getTextMap(SEXAGENARY_YEAR))
                    .appendLiteral('年'))
                    .appendText(DAY_OF_MONTH, ChineseDateText.getTextMap(DAY_OF_MONTH))
                    .toFormatter(Locale.SIMPLIFIED_CHINESE)
                    .withChronology(ChineseChronology.INSTANCE)
                    .withDecimalStyle(DecimalStyle.STANDARD)
                    .withResolverStyle(ResolverStyle.STRICT);
}
