package io.github.sbracely.chinese.calendar;

import java.time.format.DateTimeFormatterBuilder;
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
 * Text maps for {@link java.time.format.DateTimeFormatterBuilder#appendText(TemporalField, Map)}.
 * 供原生日期格式化构建器使用的文本映射。
 * These maps target {@link ChineseDate} values, not Gregorian month and day values.
 * 这些映射用于农历日期字段，而非公历月、日字段。
 * Explicit text maps are not translated by changing a formatter's locale.
 * 显式文本映射不会随格式化器的语言环境变化而自动翻译。
 * CLDR-backed cycle, zodiac and month names use the default locale at class initialization.
 * 来自 CLDR 的干支、生肖和月份名称使用本类初始化时的默认语言环境。
 */
public final class ChineseDateText {
    private static final Locale LOCALE = Locale.getDefault();
    private static final ResourceBundle CLDR = ResourceBundle.getBundle("cldr.chinese-calendar", LOCALE);
    private static final String LEAP_MONTH_PATTERN = CLDR.getString("month.leapPattern");
    private static final int LEAP_MONTH_PLACEHOLDER = leapMonthPlaceholder(LEAP_MONTH_PATTERN);
    private static final String LEAP_MONTH_PREFIX = LEAP_MONTH_PATTERN.substring(0, LEAP_MONTH_PLACEHOLDER);
    private static final String LEAP_MONTH_SUFFIX = LEAP_MONTH_PATTERN.substring(LEAP_MONTH_PLACEHOLDER + 3);
    private static final Map<Long, String> SEXAGENARY = textMap(60,
            value -> ChineseSexagenary.of(value).getDisplayName(LOCALE));
    private static final Map<Long, String> ZODIAC = textMap(12,
            value -> ChineseZodiac.of(value).getDisplayName(LOCALE));
    private static final Map<Long, String> MONTH = textMap(12, value -> CLDR.getString("month." + value));
    private static final Map<Long, String> DAY = textMap(30, ChineseDateText::dayName);
    private static final Map<Long, String> LEAP_MONTH_PREFIX_MAP = Map.of(0L, "", 1L, LEAP_MONTH_PREFIX);
    private static final Map<Long, String> LEAP_MONTH_SUFFIX_MAP = Map.of(0L, "", 1L, LEAP_MONTH_SUFFIX);

    private ChineseDateText() {
    }

    /**
     * Gets an immutable text map for a supported field.
     * 获取受支持字段的不可变文本映射。
     * Supports sexagenary year/day, zodiac, month-of-year, day-of-month and leap-month flag.
     * 支持干支年、干支日、生肖、月份、月内日期及闰月标志。
     * Month names include the month suffix; year and day-cycle names have no suffix.
     * 月份文本包含“月”字，干支及生肖文本不附加“年”或“日”字。
     * Add the leap-month prefix and suffix with {@link #appendLeapMonthPrefix(DateTimeFormatterBuilder)}
     * and {@link #appendLeapMonthSuffix(DateTimeFormatterBuilder)} around the month field.
     * 使用 {@link #appendLeapMonthPrefix(DateTimeFormatterBuilder)} 和
     * {@link #appendLeapMonthSuffix(DateTimeFormatterBuilder)} 在月份字段前后添加闰月模式文本。
     * <pre>{@code
     * DateTimeFormatterBuilder builder = ChineseDateText.appendLeapMonthPrefix(
     *         new DateTimeFormatterBuilder());
     * builder.appendText(ChronoField.MONTH_OF_YEAR,
     *         ChineseDateText.getTextMap(ChronoField.MONTH_OF_YEAR));
     * ChineseDateText.appendLeapMonthSuffix(builder)
     *         .appendText(ChronoField.DAY_OF_MONTH,
     *                 ChineseDateText.getTextMap(ChronoField.DAY_OF_MONTH));
     * DateTimeFormatter formatter = builder.toFormatter(Locale.getDefault())
     *         .withChronology(ChineseChronology.INSTANCE);
     * }</pre>
     *
     * @param field the field whose values will be printed
     *              要输出文本的字段
     * @return the shared text map, with CLDR-backed names resolved for the default locale at class initialization
     *         共享文本映射，其中 CLDR 名称按本类初始化时的默认语言环境解析
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
                case IS_LEAP_MONTH -> LEAP_MONTH_PREFIX_MAP;
                case MONTH_AS_ORDINAL_OF_YEAR ->
                        throw new UnsupportedTemporalTypeException("No Chinese text map for field: " + field);
            };
        }
        throw new UnsupportedTemporalTypeException("No Chinese text map for field: " + field);
    }

    /**
     * Appends the CLDR leap-month prefix when the pattern has one.
     * 如果 CLDR 闰月模式包含前缀，则追加此前缀。
     *
     * @param builder the formatter builder to append to
     *                要追加内容的格式化器构建器
     * @return the same formatter builder
     *         同一格式化器构建器
     * @throws NullPointerException if builder is {@code null}
     *                              如果 builder 为 {@code null}
     */
    public static DateTimeFormatterBuilder appendLeapMonthPrefix(DateTimeFormatterBuilder builder) {
        Objects.requireNonNull(builder, "builder");
        if (!LEAP_MONTH_PREFIX.isEmpty()) {
            builder.appendText(ChineseTemporalField.IS_LEAP_MONTH, LEAP_MONTH_PREFIX_MAP);
        }
        return builder;
    }

    /**
     * Appends the CLDR leap-month suffix when the pattern has one.
     * 如果 CLDR 闰月模式包含后缀，则追加此后缀。
     *
     * @param builder the formatter builder to append to
     *                要追加内容的格式化器构建器
     * @return the same formatter builder
     *         同一格式化器构建器
     * @throws NullPointerException if builder is {@code null}
     *                              如果 builder 为 {@code null}
     */
    public static DateTimeFormatterBuilder appendLeapMonthSuffix(DateTimeFormatterBuilder builder) {
        Objects.requireNonNull(builder, "builder");
        if (!LEAP_MONTH_SUFFIX.isEmpty()) {
            builder.appendText(ChineseTemporalField.IS_LEAP_MONTH, LEAP_MONTH_SUFFIX_MAP);
        }
        return builder;
    }

    private static int leapMonthPlaceholder(String pattern) {
        String placeholder = "{0}";
        int index = pattern.indexOf(placeholder);
        if (index < 0 || index != pattern.lastIndexOf(placeholder)
                || index == 0 && index + placeholder.length() == pattern.length()) {
            throw new IllegalStateException("Expected exactly one {0} in CLDR leap-month pattern: " + pattern);
        }
        return index;
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
