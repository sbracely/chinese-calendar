package io.github.sbracely.chinese.calendar;

import java.io.*;
import java.time.*;
import java.time.chrono.AbstractChronology;
import java.time.chrono.ChronoLocalDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.time.chrono.Era;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalField;
import java.time.temporal.ValueRange;
import java.util.*;

import static io.github.sbracely.chinese.calendar.ChineseEra.CHINESE_ERA;

/**
 * The Chinese calendar system.
 * 农历历法系统
 */
public final class ChineseChronology extends AbstractChronology implements Serializable {

    /**
     * The Chinese calendar id.
     * 农历历法 id
     */
    private final transient String typeId;

    /**
     * The Chinese calendar type.
     * 农历历法类型
     */
    private final transient String calendarType;

    /**
     * The calendar data loaded from the configuration file.
     * 配置文件中的日历数据
     */
    private transient volatile CalendarData calendarData;

    private static final String RESOURCE_BUNDLE_BASE_NAME = "cldr.chinese-calendar";
    private static final String RESOURCE_BUNDLE_CALENDAR_NAME = "name";

    /**
     * The calendar data for the ChineseChronology.
     * 农历历法的日历数据
     *
     * @param monthStartEpochDays the epoch day of the first day of each month
     *                            每个月初一对应的 epoch day
     * @param minimumEpochDay     the earliest supported epoch day
     *                            支持的最早 epoch day
     * @param maximumEpochDay     the latest supported epoch day
     *                            支持的最晚 epoch day
     * @param epochMonthOffset    the offset used to map epoch months to array indices
     *                            将 epoch 月份映射到数组索引时使用的偏移量
     * @param minimumDaysInMonth  the minimum number of days in a month
     *                            月份的最少天数
     * @param maximumDaysInMonth  the maximum number of days in a month
     *                            月份的最多天数
     * @param maximumMonthsInYear the maximum number of months in a year
     *                            一年的最多月份数
     * @param minimumDaysInYear   the minimum number of days in a year
     *                            年份的最少天数
     * @param maximumDaysInYear   the maximum number of days in a year
     *                            年份的最多天数
     * @param leapMonthByYear     the leap month number for each leap year
     *                            每个闰年的闰月月份
     * @param minimumYear         the earliest supported proleptic year
     *                            支持的最早历法年份
     * @param maximumYear         the latest supported proleptic year
     *                            支持的最晚历法年份
     */
    private record CalendarData(int[] monthStartEpochDays, int minimumEpochDay, int maximumEpochDay,
                                int epochMonthOffset, int minimumDaysInMonth, int maximumDaysInMonth,
                                int maximumMonthsInYear, int minimumDaysInYear, int maximumDaysInYear,
                                Map<Integer, Integer> leapMonthByYear, int minimumYear, int maximumYear) {
        private CalendarData {
            monthStartEpochDays = monthStartEpochDays.clone();
            leapMonthByYear = Map.copyOf(leapMonthByYear);
        }
    }

    /**
     * The singleton instance of the ChineseChronology.
     * 单例实例
     */
    public static final ChineseChronology INSTANCE;

    static {
        INSTANCE = new ChineseChronology("Chinese", "chinese");
    }

    /**
     * Create a ChineseChronology for the named id and type.
     * 创建一个指定id和类型的 ChineseChronology 实例
     *
     * @param id           the id of the calendar
     *                     日历的 id
     * @param calendarType the typeId of the calendar
     *                     日历的类型 id
     * @throws IllegalArgumentException if the id or typeId is empty
     *                                  如果 id 或 typeId 为空，则抛出此异常
     */
    private ChineseChronology(String id, String calendarType) {
        if (null == id || id.isEmpty()) {
            throw new IllegalArgumentException("calendar id is empty");
        }
        if (null == calendarType || calendarType.isEmpty()) {
            throw new IllegalArgumentException("calendar typeId is empty");
        }
        this.typeId = id;
        this.calendarType = calendarType;
    }

    //-----------------------------------------------------------------------
    // Public methods
    // 公开方法
    //-----------------------------------------------------------------------

    /**
     * Create a ChineseDate from the given era, year-of-era, month, leap month flag, and day-of-month.
     * 根据给定的纪元、年、月、闰月标志和日创建一个 ChineseDate
     *
     * @param era        the era of the correct type for the chronology
     *                   纪元
     * @param yearOfEra  the chronology year-of-era
     *                   年
     * @param month      the chronology month-of-year
     *                   月份
     * @param leapMonth  the leap month flag, true if the month is a leap month, false otherwise
     *                   闰月标志，如果月份是闰月，则为 true，否则为 false
     * @param dayOfMonth the chronology day-of-month
     *                   日
     * @return the ChineseDate
     * 农历对象
     */
    public ChineseDate date(Era era, int yearOfEra, int month, boolean leapMonth, int dayOfMonth) {
        return date(prolepticYear(era, yearOfEra), month, leapMonth, dayOfMonth);
    }

    /**
     * Create a ChineseDate from the given year, month, leap month flag, and day-of-month.
     * 根据给定的年、月、闰月标志和日创建一个 ChineseDate
     *
     * @param prolepticYear the proleptic year
     *                      年
     * @param month         the month-of-year
     *                      月份
     * @param leapMonth     the leap month flag, true if the month is a leap month, false otherwise
     *                      闰月标志，如果月份是闰月，则为 true，否则为 false
     * @param dayOfMonth    the day-of-month
     *                      日
     * @return the ChineseDate
     * 农历对象
     */
    public ChineseDate date(int prolepticYear, int month, boolean leapMonth, int dayOfMonth) {
        return ChineseDate.of(this, prolepticYear, month, leapMonth, dayOfMonth);
    }

    /**
     * Calculates the epoch-second from the given era date and time, including the leap-month flag.
     * 根据给定的年号日期（含闰月标志）和时间计算 epochSecond
     *
     * @param era           the era
     * @param yearOfEra     the chronology year-of-era
     * @param month         the month-of-year
     * @param leapMonth     whether the month is a leap month
     * @param dayOfMonth    the day-of-month
     * @param hour          the hour-of-day
     * @param minute        the minute-of-hour
     * @param second        the second-of-minute
     * @param zoneOffset    the zone offset
     * @return the epoch-second
     */
    public long epochSecond(Era era, int yearOfEra, int month, boolean leapMonth, int dayOfMonth,
                            int hour, int minute, int second, ZoneOffset zoneOffset) {
        Objects.requireNonNull(era, "era");
        return epochSecond(prolepticYear(era, yearOfEra), month, leapMonth, dayOfMonth,
                hour, minute, second, zoneOffset);
    }

    /**
     * Calculates the epoch-second from the given date and time, including the leap-month flag.
     * 根据给定的日期（含闰月标志）和时间计算 epochSecond
     *
     * @param prolepticYear the proleptic year
     * @param month         the month-of-year
     * @param leapMonth     whether the month is a leap month
     * @param dayOfMonth    the day-of-month
     * @param hour          the hour-of-day
     * @param minute        the minute-of-hour
     * @param second        the second-of-minute
     * @param zoneOffset    the zone offset
     * @return the epoch-second
     */
    public long epochSecond(int prolepticYear, int month, boolean leapMonth, int dayOfMonth,
                            int hour, int minute, int second, ZoneOffset zoneOffset) {
        return epochSecond(date(prolepticYear, month, leapMonth, dayOfMonth),
                hour, minute, second, zoneOffset);
    }

    /**
     * Calculates the epoch-second from the given date and time.
     * 根据给定的日期和时间计算 epochSecond
     *
     * @param date       the Chinese date
     *                   农历日期
     * @param hour       the hour-of-day
     *                   小时
     * @param minute     the minute-of-hour
     *                   分钟
     * @param second     the second-of-minute
     *                   秒
     * @param zoneOffset the zone offset
     *                   时区偏移
     * @return the epoch-second
     *         epoch 秒
     */
    public long epochSecond(ChineseDate date, int hour, int minute, int second, ZoneOffset zoneOffset) {
        Objects.requireNonNull(zoneOffset, "zoneOffset");
        ChronoField.HOUR_OF_DAY.checkValidValue(hour);
        ChronoField.MINUTE_OF_HOUR.checkValidValue(minute);
        ChronoField.SECOND_OF_MINUTE.checkValidValue(second);

        long daysInSec = Math.multiplyExact(date.toEpochDay(), 86400);
        long timeInSec = (hour * 60L + minute) * 60 + second;
        return Math.addExact(daysInSec, timeInSec - zoneOffset.getTotalSeconds());
    }

    /**
     * Get the leap month for the given year.
     * 通过年份获取闰月月份
     *
     * @param prolepticYear the proleptic year
     *                      年
     * @return the leap month, or 0 if there is no leap month
     * 闰月月份，如果没有闰月则为 0
     */
    public int getLeapMonth(long prolepticYear) {
        return calendarData().leapMonthByYear.getOrDefault((int) prolepticYear, 0);
    }

    /**
     * Get the range of the specified field.
     * 获取指定字段的范围
     *
     * @param field the field to get the range for
     *              要获取范围的字段
     * @return the range of the field
     * 字段的范围
     */
    public ValueRange range(ChineseTemporalField field) {
        CalendarData data = calendarData();
        return switch (field) {
            case IS_LEAP_MONTH -> ValueRange.of(0, data.leapMonthByYear.isEmpty() ? 0 : 1);
            case MONTH_AS_ORDINAL_OF_YEAR ->
                    ValueRange.of(1, field.range().getSmallestMaximum(), data.maximumMonthsInYear);
        };
    }

    //-----------------------------------------------------------------------
    // Override methods from Chronology
    // 重写 Chronology 中的方法
    //-----------------------------------------------------------------------

    @Override
    public String getId() {
        return typeId;
    }

    @Override
    public String getCalendarType() {
        return calendarType;
    }

    @Override
    public ChineseDate date(Era era, int yearOfEra, int month, int dayOfMonth) {
        return date(prolepticYear(era, yearOfEra), month, dayOfMonth);
    }

    @Override
    public ChineseDate date(int prolepticYear, int month, int dayOfMonth) {
        return ChineseDate.of(this, prolepticYear, month, dayOfMonth);
    }

    @Override
    public ChineseDate dateYearDay(Era era, int yearOfEra, int dayOfYear) {
        return dateYearDay(prolepticYear(era, yearOfEra), dayOfYear);
    }

    @Override
    public ChineseDate dateYearDay(int prolepticYear, int dayOfYear) {
        ChineseDate date = ChineseDate.of(this, prolepticYear, 1, 1);
        if (dayOfYear < 1 || dayOfYear > date.lengthOfYear()) {
            throw new DateTimeException("Invalid day of year: " + dayOfYear);
        }
        return date.plusDays(dayOfYear - 1);
    }

    @Override
    public ChineseDate dateEpochDay(long epochDay) {
        return ChineseDate.ofEpochDay(this, epochDay);
    }

    @Override
    public ChineseDate dateNow() {
        return dateNow(Clock.systemDefaultZone());
    }

    @Override
    public ChineseDate dateNow(ZoneId zone) {
        return dateNow(Clock.system(zone));
    }

    @Override
    public ChineseDate dateNow(Clock clock) {
        return dateEpochDay(LocalDate.now(clock).toEpochDay());
    }

    @Override
    public ChineseDate date(TemporalAccessor temporal) {
        if (temporal instanceof ChineseDate chineseDate) {
            return chineseDate;
        }
        return dateEpochDay(temporal.getLong(ChronoField.EPOCH_DAY));
    }

    @Override
    @SuppressWarnings("unchecked")
    public ChronoLocalDateTime<ChineseDate> localDateTime(TemporalAccessor temporal) {
        return (ChronoLocalDateTime<ChineseDate>) super.localDateTime(temporal);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ChronoZonedDateTime<ChineseDate> zonedDateTime(TemporalAccessor temporal) {
        return (ChronoZonedDateTime<ChineseDate>) super.zonedDateTime(temporal);
    }

    @Override
    @SuppressWarnings("unchecked")
    public ChronoZonedDateTime<ChineseDate> zonedDateTime(Instant instant, ZoneId zone) {
        return (ChronoZonedDateTime<ChineseDate>) super.zonedDateTime(instant, zone);
    }

    @Override
    public boolean isLeapYear(long prolepticYear) {
        return calendarData().leapMonthByYear.containsKey((int) prolepticYear);
    }

    @Override
    public int prolepticYear(Era era, int yearOfEra) {
        if (era != CHINESE_ERA) {
            throw new ClassCastException("Era must be ChineseEra");
        }
        return yearOfEra;
    }

    @Override
    public Era eraOf(int eraValue) {
        return ChineseEra.of(eraValue);
    }

    @Override
    public List<Era> eras() {
        return List.of(ChineseEra.values());
    }

    @Override
    public ValueRange range(ChronoField field) {
        CalendarData data = calendarData();
        return switch (field) {
            case DAY_OF_MONTH -> ValueRange.of(1, data.minimumDaysInMonth, data.maximumDaysInMonth);
            case DAY_OF_YEAR -> ValueRange.of(1, data.minimumDaysInYear, data.maximumDaysInYear);
            case YEAR, YEAR_OF_ERA -> ValueRange.of(data.minimumYear, data.maximumYear);
            case ERA -> ValueRange.of(CHINESE_ERA.getValue(), CHINESE_ERA.getValue());
            default -> field.range();
        };
    }

    @Override
    public String getDisplayName(TextStyle style, Locale locale) {
        String displayName = super.getDisplayName(style, locale);
        if (!getId().equals(displayName)) {
            return displayName;
        }
        ResourceBundle bundle = ResourceBundle.getBundle(RESOURCE_BUNDLE_BASE_NAME, locale);
        return bundle.getString(RESOURCE_BUNDLE_CALENDAR_NAME);
    }

    @Override
    public ChinesePeriod period(int years, int months, int days) {
        return new ChinesePeriod(this, years, months, days);
    }

    //-----------------------------------------------------------------------
    // Override methods from AbstractChronology
    // 重写 AbstractChronology 中的方法
    //-----------------------------------------------------------------------

    @Override
    public ChineseDate resolveDate(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle) {
        return (ChineseDate) super.resolveDate(fieldValues, resolverStyle);
    }

    //-----------------------------------------------------------------------
    // Initialize calendar data
    // 初始化日历数据
    //-----------------------------------------------------------------------

    /**
     * Gets the calendar data, loading it from the configuration file if necessary.
     * 获取日历数据；如果尚未加载，则从配置文件中加载。
     *
     * @return the calendar data for this chronology
     * 此历法的日历数据
     */
    private CalendarData calendarData() {
        CalendarData data = calendarData;
        if (data == null) {
            data = loadCalendarData(typeId, calendarType);
            calendarData = data;
        }
        return data;
    }

    /**
     * The keys used in the configuration file for the Chinese calendar.
     * 配置文件中用于农历历法的键
     */
    private static final String KEY_ID = "id";

    /**
     * The key used in the configuration file for the type of the Chinese calendar.
     * 配置文件中用于农历历法类型的键
     */
    private static final String KEY_TYPE = "type";

    /**
     * The key used in the configuration file for the version of the Chinese calendar.
     * 配置文件中用于农历历法版本的键
     */
    private static final String KEY_VERSION = "version";

    /**
     * The key used in the configuration file for the ISO start date of the Chinese calendar.
     * 配置文件中用于农历历法 ISO 起始日期的键
     */
    private static final String KEY_ISO_START = "iso-start";

    /**
     * The prefix used in the configuration file for the month lengths of the Chinese calendar.
     * 配置文件中用于农历历法月份长度的前缀
     */
    private static final String KEY_MONTH_LENGTH_PREFIX = "month-length.";

    /**
     * Load the calendar data from the configuration file.
     * 加载配置文件中的日历数据
     *
     * @param typeId       the id of the calendar
     *                     日历的 id
     * @param calendarType the typeId of the calendar
     *                     日历的类型 id
     * @return the loaded CalendarData
     * 加载的 CalendarData
     */
    private static CalendarData loadCalendarData(String typeId, String calendarType) {
        try {
            Properties props = readConfigProperties(typeId, calendarType);
            String id = props.getProperty(KEY_ID);
            if (!typeId.equals(id)) {
                throw new IllegalArgumentException("Configuration is for a different calendar: " + id);
            }
            String type = props.getProperty(KEY_TYPE);
            if (!calendarType.equals(type)) {
                throw new IllegalArgumentException("Configuration is for a different calendar type: " + type);
            }
            String version = props.getProperty(KEY_VERSION);
            if (version == null || version.isEmpty()) {
                throw new IllegalArgumentException("Configuration does not contain a version");
            }

            String isoStartValue = props.getProperty(KEY_ISO_START);
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

            Map<Integer, int[]> daysOfMonthsByYear = new TreeMap<>();
            Map<Integer, Integer> leapMonthByYear = new HashMap<>();
            int minimumYear = Integer.MAX_VALUE;
            int maximumYear = Integer.MIN_VALUE;
            int maximumMonthsInYear = Integer.MIN_VALUE;
            int monthLengthPrefixLength = KEY_MONTH_LENGTH_PREFIX.length();
            for (Map.Entry<Object, Object> entries : props.entrySet()) {
                String key = (String) entries.getKey();
                if (!key.startsWith(KEY_MONTH_LENGTH_PREFIX)) {
                    continue;
                }
                int year;
                try {
                    year = Integer.parseInt(key.substring(monthLengthPrefixLength));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("bad key: " + key, e);
                }
                ParsedMonths parsedMonths = parseMonthLengths(year, (String) entries.getValue());
                minimumYear = Math.min(minimumYear, year);
                maximumYear = Math.max(maximumYear, year);
                maximumMonthsInYear = Math.max(maximumMonthsInYear, parsedMonths.monthLengths.length);
                if (parsedMonths.leapMonth != 0) {
                    leapMonthByYear.put(year, parsedMonths.leapMonth);
                }
                daysOfMonthsByYear.put(year, parsedMonths.monthLengths);
            }

            int epochMonthOffset = minimumYear * maximumMonthsInYear;
            MonthStartData monthStartData = createMonthStartEpochDays(
                    daysOfMonthsByYear.values(), minimumEpochDay, maximumMonthsInYear);
            int[] monthStartEpochDays = monthStartData.monthStartEpochDays;
            int maximumEpochDay = monthStartEpochDays[monthStartEpochDays.length - 1] - 1;
            return new CalendarData(monthStartEpochDays, minimumEpochDay, maximumEpochDay,
                    epochMonthOffset, monthStartData.minimumDaysInMonth,
                    monthStartData.maximumDaysInMonth, maximumMonthsInYear,
                    monthStartData.minimumDaysInYear, monthStartData.maximumDaysInYear,
                    leapMonthByYear, minimumYear, maximumYear);
        } catch (Exception e) {
            throw new DateTimeException("Unable to initialize ChineseCalendar: " + typeId + "-" + calendarType, e);
        }
    }

    /**
     * The prefix for the resource file name containing the Chinese calendar configuration.
     * 包含农历历法配置的资源文件名的前缀
     */
    private static final String RESOURCE_PREFIX = "chinese-calendar-config-";

    /**
     * The suffix for the resource file name containing the Chinese calendar configuration.
     * 包含农历历法配置的资源文件名的后缀
     */
    private static final String RESOURCE_SUFFIX = ".properties";

    /**
     * Read the configuration properties for the Chinese calendar from the resource file.
     * 从资源文件中读取农历历法的配置属性
     *
     * @param chronologyId the id of the chronology
     *                     历法的 id
     * @param calendarType the type of the calendar
     *                     日历的类型
     * @return the properties loaded from the resource file
     * 从资源文件中加载的属性
     */
    private static Properties readConfigProperties(final String chronologyId, final String calendarType) {
        String resourceName = RESOURCE_PREFIX + chronologyId + "-" + calendarType + RESOURCE_SUFFIX;
        try (InputStream is = ChineseChronology.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (null == is) {
                throw new IllegalArgumentException("Config resource not found: " + resourceName);
            }
            Properties properties = new Properties();
            properties.load(is);
            return properties;
        } catch (Exception e) {
            throw new DateTimeException("Error reading config resource: " + resourceName, e);
        }
    }

    /**
     * The prefix used in the configuration file for leap months.
     * 配置文件中用于闰月的前缀
     */
    private static final String LEAP_PREFIX = "leap";

    /**
     * Parse the month lengths for a given year from the configuration line.
     * 从配置行中解析给定年份的月份长度
     *
     * @param year                  the year for which the month lengths are being parsed
     *                              正在解析月份长度的年份
     * @param monthLengthConfigLine the month length configuration line for the year
     *                              年份对应的月份长度配置行
     * @return a ParsedMonths object containing the month lengths and leap month index
     * 包含月份长度和闰月索引的 ParsedMonths 对象
     */
    private static ParsedMonths parseMonthLengths(int year, String monthLengthConfigLine) {
        String[] monthLengthStringArray = createMonthLengthStringArray(year, monthLengthConfigLine);
        int length = monthLengthStringArray.length;
        int[] monthLengthIntArray = new int[length];
        int leapMonth = 0;
        for (int i = 0; i < length; i++) {
            String monthLengthString = monthLengthStringArray[i];
            String numericMonthLength = monthLengthString;
            if (numericMonthLength.startsWith(LEAP_PREFIX)) {
                numericMonthLength = numericMonthLength.substring(LEAP_PREFIX.length());
                leapMonth = i;
            }
            try {
                monthLengthIntArray[i] = Integer.parseInt(numericMonthLength);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("bad month length: " + monthLengthString + " in year: " + year, e);
            }
        }
        return new ParsedMonths(monthLengthIntArray, leapMonth);
    }

    /**
     * A record to hold the parsed month lengths and leap month index for a given year.
     * 用于保存给定年份的解析后的月份长度和闰月索引
     *
     * @param monthLengths 月份长度数组
     * @param leapMonth    闰月索引
     */
    private record ParsedMonths(int[] monthLengths, int leapMonth) {
    }

    /**
     * A record to hold the month start epoch days and related data.
     * 用于保存月份开始的纪元天数和相关数据
     *
     * @param monthStartEpochDays 月份开始的纪元天数数组
     * @param minimumDaysInMonth  月份的最小天数
     * @param maximumDaysInMonth  月份的最大天数
     * @param minimumDaysInYear   年份的最小天数
     * @param maximumDaysInYear   年份的最大天数
     */
    private record MonthStartData(int[] monthStartEpochDays, int minimumDaysInMonth, int maximumDaysInMonth,
                                  int minimumDaysInYear, int maximumDaysInYear) {
    }

    /**
     * Create an array of month length strings for a given year from the configuration line.
     * 从配置行中为给定年份创建月份长度字符串数组
     *
     * @param year                  the year for which the month lengths are being created
     *                              正在创建月份长度的年份
     * @param monthLengthConfigLine the month length configuration line for the year
     *                              年份对应的月份长度配置行
     * @return month length string array
     * 月份长度字符串数组
     */
    private static String[] createMonthLengthStringArray(int year, String monthLengthConfigLine) {
        String[] monthLengthStringArray = monthLengthConfigLine.strip().split("\\s+");
        int leapMonthCount = 0;
        for (String monthLength : monthLengthStringArray) {
            if (monthLength.startsWith(LEAP_PREFIX)) {
                leapMonthCount++;
            }
        }

        ValueRange range = ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR.range();
        int monthCount = monthLengthStringArray.length;
        // A common year must contain 12 months and no leap month.
        // 平年必须包含 12 个月，且不能有闰月。
        boolean validCommonYear = monthCount == range.getSmallestMaximum() && leapMonthCount == 0;
        // A leap year must contain 13 months and exactly one leap month.
        // 闰年必须包含 13 个月，且只能有一个闰月。
        boolean validLeapYear = monthCount == range.getMaximum() && leapMonthCount == 1;
        // Reject configurations that match neither a common year nor a leap year.
        // 拒绝既不符合平年规则也不符合闰年规则的配置。
        if (!validCommonYear && !validLeapYear) {
            throw new IllegalArgumentException("Invalid month length config year: " + year +
                    ", line: " + monthLengthConfigLine);
        }
        return monthLengthStringArray;
    }

    /**
     * Create an array of month start epoch days from the collection of month lengths for each year.
     * 从每年的月份长度集合中创建月份开始的纪元天数数组
     *
     * @param daysOfMonthsCollection the collection of month lengths for each year
     *                               每年的月份长度集合
     * @param minimumEpochDay        the minimum epoch day
     *                               最小的纪元天
     * @param maximumMonthsInYear    the maximum number of months in a year
     *                               每年的最大月份数
     * @return MonthStartData containing the month start epoch days
     * 包含月份开始的纪元天数的数据
     */
    private static MonthStartData createMonthStartEpochDays(Collection<int[]> daysOfMonthsCollection,
                                                            int minimumEpochDay, int maximumMonthsInYear) {
        int[] monthStartEpochDays = new int[daysOfMonthsCollection.size() * maximumMonthsInYear + 1];
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
                minimumEpochDay += dayOfMonth;
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

        // Insert the final epochDay
        monthStartEpochDays[index++] = minimumEpochDay;

        if (index != monthStartEpochDays.length) {
            throw new IllegalStateException("Did not fill epochMonths exactly: ndx = " + index
                    + " should be " + monthStartEpochDays.length);
        }

        return new MonthStartData(monthStartEpochDays, minimumDaysInMonth,
                maximumDaysInMonth, minimumDaysInYear, maximumDaysInYear);
    }

    //-----------------------------------------------------------------------
    // Package-private date helpers
    // 包级日期辅助方法
    //-----------------------------------------------------------------------

    void checkValidYear(long prolepticYear) {
        range(ChronoField.YEAR).checkValidValue(prolepticYear, ChronoField.YEAR);
    }

    void checkValidDayOfYear(long dayOfYear) {
        range(ChronoField.DAY_OF_YEAR).checkValidValue(dayOfYear, ChronoField.DAY_OF_YEAR);
    }

    void checkValidMonth(int month) {
        range(ChronoField.MONTH_OF_YEAR).checkValidValue(month, ChronoField.MONTH_OF_YEAR);
    }

    int[] getChineseDateInfo(int epochDay) {
        CalendarData data = calendarData();
        if (epochDay < data.minimumEpochDay || epochDay > data.maximumEpochDay) {
            throw new DateTimeException("Date is out of range for Chinese calendar: " + epochDay);
        }

        int epochMonth = epochDayToEpochMonth(data, epochDay);
        int year = epochMonthToYear(data, epochMonth);
        int month = epochMonthToMonth(data, epochMonth, year);
        boolean leapMonth = isLeapEpochMonth(data, epochMonth, year);
        int day = epochMonthToEpochDay(data, epochMonth);
        int date = epochDay - day; // epochDay - dayOfEpoch(year, month);

        int[] dateInfo = new int[4];
        dateInfo[0] = year;
        dateInfo[1] = month;
        dateInfo[2] = date + 1; // change to 1-based.
        dateInfo[3] = leapMonth ? 1 : 0;
        return dateInfo;
    }

    long getEpochDay(int prolepticYear, int monthOfYear, int dayOfMonth) {
        return getEpochDay(prolepticYear, monthOfYear, false, dayOfMonth);
    }

    long getEpochDay(int prolepticYear, int monthOfYear, boolean leapMonth, int dayOfMonth) {
        CalendarData data = calendarData();
        checkValidYear(prolepticYear);
        checkValidMonth(monthOfYear);
        int epochMonth = yearToEpochMonth(data, prolepticYear)
                + chineseYearMonthToEpochMonth(data, prolepticYear, monthOfYear, leapMonth);
        if (epochMonth < 0 || epochMonth >= data.monthStartEpochDays.length) {
            throw new DateTimeException("Invalid Chinese date, year: " +
                    prolepticYear + ", month: " + monthOfYear);
        }
        if (dayOfMonth < 1 || dayOfMonth > epochMonthLength(data, epochMonth)) {
            throw new DateTimeException("Invalid Chinese day of month: " + dayOfMonth);
        }
        return epochMonthToEpochDay(data, epochMonth) + (dayOfMonth - 1);
    }

    int getDayOfYear(int prolepticYear, int month) {
        CalendarData data = calendarData();
        return yearMonthToDayOfYear(data, prolepticYear, (month - 1));
    }

    int getMonthLength(int prolepticYear, int monthOfYear) {
        return getMonthLength(prolepticYear, monthOfYear, false);
    }

    int getMonthLength(int prolepticYear, int monthOfYear, boolean leapMonth) {
        CalendarData data = calendarData();
        int epochMonth = yearToEpochMonth(data, prolepticYear)
                + chineseYearMonthToEpochMonth(data, prolepticYear, monthOfYear, leapMonth);
        if (epochMonth < 0 || epochMonth >= data.monthStartEpochDays.length) {
            throw new DateTimeException("Invalid Chinese date, year: " +
                    prolepticYear + ", month: " + monthOfYear);
        }
        return epochMonthLength(data, epochMonth);
    }

    int getYearLength(int prolepticYear) {
        CalendarData data = calendarData();
        return epochMonthToEpochDay(data, yearToEpochMonth(data, prolepticYear + 1))
                - epochMonthToEpochDay(data, yearToEpochMonth(data, prolepticYear));
    }

    long monthsBetween(ChineseDate start, ChineseDate end) {
        CalendarData data = calendarData();
        long startMonth = monthIndex(data, start);
        long endMonth = monthIndex(data, end);
        long months = endMonth - startMonth;
        if (months > 0 && end.getLong(ChronoField.DAY_OF_MONTH) < start.getLong(ChronoField.DAY_OF_MONTH)) {
            months--;
        } else if (months < 0 && end.getLong(ChronoField.DAY_OF_MONTH) > start.getLong(ChronoField.DAY_OF_MONTH)) {
            months++;
        }
        return months;
    }

    long epochDayAfterMonths(ChineseDate start, long months) {
        CalendarData data = calendarData();
        long targetIndex = Math.addExact(monthIndex(data, start), months);
        if (targetIndex < 0) {
            throw new DateTimeException("Month adjustment is out of range for Chinese calendar: " + months);
        }
        int year = data.minimumYear;
        while (year <= data.maximumYear) {
            int monthCount = data.leapMonthByYear.containsKey(year) ? 13 : 12;
            if (targetIndex < monthCount) {
                break;
            }
            targetIndex -= monthCount;
            year++;
        }
        if (year > data.maximumYear) {
            throw new DateTimeException("Month adjustment is out of range for Chinese calendar: " + months);
        }
        int monthIndex = Math.toIntExact(targetIndex);
        int leapMonth = data.leapMonthByYear.getOrDefault(year, 0);
        boolean leap = leapMonth != 0 && monthIndex == leapMonth;
        int month = leap ? leapMonth : monthIndex + 1 - (leapMonth != 0 && monthIndex > leapMonth ? 1 : 0);
        int epochMonth = yearToEpochMonth(data, year) + chineseYearMonthToEpochMonth(data, year, month, leap);
        if (epochMonth < 0 || epochMonth >= data.monthStartEpochDays.length) {
            throw new DateTimeException("Invalid Chinese date, year: " + year + ", month: " + month);
        }
        checkValidYear(year);
        checkValidMonth(month);
        int day = Math.min(start.get(ChronoField.DAY_OF_MONTH), epochMonthLength(data, epochMonth));
        return epochMonthToEpochDay(data, epochMonth) + day - 1;
    }

    //-----------------------------------------------------------------------
    // Private date conversion helpers
    // 私有日期转换辅助方法
    //-----------------------------------------------------------------------

    private int epochDayToEpochMonth(CalendarData data, int epochDay) {
        // binary search
        int ndx = Arrays.binarySearch(data.monthStartEpochDays, epochDay);
        if (ndx < 0) {
            ndx = -ndx - 2;
        }
        // todo 最后3天相同？
        while (ndx + 1 < data.monthStartEpochDays.length
                && data.monthStartEpochDays[ndx + 1] == epochDay) {
            ndx++;
        }
        return ndx;
    }

    private int epochMonthToYear(CalendarData data, int epochMonth) {
        return (epochMonth + data.epochMonthOffset) / data.maximumMonthsInYear;
    }

    private int yearToEpochMonth(CalendarData data, int year) {
        return year * data.maximumMonthsInYear - data.epochMonthOffset;
    }


    // todo 可优化？
    private int epochMonthToMonth(CalendarData data, int epochMonth, int year) {
        int monthIndex = epochMonth - yearToEpochMonth(data, year);
        int leapMonth = data.leapMonthByYear.getOrDefault(year, 0);
        if (leapMonth != 0 && monthIndex == leapMonth) {
            return leapMonth;
        }
        return monthIndex + 1 - (leapMonth != 0 && monthIndex > leapMonth ? 1 : 0);
    }

    private int epochMonthToEpochDay(CalendarData data, int epochMonth) {
        return data.monthStartEpochDays[epochMonth];
    }

    private int yearMonthToDayOfYear(CalendarData data, int prolepticYear, int month) {
        int epochMonthFirst = yearToEpochMonth(data, prolepticYear);
        return epochMonthToEpochDay(data, epochMonthFirst + month)
                - epochMonthToEpochDay(data, epochMonthFirst);
    }

    private int epochMonthLength(CalendarData data, int epochMonth) {
        // The very last entry in the epochMonth table is not the start of a month
        int result = data.monthStartEpochDays[epochMonth + 1] - data.monthStartEpochDays[epochMonth];
        if (result == 0) {
            return data.monthStartEpochDays[epochMonth + 2] - data.monthStartEpochDays[epochMonth];
        }
        return result;
    }

    private static void registerCustomChrono() {

        //TODO
    }

    private boolean isLeapEpochMonth(CalendarData data, int epochMonth, int year) {
        int monthIndex = epochMonth - yearToEpochMonth(data, year);
        int leapMonth = data.leapMonthByYear.getOrDefault(year, 0);
        return leapMonth != 0 && leapMonth == monthIndex;
    }

    private int chineseYearMonthToEpochMonth(CalendarData data, int year, int month, boolean leap) {
        int leapMonth = data.leapMonthByYear.getOrDefault(year, 0);
        if (leap) {
            if (leapMonth != month) {
                throw new DateTimeException("Invalid leap month: " + month + " in year " + year);
            }
            return month;
        }
        return month - 1 + (leapMonth != 0 && month > leapMonth ? 1 : 0);
    }

    private long monthIndex(CalendarData data, ChineseDate date) {
        // The epoch-month table has padding slots for common years; they are not actual months.
        long months = (long) (date.prolepticYear() - data.minimumYear) * 12;
        for (int leapYear : data.leapMonthByYear.keySet()) {
            if (leapYear < date.prolepticYear()) {
                months++;
            }
        }
        return months + chineseYearMonthToEpochMonth(
                data, date.prolepticYear(), date.monthOfYear(), date.isLeapMonth());
    }

    //-----------------------------------------------------------------------
    // Serialization methods
    // 序列化相关方法
    //-----------------------------------------------------------------------

    @Serial
    Object writeReplace() {
        return new Ser(typeId, calendarType);
    }

    @Serial
    private void readObject(ObjectInputStream s) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    private static final class Ser implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private final String typeId;
        private final String calendarType;

        private Ser(String typeId, String calendarType) {
            this.typeId = typeId;
            this.calendarType = calendarType;
        }

        @Serial
        private Object readResolve() throws ObjectStreamException {
            if (INSTANCE.getId().equals(typeId) && INSTANCE.getCalendarType().equals(calendarType)) {
                return INSTANCE;
            }
            throw new InvalidObjectException("Unknown Chinese chronology: " + typeId + "-" + calendarType);
        }
    }

}
