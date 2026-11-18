package io.github.sbracely.chinese.calendar;

import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.IntFunction;

/**
 * Simplified Chinese text maps for {@link java.time.format.DateTimeFormatterBuilder#appendText(TemporalField, Map)}.
 * 供原生日期格式化构建器使用的简体中文文本映射。
 * These maps target {@link ChineseDate} values, not Gregorian month and day values.
 * 这些映射用于农历日期字段，而非公历月、日字段。
 * Explicit text maps are not translated by changing a formatter's locale.
 * 显式文本映射不会随格式化器的语言环境变化而自动翻译。
 */
public final class ChineseDateText {
    private static final Map<Long, String> SEXAGENARY = textMap(60,
            value -> ChineseSexagenary.of(value).getDisplayName(Locale.SIMPLIFIED_CHINESE));
    private static final Map<Long, String> ZODIAC = textMap(12,
            value -> ChineseZodiac.of(value).getDisplayName(Locale.SIMPLIFIED_CHINESE));
    private static final Map<Long, String> MONTH = textMap(12,
            value -> ResourceBundle.getBundle("cldr.chinese-calendar", Locale.SIMPLIFIED_CHINESE)
                    .getString("month." + value));
    private static final Map<Long, String> DAY = textMap(30, ChineseDateText::dayName);
    private static final Map<Long, String> LEAP_MONTH = Map.of(0L, "", 1L, "闰");

    private ChineseDateText() {
    }

    /**
     * Gets an immutable text map for a supported field.
     * 获取受支持字段的不可变文本映射。
     * Supports sexagenary year/day, zodiac, month-of-year, day-of-month and leap-month flag.
     * 支持干支年、干支日、生肖、月份、月内日期及闰月标志。
     * Month names include the month suffix; year and day-cycle names have no suffix.
     * 月份文本包含“月”字，干支及生肖文本不附加“年”或“日”字。
     * The leap flag maps 0 to an empty string and 1 to the leap-month prefix.
     * 闰月标志将 0 映射为空串，1 映射为“闰”字。
     * <pre>{@code
     * DateTimeFormatter formatter = new DateTimeFormatterBuilder()
     *         .appendText(ChineseTemporalField.IS_LEAP_MONTH,
     *                 ChineseDateText.getTextMap(ChineseTemporalField.IS_LEAP_MONTH))
     *         .appendText(ChronoField.MONTH_OF_YEAR,
     *                 ChineseDateText.getTextMap(ChronoField.MONTH_OF_YEAR))
     *         .appendText(ChronoField.DAY_OF_MONTH,
     *                 ChineseDateText.getTextMap(ChronoField.DAY_OF_MONTH))
     *         .toFormatter(Locale.SIMPLIFIED_CHINESE)
     *         .withChronology(ChineseChronology.INSTANCE);
     * }</pre>
     *
     * @param field the field whose values will be printed
     *              要输出文本的字段
     * @return the shared Simplified Chinese text map
     *         共享的简体中文文本映射
     * @throws NullPointerException if field is {@code null}
     *                              如果字段为 {@code null}
     * @throws UnsupportedTemporalTypeException if no text map is provided for the field
     *                                         如果该字段没有文本映射
     */
    public static Map<Long, String> getTextMap(TemporalField field) {
        Objects.requireNonNull(field, "field");
        if (field == ChronoField.MONTH_OF_YEAR) {
            return MONTH;
        }
        if (field == ChronoField.DAY_OF_MONTH) {
            return DAY;
        }
        if (field instanceof ChineseTemporalField chineseField) {
            return switch (chineseField) {
                case SEXAGENARY_YEAR, SEXAGENARY_DAY -> SEXAGENARY;
                case ZODIAC -> ZODIAC;
                case IS_LEAP_MONTH -> LEAP_MONTH;
                case MONTH_AS_ORDINAL_OF_YEAR ->
                        throw new UnsupportedTemporalTypeException("No Chinese text map for field: " + field);
            };
        }
        throw new UnsupportedTemporalTypeException("No Chinese text map for field: " + field);
    }

    private static Map<Long, String> textMap(int count, IntFunction<String> names) {
        Map<Long, String> result = new HashMap<>();
        for (int value = 1; value <= count; value++) {
            result.put((long) value, names.apply(value));
        }
        return Map.copyOf(result);
    }

    private static String dayName(int day) {
        return switch (day) {
            case 10 -> "初十";
            case 20 -> "二十";
            case 30 -> "三十";
            default -> (day < 10 ? "初" : day < 20 ? "十" : "廿")
                    + "一二三四五六七八九".charAt(day % 10 - 1);
        };
    }
}
