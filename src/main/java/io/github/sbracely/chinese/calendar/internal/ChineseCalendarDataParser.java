package io.github.sbracely.chinese.calendar.internal;

import io.github.sbracely.chinese.calendar.ChineseTemporalField;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ValueRange;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Parses and validates Chinese calendar configuration properties.
 * 解析并校验农历配置属性。
 */
public final class ChineseCalendarDataParser {

    /**
     * The configuration key for the chronology identifier.
     * 历法标识符的配置键。
     */
    private static final String KEY_ID = "id";
    /**
     * The configuration key for the calendar type.
     * 日历类型的配置键。
     */
    private static final String KEY_TYPE = "type";
    /**
     * The configuration key for the ISO date of the first supported lunar New Year.
     * 首个受支持农历年份正月初一所对应 ISO 日期的配置键。
     */
    private static final String KEY_ISO_START = "iso-start";
    /**
     * The prefix for keys containing each year's chronological month lengths.
     * 保存各年份按时间顺序排列的月份天数的配置键前缀。
     */
    private static final String KEY_MONTH_LENGTH_PREFIX = "month-length.";
    /**
     * The prefix marking a leap-month length after its regular month.
     * 标记同名普通月之后闰月天数的前缀。
     */
    private static final String LEAP_PREFIX = "leap";

    /**
     * Prevents instantiation of this static parser.
     * 禁止实例化此静态解析器。
     */
    private ChineseCalendarDataParser() {
    }

    /**
     * Validates the calendar identity, ISO start date, and consecutive positive lunar years,
     * then builds padded month-start data and its fingerprint.
     * Each year must contain twelve regular months and optionally one marked leap month;
     * each actual month must have 29 or 30 days.
     * Year entries are read from direct properties, not from the defaults chain.
     * 校验历法标识、ISO 起始日期及连续的正数农历年份，再构建含填充槽位的月初数据和指纹。
     * 每年必须包含十二个普通月，以及可选的一个带标记闰月；每个实际月份须为 29 或 30 天。
     * 年份条目读取直接属性，不读取默认属性链。
     *
     * @param properties the string configuration properties
     *                   字符串配置属性
     * @param expectedId the required chronology identifier
     *                   要求的历法标识符
     * @param expectedType the required calendar type
     *                     要求的日历类型
     * @return the parsed calendar data
     *         解析后的历法数据
     * @throws IllegalArgumentException if required entries are missing, mismatched, or malformed
     *                                  如果必需条目缺失、不匹配或格式错误
     * @throws java.time.DateTimeException if a year is outside the year-of-era field range
     *                                    如果年份超出纪元年份字段的范围
     * @throws ArithmeticException if epoch-day or table-index calculations overflow
     *                             如果纪元天数或表索引计算溢出
     * @throws NullPointerException if a required argument is {@code null}
     *                              如果必需参数为 {@code null}
     * @throws ClassCastException if direct property keys or year-entry values are not strings
     *                            如果直接属性的键或年份条目的值不是字符串
     */
    public static ChineseCalendarData parse(Properties properties, String expectedId, String expectedType) {
        String id = properties.getProperty(KEY_ID);
        if (!expectedId.equals(id)) {
            throw new IllegalArgumentException("Configuration is for a different calendar: " + id);
        }
        String type = properties.getProperty(KEY_TYPE);
        if (!expectedType.equals(type)) {
            throw new IllegalArgumentException("Configuration is for a different calendar type: " + type);
        }
        String isoStartValue = properties.getProperty(KEY_ISO_START);
        if (isoStartValue == null || isoStartValue.isEmpty()) {
            throw new IllegalArgumentException("Configuration does not contain a ISO start date");
        }
        int minimumEpochDay;
        try {
            minimumEpochDay = Math.toIntExact(LocalDate.parse(
                    isoStartValue, DateTimeFormatter.ISO_LOCAL_DATE).toEpochDay());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid ISO start date, expected format yyyy-MM-dd", e);
        }

        Map<Integer, int[]> monthsByYear = new TreeMap<>();
        Map<Integer, Integer> leapMonthByYear = new HashMap<>();
        int minimumYear = Integer.MAX_VALUE;
        int maximumYear = Integer.MIN_VALUE;
        int maximumMonthsInYear = Integer.MIN_VALUE;
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            if (!key.startsWith(KEY_MONTH_LENGTH_PREFIX)) {
                continue;
            }
            int year;
            try {
                year = Integer.parseInt(key.substring(KEY_MONTH_LENGTH_PREFIX.length()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("bad key: " + key, e);
            }
            ChronoField.YEAR_OF_ERA.checkValidValue(year);
            ParsedMonths parsedMonths = parseMonthLengths(year, (String) entry.getValue());
            minimumYear = Math.min(minimumYear, year);
            maximumYear = Math.max(maximumYear, year);
            maximumMonthsInYear = Math.max(maximumMonthsInYear, parsedMonths.monthLengths.length);
            if (parsedMonths.leapMonth != 0) {
                leapMonthByYear.put(year, parsedMonths.leapMonth);
            }
            if (monthsByYear.put(year, parsedMonths.monthLengths) != null) {
                throw new IllegalArgumentException("Duplicate calendar year: " + year);
            }
        }

        if (monthsByYear.isEmpty()
                || (long) maximumYear - minimumYear + 1 != monthsByYear.size()) {
            throw new IllegalArgumentException("Configuration must contain consecutive calendar years");
        }
        int epochMonthOffset = Math.multiplyExact(minimumYear, maximumMonthsInYear);
        Math.multiplyExact(Math.addExact(maximumYear, 1), maximumMonthsInYear);
        MonthStartData monthStartData = createMonthStartEpochDays(
                monthsByYear.values(), minimumEpochDay, maximumMonthsInYear);
        int[] monthStartEpochDays = monthStartData.monthStartEpochDays;
        int maximumEpochDay = monthStartEpochDays[monthStartEpochDays.length - 1] - 1;
        return new ChineseCalendarData(monthStartEpochDays, minimumEpochDay, maximumEpochDay, epochMonthOffset,
                monthStartData.minimumDaysInMonth, monthStartData.maximumDaysInMonth,
                maximumMonthsInYear, monthStartData.minimumDaysInYear, monthStartData.maximumDaysInYear,
                leapMonthByYear, minimumYear, maximumYear,
                dataFingerprint(minimumEpochDay, monthsByYear, leapMonthByYear));
    }

    /**
     * Parses whitespace-separated month lengths and the optional {@code leap} prefix.
     * The zero-based position of a leap entry equals its repeated one-based month number.
     * 解析以空白分隔的月份天数及可选的 {@code leap} 前缀。
     * 闰月条目从零开始的位置，等于其重复的从一开始计数的月份编号。
     *
     * @param year the year used in validation messages
     *             校验消息中使用的年份
     * @param monthLengthConfigLine the chronological month-length configuration
     *                              按时间顺序排列的月份天数配置
     * @return the month lengths and leap-month number, or zero for a common year
     *         月份天数及闰月编号，平年的闰月编号为零
     * @throws IllegalArgumentException if month counts, leap placement, or lengths are invalid
     *                                  如果月份数、闰月位置或天数无效
     */
    private static ParsedMonths parseMonthLengths(int year, String monthLengthConfigLine) {
        String[] monthLengthStrings = monthLengthConfigLine.strip().split("\\s+");
        int leapMonthCount = (int) Arrays.stream(monthLengthStrings)
                .filter(monthLength -> monthLength.startsWith(LEAP_PREFIX)).count();
        int monthCount = monthLengthStrings.length;
        ValueRange monthRange = ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR.range();
        boolean validCommonYear = monthCount == monthRange.getSmallestMaximum() && leapMonthCount == 0;
        boolean validLeapYear = monthCount == monthRange.getMaximum() && leapMonthCount == 1;
        if (!validCommonYear && !validLeapYear) {
            throw new IllegalArgumentException("Invalid month length config year: " + year
                    + ", line: " + monthLengthConfigLine);
        }

        int[] monthLengths = new int[monthCount];
        int leapMonth = 0;
        for (int i = 0; i < monthCount; i++) {
            String monthLength = monthLengthStrings[i];
            String numericMonthLength = monthLength;
            if (numericMonthLength.startsWith(LEAP_PREFIX)) {
                if (i == 0) {
                    throw new IllegalArgumentException("Leap month must follow a regular month in year: " + year);
                }
                numericMonthLength = numericMonthLength.substring(LEAP_PREFIX.length());
                leapMonth = i;
            }
            try {
                monthLengths[i] = Integer.parseInt(numericMonthLength);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("bad month length: " + monthLength + " in year: " + year, e);
            }
            if (monthLengths[i] != 29 && monthLengths[i] != 30) {
                throw new IllegalArgumentException("Invalid month length: " + monthLength + " in year: " + year);
            }
        }
        return new ParsedMonths(monthLengths, leapMonth);
    }

    /**
     * Accumulates month starts and actual month/year length bounds in chronological year order.
     * Pads short years with the following year's start and appends an exclusive final bound.
     * 按年份时间顺序累积月初日期及实际月份、年份天数的上下界。
     * 月份较少的年份以次年起始日填充，最后追加不包含在支持范围内的结束边界。
     *
     * @param daysOfMonthsCollection the month lengths for each year in chronological order
     *                               按年份时间顺序排列的各年月份天数
     * @param minimumEpochDay the epoch day of the first year's start
     *                        首年起始日的纪元天数
     * @param maximumMonthsInYear the fixed number of table slots per year
     *                            每年固定的表槽位数
     * @return the padded month-start table and actual length bounds
     *         含填充槽位的月初表及实际天数上下界
     * @throws ArithmeticException if the table size or epoch days overflow
     *                             如果表大小或纪元天数溢出
     * @throws IllegalArgumentException if a month length is negative
     *                                  如果月份天数为负数
     * @throws IllegalStateException if the number of filled entries differs from the allocated size
     *                               如果填充条目数与分配的表大小不一致
     */
    private static MonthStartData createMonthStartEpochDays(Collection<int[]> daysOfMonthsCollection,
                                                            int minimumEpochDay, int maximumMonthsInYear) {
        int[] monthStartEpochDays = new int[Math.addExact(
                Math.multiplyExact(daysOfMonthsCollection.size(), maximumMonthsInYear), 1)];
        int minimumDaysInMonth = Integer.MAX_VALUE;
        int maximumDaysInMonth = Integer.MIN_VALUE;
        int minimumDaysInYear = Integer.MAX_VALUE;
        int maximumDaysInYear = Integer.MIN_VALUE;
        int index = 0;
        for (int[] daysOfMonths : daysOfMonthsCollection) {
            int daysInYear = 0;
            for (int dayOfMonth : daysOfMonths) {
                if (dayOfMonth < 0) {
                    throw new IllegalArgumentException("Invalid month length: " + dayOfMonth);
                }
                monthStartEpochDays[index++] = minimumEpochDay;
                minimumEpochDay = Math.addExact(minimumEpochDay, dayOfMonth);
                daysInYear += dayOfMonth;
                minimumDaysInMonth = Math.min(minimumDaysInMonth, dayOfMonth);
                maximumDaysInMonth = Math.max(maximumDaysInMonth, dayOfMonth);
            }
            minimumDaysInYear = Math.min(minimumDaysInYear, daysInYear);
            maximumDaysInYear = Math.max(maximumDaysInYear, daysInYear);
            for (int i = daysOfMonths.length; i < maximumMonthsInYear; i++) {
                monthStartEpochDays[index++] = minimumEpochDay;
            }
        }
        monthStartEpochDays[index++] = minimumEpochDay;
        if (index != monthStartEpochDays.length) {
            throw new IllegalStateException("Did not fill epochMonths exactly: ndx = " + index
                    + " should be " + monthStartEpochDays.length);
        }
        return new MonthStartData(monthStartEpochDays, minimumDaysInMonth,
                maximumDaysInMonth, minimumDaysInYear, maximumDaysInYear);
    }

    /**
     * Hashes the start day and ordered year/month data as UTF-8 text using SHA-256.
     * Metadata and source descriptions do not participate in the fingerprint.
     * 将起始日和有序的年份、月份数据编码为 UTF-8 文本，再计算 SHA-256。
     * 元数据和来源描述不参与指纹计算。
     *
     * @param minimumEpochDay the first supported epoch day
     *                        支持的最早纪元天数
     * @param months the chronological year-to-month-length mapping
     *               按年份时间顺序排列的年份到月份天数映射
     * @param leapMonths the year-to-leap-month-number mapping
     *                   年份到闰月编号的映射
     * @return the lowercase hexadecimal SHA-256 fingerprint
     *         小写十六进制 SHA-256 指纹
     */
    private static String dataFingerprint(int minimumEpochDay, Map<Integer, int[]> months,
                                          Map<Integer, Integer> leapMonths) {
        StringBuilder content = new StringBuilder().append(minimumEpochDay);
        months.forEach((year, lengths) -> content.append('|').append(year).append(':')
                .append(Arrays.toString(lengths)).append(':').append(leapMonths.getOrDefault(year, 0)));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * The validated month lengths and leap-month number for one year.
     * 单个年份已校验的月份天数及闰月编号。
     *
     * @param monthLengths the chronological actual month lengths
     *                     按时间顺序排列的实际月份天数
     * @param leapMonth the repeated month number, or zero for a common year
     *                  重复的月份编号，平年为零
     */
    private record ParsedMonths(int[] monthLengths, int leapMonth) {
    }

    /**
     * The padded month-start table and length bounds derived from actual months and years.
     * 含填充槽位的月初表及根据实际月份和年份计算的天数上下界。
     *
     * @param monthStartEpochDays the padded starts with an exclusive final bound
     *                            含填充槽位和不包含在支持范围内的最终边界的起始日表
     * @param minimumDaysInMonth the shortest actual month
     *                           实际月份的最少天数
     * @param maximumDaysInMonth the longest actual month
     *                           实际月份的最多天数
     * @param minimumDaysInYear the shortest year
     *                          年份的最少天数
     * @param maximumDaysInYear the longest year
     *                          年份的最多天数
     */
    private record MonthStartData(int[] monthStartEpochDays, int minimumDaysInMonth, int maximumDaysInMonth,
                                  int minimumDaysInYear, int maximumDaysInYear) {
    }
}
