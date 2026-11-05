package io.github.sbracely.chinese.calendar;

import io.github.sbracely.chinese.calendar.spi.ConfigLoader;
import io.github.sbracely.chinese.calendar.spi.ConfigProvider;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
 * <p>
 * All instances share one data set. An external data file can be selected before the first
 * calendar-data access using {@link #configure(Path)}, a {@link ConfigProvider},
 * the {@code chinese.calendar.config}
 * system property, or the {@code CHINESE_CALENDAR_CONFIG} environment variable, in that order
 * of precedence. If none is set, {@code chinese-calendar-config.properties} in the JVM
 * working directory takes precedence over the built-in resource. Relative paths use the JVM
 * working directory. Data cannot be reloaded.
 * 所有实例共享一套数据。首次访问历法数据前，可通过 {@link #configure(Path)}、
 * SPI 配置提供者、系统属性 {@code chinese.calendar.config} 或环境变量
 * {@code CHINESE_CALENDAR_CONFIG} 指定外部数据文件，优先级依次降低。若均未指定，则优先读取 JVM 工作目录下的
 * {@code chinese-calendar-config.properties}，再读取 JAR 内置资源。相对路径基于 JVM 工作目录，
 * 数据不可重新加载。
 */
public final class ChineseChronology extends AbstractChronology implements Serializable {

    /**
     * The base name of the resource bundle containing localized calendar names.
     * 包含本地化历法名称的资源包基础名称。
     */
    private static final String RESOURCE_BUNDLE_BASE_NAME = "cldr.chinese-calendar";

    /**
     * The key for the localized Chinese calendar name in the resource bundle.
     * 资源包中农历本地化名称对应的键。
     */
    private static final String RESOURCE_BUNDLE_CALENDAR_NAME = "name";

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
     * The built-in calendar data resource.
     * 内置农历数据资源。
     */
    private static final String CONFIG_RESOURCE = "chinese-calendar-config.properties";

    /**
     * The optional external data path, selected from the SPI provider, system property, or environment.
     * 从 SPI 提供者、系统属性或环境变量选定的可选外部数据路径。
     */
    private static volatile String configFile = configurationPath();

    /**
     * The JVM working directory used to resolve relative configuration paths.
     * 用于解析相对配置路径的 JVM 工作目录。
     */
    private static final Path WORKING_DIRECTORY = Path.of(System.getProperty("user.dir"))
            .toAbsolutePath().normalize();

    /**
     * The prefix used in the configuration file for leap months.
     * 配置文件中用于闰月的前缀
     */
    private static final String LEAP_PREFIX = "leap";

    /**
     * The shared default chronology instance.
     * 共享的默认历法实例。
     */
    public static final ChineseChronology INSTANCE;

    /**
     * The Chinese calendar id.
     * 农历历法 id
     */
    private static final String ID = "Chinese";

    /**
     * The Chinese calendar type.
     * 农历历法类型
     */
    private static final String CALENDAR_TYPE = "chinese";

    /**
     * The immutable calendar data shared by all instances.
     * 所有实例共享的不可变历法数据。
     */
    private static volatile CalendarData calendarData;

    /**
     * The cached failure from the single data loading attempt.
     * 单次数据加载尝试中缓存的异常。
     */
    private static DateTimeException initializationFailure;

    /**
     * Whether an application has already selected a configuration path explicitly.
     * 应用是否已显式选择过配置路径。
     */
    private static boolean applicationConfigurationSet;

    //-----------------------------------------------------------------------
    // Initialization and construction
    // 初始化与构造
    //-----------------------------------------------------------------------

    static {
        INSTANCE = new ChineseChronology();
    }

    /**
     * Creates a chronology sharing the selected data, for service loading.
     * 创建共享选定数据的历法实例，供服务加载使用。
     */
    public ChineseChronology() {
    }

    /**
     * Selects an external data file before the calendar data is first used.
     * The path may be absolute or relative to the JVM working directory.
     * This method can be called only once and before any chronology instance loads calendar data.
     * 在首次使用历法数据前指定外部数据文件。路径可以是绝对路径或相对于 JVM 工作目录的路径。
     * 此方法只能调用一次，且必须在任何历法实例加载数据之前调用。
     *
     * @param path the external calendar data file
     *             外部历法数据文件
     * @throws NullPointerException if the path is null
     *                              如果路径为 null
     * @throws IllegalStateException if calendar data has been loaded or this method was already called
     *                               如果历法数据已加载或此方法已调用过
     */
    public static synchronized void configure(Path path) {
        Objects.requireNonNull(path, "path");
        if (path.toString().isBlank()) {
            throw new IllegalArgumentException("External configuration path is blank");
        }
        if (calendarData != null || initializationFailure != null || applicationConfigurationSet) {
            throw new IllegalStateException("Chinese calendar data configuration is already fixed");
        }
        configFile = resolvePath(path).toString();
        applicationConfigurationSet = true;
    }

    //-----------------------------------------------------------------------
    // Chinese-calendar-specific public methods
    // 农历专有公开方法
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
     * @throws ClassCastException if the era is not the Chinese era, including {@code null}
     *                            如果纪元不是农历纪元（包括 {@code null}）
     * @throws DateTimeException if the date is invalid, the leap month does not exist,
     *                           or the year is outside the supported range
     *                           如果日期无效、闰月不存在或年份超出支持范围
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
     * @throws DateTimeException if the date is invalid, the leap month does not exist,
     *                           or the year is outside the supported range
     *                           如果日期无效、闰月不存在或年份超出支持范围
     */
    public ChineseDate date(int prolepticYear, int month, boolean leapMonth, int dayOfMonth) {
        return ChineseDate.of(this, prolepticYear, month, leapMonth, dayOfMonth);
    }

    /**
     * Calculates the epoch-second from the given era date and time, including the leap-month flag.
     * 根据给定的年号日期（含闰月标志）和时间计算 epochSecond
     *
     * @param era           the era
     *                      纪元
     * @param yearOfEra     the chronology year-of-era
     *                      纪元中的年份
     * @param month         the month-of-year
     *                      年中的月份
     * @param leapMonth     whether the month is a leap month
     *                      该月份是否为闰月
     * @param dayOfMonth    the day-of-month
     *                      月中的日期
     * @param hour          the hour-of-day
     *                      日中的小时
     * @param minute        the minute-of-hour
     *                      小时中的分钟
     * @param second        the second-of-minute
     *                      分钟中的秒
     * @param zoneOffset    the zone offset
     *                      时区偏移量
     * @return the epoch-second
     *         epoch 秒
     * @throws NullPointerException if the era or zone offset is {@code null}
     *                              如果纪元或时区偏移量为 {@code null}
     * @throws ClassCastException if the era is not the Chinese era
     *                            如果纪元不是农历纪元
     * @throws DateTimeException if the date, leap-month flag, or time is invalid,
     *                           or the year is outside the supported range
     *                           如果日期、闰月标志或时间无效，或年份超出支持范围
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
     *                      历法年份
     * @param month         the month-of-year
     *                      年中的月份
     * @param leapMonth     whether the month is a leap month
     *                      该月份是否为闰月
     * @param dayOfMonth    the day-of-month
     *                      月中的日期
     * @param hour          the hour-of-day
     *                      日中的小时
     * @param minute        the minute-of-hour
     *                      小时中的分钟
     * @param second        the second-of-minute
     *                      分钟中的秒
     * @param zoneOffset    the zone offset
     *                      时区偏移量
     * @return the epoch-second
     *         epoch 秒
     * @throws NullPointerException if the zone offset is {@code null}
     *                              如果时区偏移量为 {@code null}
     * @throws DateTimeException if the date, leap-month flag, or time is invalid,
     *                           or the year is outside the supported range
     *                           如果日期、闰月标志或时间无效，或年份超出支持范围
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
     * @throws NullPointerException if the date or zone offset is {@code null}
     *                              如果日期或时区偏移量为 {@code null}
     * @throws DateTimeException if the hour, minute, or second is invalid
     *                           如果小时、分钟或秒无效
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
     * Years outside the configured data also return 0.
     * 配置数据范围之外的年份也返回 0。
     */
    public int getLeapMonth(long prolepticYear) {
        if (!range(ChronoField.YEAR).isValidValue(prolepticYear)) {
            return 0;
        }
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
     * @throws NullPointerException if the field is {@code null}
     *                              如果字段为 {@code null}
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

    /**
     * Gets the identifier of this chronology.
     * 获取此历法的标识符。
     *
     * @return the chronology identifier
     *         历法标识符
     */
    @Override
    public String getId() {
        return ID;
    }

    /**
     * Gets the calendar type of this chronology.
     * 获取此历法的日历类型。
     *
     * @return the calendar type
     *         日历类型
     */
    @Override
    public String getCalendarType() {
        return CALENDAR_TYPE;
    }

    /**
     * Creates a date from the era, year, month, and day.
     * 根据纪元、年份、月份和日期创建农历日期。
     *
     * @param era       the era
     *                  纪元
     * @param yearOfEra the year within the era
     *                  纪元中的年份
     * @param month     the month of the year
     *                  年中的月份
     * @param dayOfMonth the day of the month
     *                   月中的日期
     * @return the created Chinese date
     *         创建的农历日期
     * @throws ClassCastException if the era is not the Chinese era, including {@code null}
     *                            如果纪元不是农历纪元（包括 {@code null}）
     * @throws DateTimeException if the date is invalid or outside the supported range
     *                           如果日期无效或超出支持范围
     */
    @Override
    public ChineseDate date(Era era, int yearOfEra, int month, int dayOfMonth) {
        return date(prolepticYear(era, yearOfEra), month, dayOfMonth);
    }

    /**
     * Creates a date from the proleptic year, month, and day.
     * 根据历法年份、月份和日期创建农历日期。
     *
     * @param prolepticYear the proleptic year
     *                      历法年份
     * @param month         the month of the year
     *                      年中的月份
     * @param dayOfMonth    the day of the month
     *                      月中的日期
     * @return the created Chinese date
     *         创建的农历日期
     * @throws DateTimeException if the date is invalid or outside the supported range
     *                           如果日期无效或超出支持范围
     */
    @Override
    public ChineseDate date(int prolepticYear, int month, int dayOfMonth) {
        return ChineseDate.of(this, prolepticYear, month, dayOfMonth);
    }

    /**
     * Creates a date from the era, year, and day-of-year.
     * 根据纪元、年份和年内日期创建农历日期。
     *
     * @param era       the era
     *                  纪元
     * @param yearOfEra the year within the era
     *                  纪元中的年份
     * @param dayOfYear the day of the year
     *                  年内日期
     * @return the created Chinese date
     *         创建的农历日期
     * @throws ClassCastException if the era is not the Chinese era, including {@code null}
     *                            如果纪元不是农历纪元（包括 {@code null}）
     * @throws DateTimeException if the year or day-of-year is outside its supported range
     *                           如果年份或年内日序超出其支持范围
     */
    @Override
    public ChineseDate dateYearDay(Era era, int yearOfEra, int dayOfYear) {
        return dateYearDay(prolepticYear(era, yearOfEra), dayOfYear);
    }

    /**
     * Creates a date from the proleptic year and day-of-year.
     * 根据历法年份和年内日期创建农历日期。
     *
     * @param prolepticYear the proleptic year
     *                      历法年份
     * @param dayOfYear     the day of the year
     *                      年内日期
     * @return the created Chinese date
     *         创建的农历日期
     * @throws DateTimeException if the year is outside the supported range
     *                           or the day-of-year is invalid for that year
     *                           如果年份超出支持范围，或年内日期超出该年份的有效范围
     */
    @Override
    public ChineseDate dateYearDay(int prolepticYear, int dayOfYear) {
        ChineseDate date = ChineseDate.of(this, prolepticYear, 1, 1);
        if (dayOfYear < 1 || dayOfYear > date.lengthOfYear()) {
            throw new DateTimeException("Invalid day of year: " + dayOfYear);
        }
        return date.plusDays(dayOfYear - 1);
    }

    /**
     * Creates a date from the number of days since the epoch.
     * 根据自历元以来的天数创建农历日期。
     *
     * @param epochDay the number of days since the epoch
     *                 自历元以来的天数
     * @return the created Chinese date
     *         创建的农历日期
     * @throws DateTimeException if the epoch day is outside the supported range
     *                           如果纪元天数超出支持范围
     */
    @Override
    public ChineseDate dateEpochDay(long epochDay) {
        return ChineseDate.ofEpochDay(this, epochDay);
    }

    /**
     * Gets the current date using the system clock and default time zone.
     * 使用系统时钟和默认时区获取当前农历日期。
     *
     * @return the current Chinese date
     *         当前农历日期
     * @throws DateTimeException if the current date is outside the supported range
     *                           如果当前日期超出支持范围
     */
    @Override
    public ChineseDate dateNow() {
        return dateNow(Clock.systemDefaultZone());
    }

    /**
     * Gets the current date using the system clock in the specified time zone.
     * 使用指定时区的系统时钟获取当前农历日期。
     *
     * @param zone the time zone to use
     *             要使用的时区
     * @return the current Chinese date
     *         当前农历日期
     * @throws NullPointerException if the zone is {@code null}
     *                              如果时区为 {@code null}
     * @throws DateTimeException if the current date is outside the supported range
     *                           如果当前日期超出支持范围
     */
    @Override
    public ChineseDate dateNow(ZoneId zone) {
        return dateNow(Clock.system(zone));
    }

    /**
     * Gets the current date using the specified clock.
     * 使用指定时钟获取当前农历日期。
     *
     * @param clock the clock to use
     *              要使用的时钟
     * @return the current Chinese date
     *         当前农历日期
     * @throws NullPointerException if the clock is {@code null}
     *                              如果时钟为 {@code null}
     * @throws DateTimeException if the current date is outside the supported range
     *                           如果当前日期超出支持范围
     */
    @Override
    public ChineseDate dateNow(Clock clock) {
        return dateEpochDay(LocalDate.now(clock).toEpochDay());
    }

    /**
     * Obtains a Chinese date from a temporal object.
     * 从时态对象中获取农历日期。
     *
     * @param temporal the temporal object to convert
     *                 要转换的时态对象
     * @return the converted Chinese date
     *         转换后的农历日期
     * @throws NullPointerException if the temporal object is {@code null}
     *                              如果时态对象为 {@code null}
     * @throws DateTimeException if an epoch day cannot be obtained
     *                           or the date is outside the supported range
     *                           如果无法获取纪元天数或日期超出支持范围
     */
    @Override
    public ChineseDate date(TemporalAccessor temporal) {
        if (temporal instanceof ChineseDate chineseDate) {
            return chineseDate;
        }
        return dateEpochDay(temporal.getLong(ChronoField.EPOCH_DAY));
    }

    /**
     * Obtains a local date-time in this chronology from a temporal object.
     * 根据时态对象获取此历法中的本地日期时间。
     *
     * @param temporal the temporal object to convert
     *                 要转换的时态对象
     * @return the converted local date-time
     *         转换后的本地日期时间
     * @throws NullPointerException if the temporal object is {@code null}
     *                              如果时态对象为 {@code null}
     * @throws DateTimeException if the date-time cannot be obtained
     *                           如果无法获取日期时间
     */
    @Override
    @SuppressWarnings("unchecked")
    public ChronoLocalDateTime<ChineseDate> localDateTime(TemporalAccessor temporal) {
        return (ChronoLocalDateTime<ChineseDate>) super.localDateTime(temporal);
    }

    /**
     * Obtains a zoned date-time in this chronology from a temporal object.
     * 根据时态对象获取此历法中的带时区日期时间。
     *
     * @param temporal the temporal object to convert
     *                 要转换的时态对象
     * @return the converted zoned date-time
     *         转换后的带时区日期时间
     * @throws NullPointerException if the temporal object is {@code null}
     *                              如果时态对象为 {@code null}
     * @throws DateTimeException if the zoned date-time cannot be obtained
     *                           如果无法获取带时区日期时间
     */
    @Override
    @SuppressWarnings("unchecked")
    public ChronoZonedDateTime<ChineseDate> zonedDateTime(TemporalAccessor temporal) {
        return (ChronoZonedDateTime<ChineseDate>) super.zonedDateTime(temporal);
    }

    /**
     * Obtains a zoned date-time in this chronology from an instant and zone.
     * 根据瞬时点和时区获取此历法中的带时区日期时间。
     *
     * @param instant the instant to convert
     *                要转换的瞬时点
     * @param zone    the time zone to use
     *                要使用的时区
     * @return the converted zoned date-time
     *         转换后的带时区日期时间
     * @throws NullPointerException if the instant or zone is {@code null}
     *                              如果瞬时点或时区为 {@code null}
     * @throws DateTimeException if the zoned date-time cannot be obtained
     *                           如果无法获取带时区日期时间
     */
    @Override
    @SuppressWarnings("unchecked")
    public ChronoZonedDateTime<ChineseDate> zonedDateTime(Instant instant, ZoneId zone) {
        return (ChronoZonedDateTime<ChineseDate>) super.zonedDateTime(instant, zone);
    }

    /**
     * Checks whether the specified proleptic year is a leap year.
     * 检查指定历法年份是否为闰年。
     *
     * @param prolepticYear the proleptic year to check
     *                      要检查的历法年份
     * @return {@code true} if the year contains a leap month
     *         如果该年份包含闰月则返回 {@code true}
     *         Years outside the configured data return {@code false}.
     *         配置数据范围之外的年份返回 {@code false}。
     */
    @Override
    public boolean isLeapYear(long prolepticYear) {
        if (!range(ChronoField.YEAR).isValidValue(prolepticYear)) {
            return false;
        }
        return calendarData().leapMonthByYear.containsKey((int) prolepticYear);
    }

    /**
     * Converts an era-based year to a proleptic year.
     * 将纪元年份转换为历法年份。
     *
     * @param era       the era
     *                  纪元
     * @param yearOfEra the year within the era
     *                  纪元中的年份
     * @return the proleptic year
     *         历法年份
     * @throws ClassCastException if the era is not the Chinese era, including {@code null}
     *                            如果纪元不是农历纪元（包括 {@code null}）
     */
    @Override
    public int prolepticYear(Era era, int yearOfEra) {
        if (era != CHINESE_ERA) {
            throw new ClassCastException("Era must be ChineseEra");
        }
        return yearOfEra;
    }

    /**
     * Gets the era corresponding to the specified value.
     * 获取与指定值对应的纪元。
     *
     * @param eraValue the era value
     *                 纪元值
     * @return the corresponding Chinese era
     *         对应的农历纪元
     * @throws DateTimeException if the era value is invalid
     *                           如果纪元值无效
     */
    @Override
    public Era eraOf(int eraValue) {
        return ChineseEra.of(eraValue);
    }

    /**
     * Gets the eras supported by this chronology.
     * 获取此历法支持的所有纪元。
     *
     * @return the supported eras
     *         支持的纪元列表
     */
    @Override
    public List<Era> eras() {
        return List.of(ChineseEra.values());
    }

    /**
     * Gets the valid range for a chronology field.
     * 获取历法字段的有效范围。
     *
     * @param field the field to query
     *              要查询的字段
     * @return the valid value range
     *         有效值范围
     * @throws NullPointerException if the field is {@code null}
     *                              如果字段为 {@code null}
     */
    @Override
    public ValueRange range(ChronoField field) {
        CalendarData data = calendarData();
        return switch (field) {
            case DAY_OF_MONTH -> ValueRange.of(1, data.minimumDaysInMonth, data.maximumDaysInMonth);
            case DAY_OF_YEAR -> ValueRange.of(1, data.minimumDaysInYear, data.maximumDaysInYear);
            case ALIGNED_WEEK_OF_YEAR ->
                    ValueRange.of(1, (data.minimumDaysInYear + 6) / 7, (data.maximumDaysInYear + 6) / 7);
            case YEAR, YEAR_OF_ERA -> ValueRange.of(data.minimumYear, data.maximumYear);
            case ERA -> ValueRange.of(CHINESE_ERA.getValue(), CHINESE_ERA.getValue());
            default -> field.range();
        };
    }

    /**
     * Gets the localized display name of this chronology.
     * 获取此历法的本地化显示名称。
     *
     * @param style the text style
     *              文本样式
     * @param locale the locale to use
     *               要使用的区域设置
     * @return the localized display name
     *         本地化显示名称
     * @throws NullPointerException if the style or locale is {@code null}
     *                              如果文本样式或区域设置为 {@code null}
     */
    @Override
    public String getDisplayName(TextStyle style, Locale locale) {
        String displayName = super.getDisplayName(style, locale);
        if (!getId().equals(displayName)) {
            return displayName;
        }
        ResourceBundle bundle = ResourceBundle.getBundle(RESOURCE_BUNDLE_BASE_NAME, locale);
        return bundle.getString(RESOURCE_BUNDLE_CALENDAR_NAME);
    }

    /**
     * Creates a period in this chronology.
     * 在此历法中创建日期期间。
     *
     * @param years  the number of years
     *               年数
     * @param months the number of months
     *               月数
     * @param days   the number of days
     *               天数
     * @return the created Chinese period
     *         创建的农历期间
     */
    @Override
    public ChinesePeriod period(int years, int months, int days) {
        return new ChinesePeriod(this, years, months, days);
    }

    //-----------------------------------------------------------------------
    // Override methods from AbstractChronology
    // 重写 AbstractChronology 中的方法
    //-----------------------------------------------------------------------

    /**
     * Resolves parsed date fields into a Chinese date.
     * 将解析后的日期字段解析为农历日期。
     * Month numbers identify regular months unless {@code IS_LEAP_MONTH} is set to 1.
     * 月份编号表示普通月，除非 {@code IS_LEAP_MONTH} 被设为 1。
     * In lenient year-month-day resolution, month overflow uses twelve named months per year;
     * day overflow follows the timeline through any leap months.
     * 宽松解析年月日时，月份溢出按每年十二个命名月份处理；日期溢出沿时间线经过闰月。
     * {@code MONTH_AS_ORDINAL_OF_YEAR} counts leap months separately; in lenient mode
     * its overflow follows actual chronological months.
     * {@code MONTH_AS_ORDINAL_OF_YEAR} 将闰月单独计数，宽松模式下按实际月份顺序处理溢出。
     * An epoch day takes precedence; remaining fields are cross-checked by the formatter.
     * 纪元天数优先解析，剩余字段由格式化器进行交叉校验。
     *
     * @param fieldValues the parsed field values
     *                    已解析的字段值
     * @param resolverStyle the resolver style
     *                      解析样式
     * @return the resolved Chinese date, or {@code null} if unresolved
     *         解析后的农历日期；如果无法解析则返回 {@code null}
     * @throws DateTimeException if the fields conflict or cannot form a valid supported date
     *                           如果字段冲突或无法组成有效且受支持的日期
     */
    @Override
    public ChineseDate resolveDate(Map<TemporalField, Long> fieldValues, ResolverStyle resolverStyle) {
        Objects.requireNonNull(resolverStyle, "resolverStyle");
        if (fieldValues.containsKey(ChronoField.EPOCH_DAY)) {
            return (ChineseDate) super.resolveDate(fieldValues, resolverStyle);
        }
        if ((fieldValues.containsKey(ChronoField.MONTH_OF_YEAR)
                || fieldValues.containsKey(ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR))
                && fieldValues.containsKey(ChronoField.DAY_OF_MONTH)
                && (resolverStyle == ResolverStyle.LENIENT
                || fieldValues.containsKey(ChineseTemporalField.IS_LEAP_MONTH)
                || fieldValues.containsKey(ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR))) {
            Map<TemporalField, Long> yearFields = new HashMap<>();
            for (ChronoField field : new ChronoField[]{
                    ChronoField.YEAR, ChronoField.YEAR_OF_ERA, ChronoField.ERA}) {
                if (fieldValues.containsKey(field)) {
                    yearFields.put(field, fieldValues.get(field));
                }
            }
            super.resolveDate(yearFields, resolverStyle);
            Long resolvedYear = yearFields.get(ChronoField.YEAR);
            if (resolvedYear != null) {
                int year = range(ChronoField.YEAR).checkValidIntValue(resolvedYear, ChronoField.YEAR);
                long day = fieldValues.get(ChronoField.DAY_OF_MONTH);
                Long ordinal = fieldValues.get(ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR);
                if (ordinal != null) {
                    ChineseDate first = date(year, 1, 1);
                    if (resolverStyle != ResolverStyle.LENIENT) {
                        first.range(ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR)
                                .checkValidValue(ordinal, ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR);
                        range(ChronoField.DAY_OF_MONTH).checkValidValue(day, ChronoField.DAY_OF_MONTH);
                    }
                    first = first.plusMonths(Math.subtractExact(ordinal, 1));
                    ChineseDate result = resolverStyle == ResolverStyle.LENIENT
                            ? first.plusDays(Math.subtractExact(day, 1))
                            : date(first.prolepticYear(), first.monthOfYear(), first.isLeapMonth(),
                            Math.toIntExact(resolverStyle == ResolverStyle.SMART
                                    ? Math.min(day, first.lengthOfMonth()) : day));
                    fieldValues.remove(ChronoField.YEAR);
                    fieldValues.remove(ChronoField.YEAR_OF_ERA);
                    fieldValues.remove(ChronoField.ERA);
                    fieldValues.remove(ChronoField.DAY_OF_MONTH);
                    fieldValues.remove(ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR);
                    checkResolvedFields(fieldValues, result);
                    return result;
                }
                long month = fieldValues.get(ChronoField.MONTH_OF_YEAR);
                Long leapValue = fieldValues.get(ChineseTemporalField.IS_LEAP_MONTH);
                boolean leap = leapValue != null
                        && ChineseTemporalField.IS_LEAP_MONTH.range()
                        .checkValidIntValue(leapValue, ChineseTemporalField.IS_LEAP_MONTH) == 1;
                ChineseDate result;
                if (resolverStyle == ResolverStyle.LENIENT) {
                    // Month numbers name regular months; leap months are selected explicitly.
                    // 月份编号表示普通月，闰月须显式指定。
                    long yearOffset = Math.floorDiv(Math.subtractExact(month, 1), 12);
                    int targetYear = range(ChronoField.YEAR).checkValidIntValue(
                            Math.addExact(year, yearOffset), ChronoField.YEAR);
                    int targetMonth = (int) Math.floorMod(Math.subtractExact(month, 1), 12) + 1;
                    result = date(targetYear, targetMonth, leap, 1).plusDays(Math.subtractExact(day, 1));
                } else {
                    int validMonth = range(ChronoField.MONTH_OF_YEAR)
                            .checkValidIntValue(month, ChronoField.MONTH_OF_YEAR);
                    int validDay = range(ChronoField.DAY_OF_MONTH)
                            .checkValidIntValue(day, ChronoField.DAY_OF_MONTH);
                    ChineseDate first = date(year, validMonth, leap, 1);
                    result = date(year, validMonth, leap, resolverStyle == ResolverStyle.SMART
                            ? Math.min(validDay, first.lengthOfMonth()) : validDay);
                }
                fieldValues.remove(ChronoField.YEAR);
                fieldValues.remove(ChronoField.YEAR_OF_ERA);
                fieldValues.remove(ChronoField.ERA);
                fieldValues.remove(ChronoField.MONTH_OF_YEAR);
                fieldValues.remove(ChronoField.DAY_OF_MONTH);
                fieldValues.remove(ChineseTemporalField.IS_LEAP_MONTH);
                checkResolvedFields(fieldValues, result);
                return result;
            }
        }
        return (ChineseDate) super.resolveDate(fieldValues, resolverStyle);
    }

    /**
     * Checks and consumes remaining fields supported by a resolved date.
     * 校验并移除已解析日期所支持的剩余字段。
     *
     * @param fields the remaining parsed fields
     *               剩余的已解析字段
     * @param date the resolved date
     *             已解析日期
     * @throws DateTimeException if a remaining field conflicts with the date
     *                           如果剩余字段与日期冲突
     */
    private void checkResolvedFields(Map<TemporalField, Long> fields, ChineseDate date) {
        var iterator = fields.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<TemporalField, Long> entry = iterator.next();
            if (date.isSupported(entry.getKey())) {
                if (date.getLong(entry.getKey()) != entry.getValue()) {
                    throw new DateTimeException("Conflict found: " + entry.getKey());
                }
                iterator.remove();
            }
        }
    }

    //-----------------------------------------------------------------------
    // Initialize calendar data
    // 初始化日历数据
    //-----------------------------------------------------------------------

    private static String configurationPath() {
        String path = ConfigLoader.getChineseCalendarConfig();
        if (path == null) {
            path = System.getProperty("chinese.calendar.config");
        }
        return path != null ? path : System.getenv("CHINESE_CALENDAR_CONFIG");
    }

    private static Path resolvePath(Path path) {
        return path.isAbsolute() ? path.normalize() : WORKING_DIRECTORY.resolve(path).normalize();
    }

    /**
     * Gets the calendar data, loading it from the configuration file if necessary.
     * 获取日历数据；如果尚未加载，则从配置文件中加载。
     *
     * @return the calendar data for this chronology
     * 此历法的日历数据
     */
    private static CalendarData calendarData() {
        CalendarData data = calendarData;
        if (data == null) {
            synchronized (ChineseChronology.class) {
                data = calendarData;
                if (data == null) {
                    if (initializationFailure != null) {
                        throw initializationFailure;
                    }
                    try {
                        data = loadCalendarData();
                        calendarData = data;
                    } catch (DateTimeException e) {
                        initializationFailure = e;
                        throw e;
                    }
                }
            }
        }
        return data;
    }

    /**
     * Load the calendar data from the configuration file.
     * 加载配置文件中的日历数据
     *
     * @return the loaded CalendarData
     * 加载的 CalendarData
     * @throws DateTimeException if the configuration cannot be read or its data is invalid
     *                           如果无法读取配置或配置数据无效
     */
    private static CalendarData loadCalendarData() {
        try {
            Properties props = readConfigProperties();
            String id = props.getProperty(KEY_ID);
            if (!ID.equals(id)) {
                throw new IllegalArgumentException("Configuration is for a different calendar: " + id);
            }
            String type = props.getProperty(KEY_TYPE);
            if (!CALENDAR_TYPE.equals(type)) {
                throw new IllegalArgumentException("Configuration is for a different calendar type: " + type);
            }
            String version = props.getProperty(KEY_VERSION);
            if (version == null || version.isBlank()) {
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
                ChronoField.YEAR_OF_ERA.checkValidValue(year);
                ParsedMonths parsedMonths = parseMonthLengths(year, (String) entries.getValue());
                minimumYear = Math.min(minimumYear, year);
                maximumYear = Math.max(maximumYear, year);
                maximumMonthsInYear = Math.max(maximumMonthsInYear, parsedMonths.monthLengths.length);
                if (parsedMonths.leapMonth != 0) {
                    leapMonthByYear.put(year, parsedMonths.leapMonth);
                }
                if (daysOfMonthsByYear.put(year, parsedMonths.monthLengths) != null) {
                    throw new IllegalArgumentException("Duplicate calendar year: " + year);
                }
            }

            if (daysOfMonthsByYear.isEmpty()
                    || (long) maximumYear - minimumYear + 1 != daysOfMonthsByYear.size()) {
                throw new IllegalArgumentException("Configuration must contain consecutive calendar years");
            }
            int epochMonthOffset = Math.multiplyExact(minimumYear, maximumMonthsInYear);
            // Year-length calculation also indexes the start of the following year.
            // 年长度计算还会定位下一年的起点。
            Math.multiplyExact(Math.addExact(maximumYear, 1), maximumMonthsInYear);
            MonthStartData monthStartData = createMonthStartEpochDays(
                    daysOfMonthsByYear.values(), minimumEpochDay, maximumMonthsInYear);
            int[] monthStartEpochDays = monthStartData.monthStartEpochDays;
            int maximumEpochDay = monthStartEpochDays[monthStartEpochDays.length - 1] - 1;
            return new CalendarData(monthStartEpochDays, minimumEpochDay, maximumEpochDay,
                    epochMonthOffset, monthStartData.minimumDaysInMonth,
                    monthStartData.maximumDaysInMonth, maximumMonthsInYear,
                    monthStartData.minimumDaysInYear, monthStartData.maximumDaysInYear,
                    leapMonthByYear, minimumYear, maximumYear,
                    dataFingerprint(minimumEpochDay, daysOfMonthsByYear, leapMonthByYear));
        } catch (Exception e) {
            throw new DateTimeException("Unable to initialize Chinese calendar from: "
                    + (configFile == null ? CONFIG_RESOURCE : configFile), e);
        }
    }

    /**
     * Reads configuration in the following priority order:
     * <ol>
     *   <li>{@link #configure(Path)}</li>
     *   <li>{@link ConfigProvider} configuration path</li>
     *   <li>{@code chinese.calendar.config} system property</li>
     *   <li>{@code CHINESE_CALENDAR_CONFIG} environment variable</li>
     *   <li>{@code chinese-calendar-config.properties} in the JVM working directory</li>
     *   <li>The built-in {@code chinese-calendar-config.properties} resource</li>
     * </ol>
     * 按以下优先级读取配置：
     * <ol>
     *   <li>{@link #configure(Path)}</li>
     *   <li>{@link ConfigProvider} 提供的配置文件路径</li>
     *   <li>{@code chinese.calendar.config} 系统属性</li>
     *   <li>{@code CHINESE_CALENDAR_CONFIG} 环境变量</li>
     *   <li>JVM 工作目录下的 {@code chinese-calendar-config.properties}</li>
     *   <li>JAR 内置的 {@code chinese-calendar-config.properties} 资源</li>
     * </ol>
     * @return the properties loaded from the resource file
     * 从资源文件中加载的属性
     * @throws IOException if the configuration cannot be read
     *                     如果配置无法读取
     * @throws IllegalArgumentException if the path is blank or the built-in resource is missing
     *                                  如果路径为空白或内置资源不存在
     */
    private static Properties readConfigProperties() throws IOException {
        // 1-3: configFile reflects configure(Path) > system property > environment variable.
        // 1-3：configFile 已按 configure(Path) > 系统属性 > 环境变量的顺序确定。
        if (configFile != null && configFile.isBlank()) {
            throw new IllegalArgumentException("External configuration path is blank");
        }
        // 4-5: if none is set, check the working-directory file, then the built-in resource.
        // 4-5：若均未指定，则依次检查工作目录文件和 JAR 内置资源。
        Path externalPath = configFile == null
                ? WORKING_DIRECTORY.resolve(CONFIG_RESOURCE)
                : resolvePath(Path.of(configFile));
        // Fall back only when the working-directory file is confirmed missing, not unreadable.
        // 仅在确认工作目录文件不存在时回退，文件无法读取时应直接报错。
        InputStream input;
        if (configFile == null && Files.notExists(externalPath)) {
            input = ChineseChronology.class.getClassLoader().getResourceAsStream(CONFIG_RESOURCE);
            if (input == null) {
                throw new IllegalArgumentException("Config resource not found: " + CONFIG_RESOURCE);
            }
        } else {
            input = Files.newInputStream(externalPath);
        }
        try (InputStream is = input) {
            Properties properties = new Properties();
            properties.load(is);
            return properties;
        }
    }

    /**
     * Computes a fingerprint of effective data, independent of file layout and metadata.
     * 计算有效数据的指纹，不受文件排版和元数据影响。
     *
     * @param minimumEpochDay the first supported epoch day
     *                        首个支持的纪元天数
     * @param months the month lengths ordered by year
     *               按年份排序的月份天数
     * @param leapMonths the leap-month numbers by year
     *                   各年的闰月编号
     * @return the SHA-256 data fingerprint
     *         SHA-256 数据指纹
     */
    private static String dataFingerprint(int minimumEpochDay, Map<Integer, int[]> months,
                                          Map<Integer, Integer> leapMonths) {
        StringBuilder content = new StringBuilder().append(minimumEpochDay);
        months.forEach((year, lengths) -> content.append('|').append(year).append(':')
                .append(Arrays.toString(lengths)).append(':').append(leapMonths.getOrDefault(year, 0)));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

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
     * @throws IllegalArgumentException if the month count, leap-month count,
     *                                  or numeric month length is invalid
     *                                  如果月份数量、闰月数量或月份天数的数字格式无效
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
                if (i == 0) {
                    throw new IllegalArgumentException("Leap month must follow a regular month in year: " + year);
                }
                numericMonthLength = numericMonthLength.substring(LEAP_PREFIX.length());
                leapMonth = i;
            }
            try {
                monthLengthIntArray[i] = Integer.parseInt(numericMonthLength);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("bad month length: " + monthLengthString + " in year: " + year, e);
            }
            if (monthLengthIntArray[i] != 29 && monthLengthIntArray[i] != 30) {
                throw new IllegalArgumentException("Invalid month length: " + monthLengthString + " in year: " + year);
            }
        }
        return new ParsedMonths(monthLengthIntArray, leapMonth);
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
     * @throws IllegalArgumentException if the month count and leap-month count
     *                                  do not describe a common or leap year
     *                                  如果月份数量与闰月数量不符合平年或闰年规则
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
     * @throws IllegalArgumentException if a month length is negative
     *                                  如果月份天数为负数
     * @throws IllegalStateException if the month-start table is not filled exactly
     *                               如果月初纪元天数表未被恰好填满
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

    /**
     * Checks that the proleptic year is within this chronology's supported range.
     * 检查历法年份是否在此历法支持的范围内。
     *
     * @param prolepticYear the year to validate
     *                      要验证的历法年份
     * @throws DateTimeException if the year is outside the supported range
     *                           如果年份超出支持范围，则抛出此异常
     */
    void checkValidYear(long prolepticYear) {
        range(ChronoField.YEAR).checkValidValue(prolepticYear, ChronoField.YEAR);
    }

    /**
     * Gets the year, month, day, and leap-month flag for an epoch day.
     * 根据纪元天数获取年份、月份、日期和闰月标志。
     *
     * @param epochDay the epoch day to convert
     *                 要转换的纪元天数
     * @return an array containing year, month, day, and leap-month flag in that order
     *         按顺序包含年份、月份、日期和闰月标志的数组
     * @throws DateTimeException if the epoch day is outside the supported range
     *                           如果纪元天数超出支持范围，则抛出此异常
     */
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

    /**
     * Converts a Chinese date, including its leap-month flag, to an epoch day.
     * 将农历日期（包括闰月标志）转换为纪元天数。
     *
     * @param prolepticYear the proleptic year
     *                      历法年份
     * @param monthOfYear   the month of the year
     *                      年中的月份
     * @param leapMonth     whether the specified month is a leap month
     *                      指定月份是否为闰月
     * @param dayOfMonth    the day of the month
     *                      月中的日期
     * @return the corresponding epoch day
     *         对应的纪元天数
     * @throws DateTimeException if the date is invalid, the leap month does not exist,
     *                           or the year is outside the supported range
     *                           如果日期无效、闰月不存在或年份超出支持范围
     */
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

    /**
     * Gets the length of the specified month, accounting for a leap month.
     * 获取指定月份的天数，并考虑闰月标志。
     *
     * @param prolepticYear the proleptic year
     *                      历法年份
     * @param monthOfYear   the month of the year
     *                      年中的月份
     * @param leapMonth     whether the specified month is a leap month
     *                      指定月份是否为闰月
     * @return the number of days in the month
     *         月份天数
     * @throws DateTimeException if the leap month does not exist
     *                           or the computed month index is outside the calendar data
     *                           如果闰月不存在或计算出的月份索引超出历法数据范围
     */
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

    /**
     * Gets the number of days in the specified year.
     * 获取指定年份的总天数。
     *
     * @param prolepticYear the proleptic year
     *                      历法年份
     * @return the number of days in the year
     *         年份天数
     */
    int getYearLength(int prolepticYear) {
        CalendarData data = calendarData();
        return epochMonthToEpochDay(data, yearToEpochMonth(data, prolepticYear + 1))
                - epochMonthToEpochDay(data, yearToEpochMonth(data, prolepticYear));
    }

    /**
     * Calculates the whole number of months between two dates.
     * 计算两个日期之间的整月数。
     *
     * @param start the starting date
     *              起始日期
     * @param end   the ending date
     *              结束日期
     * @return the number of whole months from start to end
     *         从起始日期到结束日期的整月数
     */
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

    /**
     * Gets the epoch day reached by adding months to a date, clipping the day to the target month.
     * 将月份数加到日期上，并在目标月份中截取有效日期后返回对应的纪元天数。
     *
     * @param start  the starting date
     *               起始日期
     * @param months the number of months to add
     *               要增加的月份数
     * @return the resulting epoch day
     *         结果对应的纪元天数
     * @throws DateTimeException if the resulting date is outside the supported range
     *                           如果结果日期超出支持范围，则抛出此异常
     * @throws ArithmeticException if adding the month count overflows
     *                             如果月份数量相加时溢出
     */
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

    /**
     * Checks that the month is within the valid range.
     * 检查月份是否在有效范围内。
     *
     * @param month the month to validate
     *              要验证的月份
     * @throws DateTimeException if the month is outside the valid range
     *                           如果月份超出有效范围，则抛出此异常
     */
    private void checkValidMonth(int month) {
        range(ChronoField.MONTH_OF_YEAR).checkValidValue(month, ChronoField.MONTH_OF_YEAR);
    }

    /**
     * Finds the epoch-month index containing the specified epoch day.
     * 查找包含指定纪元天数的纪元月份索引。
     *
     * @param data     the calendar data
     *                 历法数据
     * @param epochDay the epoch day to locate
     *                 要定位的纪元天数
     * @return the index of the containing epoch month
     *         所属纪元月份的索引
     */
    private int epochDayToEpochMonth(CalendarData data, int epochDay) {
        // binary search
        int ndx = Arrays.binarySearch(data.monthStartEpochDays, epochDay);
        if (ndx < 0) {
            ndx = -ndx - 2;
        }
        while (ndx + 1 < data.monthStartEpochDays.length
                && data.monthStartEpochDays[ndx + 1] == epochDay) {
            ndx++;
        }
        return ndx;
    }

    /**
     * Converts an epoch-month index to a proleptic year.
     * 将纪元月份索引转换为历法年份。
     *
     * @param data       the calendar data
     *                   历法数据
     * @param epochMonth the epoch-month index
     *                   纪元月份索引
     * @return the corresponding proleptic year
     *         对应的历法年份
     */
    private int epochMonthToYear(CalendarData data, int epochMonth) {
        return (epochMonth + data.epochMonthOffset) / data.maximumMonthsInYear;
    }

    /**
     * Converts a proleptic year to the first epoch-month index of that year.
     * 将历法年份转换为该年份第一个纪元月份的索引。
     *
     * @param data the calendar data
     *             历法数据
     * @param year the proleptic year
     *             历法年份
     * @return the first epoch-month index of the year
     *         该年份第一个纪元月份的索引
     */
    private int yearToEpochMonth(CalendarData data, int year) {
        return year * data.maximumMonthsInYear - data.epochMonthOffset;
    }

    /**
     * Converts an epoch-month index to its month number, excluding the leap-month flag.
     * 将纪元月份索引转换为月份编号，不包含闰月标志。
     *
     * @param data       the calendar data
     *                   历法数据
     * @param epochMonth the epoch-month index
     *                   纪元月份索引
     * @param year       the proleptic year
     *                   历法年份
     * @return the one-based month number
     *         从一开始计数的月份编号
     */
    private int epochMonthToMonth(CalendarData data, int epochMonth, int year) {
        int monthIndex = epochMonth - yearToEpochMonth(data, year);
        int leapMonth = data.leapMonthByYear.getOrDefault(year, 0);
        if (leapMonth != 0 && monthIndex == leapMonth) {
            return leapMonth;
        }
        return monthIndex + 1 - (leapMonth != 0 && monthIndex > leapMonth ? 1 : 0);
    }

    /**
     * Gets the epoch day at the start of an epoch month.
     * 获取纪元月份开始时对应的纪元天数。
     *
     * @param data       the calendar data
     *                   历法数据
     * @param epochMonth the epoch-month index
     *                   纪元月份索引
     * @return the epoch day at the start of the month
     *         月初对应的纪元天数
     */
    private int epochMonthToEpochDay(CalendarData data, int epochMonth) {
        return data.monthStartEpochDays[epochMonth];
    }

    /**
     * Gets the number of days in an epoch month.
     * 获取纪元月份的天数。
     *
     * @param data       the calendar data
     *                   历法数据
     * @param epochMonth the epoch-month index
     *                   纪元月份索引
     * @return the number of days in the month
     *         月份天数
     */
    private int epochMonthLength(CalendarData data, int epochMonth) {
        // The very last entry in the epochMonth table is not the start of a month
        int result = data.monthStartEpochDays[epochMonth + 1] - data.monthStartEpochDays[epochMonth];
        if (result == 0) {
            return data.monthStartEpochDays[epochMonth + 2] - data.monthStartEpochDays[epochMonth];
        }
        return result;
    }

    /**
     * Checks whether an epoch-month index represents the leap month of a year.
     * 检查纪元月份索引是否对应某年的闰月。
     *
     * @param data       the calendar data
     *                   历法数据
     * @param epochMonth the epoch-month index
     *                   纪元月份索引
     * @param year       the proleptic year
     *                   历法年份
     * @return {@code true} if the epoch month is the leap month
     *         如果该纪元月份是闰月则返回 {@code true}
     */
    private boolean isLeapEpochMonth(CalendarData data, int epochMonth, int year) {
        int monthIndex = epochMonth - yearToEpochMonth(data, year);
        int leapMonth = data.leapMonthByYear.getOrDefault(year, 0);
        return leapMonth != 0 && leapMonth == monthIndex;
    }

    /**
     * Converts a Chinese year, month, and leap flag to an epoch-month offset within the year.
     * 将农历年份、月份和闰月标志转换为该年内的纪元月份偏移量。
     *
     * @param data  the calendar data
     *              历法数据
     * @param year  the proleptic year
     *              历法年份
     * @param month the one-based month number
     *              从一开始计数的月份编号
     * @param leap  whether the month is a leap month
     *              该月份是否为闰月
     * @return the epoch-month offset within the year
     *         该年内的纪元月份偏移量
     * @throws DateTimeException if the leap-month flag does not match the year's leap month
     *                           如果闰月标志与该年的闰月不匹配，则抛出此异常
     */
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

    /**
     * Gets the zero-based chronological month index of a date in the supported range.
     * 获取日期在支持范围内按时间顺序排列的从零开始计数的月份索引。
     *
     * @param data the calendar data
     *             历法数据
     * @param date the date to index
     *             要计算索引的日期
     * @return the chronological month index
     *         按时间顺序排列的月份索引
     */
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
    // Internal calendar data types
    // 内部历法数据类型
    //-----------------------------------------------------------------------

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
     * @param fingerprint         the effective calendar data fingerprint
     *                            有效历法数据指纹
     */
    private record CalendarData(int[] monthStartEpochDays, int minimumEpochDay, int maximumEpochDay,
                                int epochMonthOffset, int minimumDaysInMonth, int maximumDaysInMonth,
                                int maximumMonthsInYear, int minimumDaysInYear, int maximumDaysInYear,
                                Map<Integer, Integer> leapMonthByYear, int minimumYear, int maximumYear,
                                String fingerprint) {
        /**
         * Creates immutable calendar data by copying mutable input values.
         * 通过复制可变输入值创建不可变的历法数据。
         */
        private CalendarData {
            monthStartEpochDays = monthStartEpochDays.clone();
            leapMonthByYear = Map.copyOf(leapMonthByYear);
        }
    }

    /**
     * The parsed month lengths and leap-month number for one year.
     * 某一年的月份天数和闰月编号。
     *
     * @param monthLengths the month lengths in sequence
     *                     按顺序排列的月份天数
     * @param leapMonth    the one-based month number of the leap month, or zero if none
     *                     闰月对应的月份编号；没有闰月时为零
     */
    private record ParsedMonths(int[] monthLengths, int leapMonth) {
    }

    /**
     * A record to hold the month start epoch days and related data.
     * 用于保存月份开始的纪元天数和相关数据
     *
     * @param monthStartEpochDays the epoch day at the start of each month
     *                            每个月开始时对应的纪元天数
     * @param minimumDaysInMonth  the minimum number of days in a month
     *                            月份的最少天数
     * @param maximumDaysInMonth  the maximum number of days in a month
     *                            月份的最多天数
     * @param minimumDaysInYear   the minimum number of days in a year
     *                            年份的最少天数
     * @param maximumDaysInYear   the maximum number of days in a year
     *                            年份的最多天数
     */
    private record MonthStartData(int[] monthStartEpochDays, int minimumDaysInMonth, int maximumDaysInMonth,
                                  int minimumDaysInYear, int maximumDaysInYear) {
    }

    //-----------------------------------------------------------------------
    // Serialization methods
    // 序列化相关方法
    //-----------------------------------------------------------------------

    /**
     * Replaces this chronology with its serialization proxy.
     * 将此历法替换为其序列化代理对象。
     *
     * @return the serialization proxy
     *         序列化代理对象
     */
    @Serial
    Object writeReplace() {
        return new Ser(ID, CALENDAR_TYPE, calendarData().fingerprint);
    }

    /**
     * Rejects direct deserialization in favor of the serialization proxy.
     * 拒绝直接反序列化，要求通过序列化代理对象完成反序列化。
     *
     * @param s the input stream
     *          输入流
     * @throws InvalidObjectException always, because deserialization must use the proxy
     *                                 始终抛出此异常，因为反序列化必须通过代理对象完成
     */
    @Serial
    private void readObject(ObjectInputStream s) throws InvalidObjectException {
        throw new InvalidObjectException("Deserialization via serialization delegate");
    }

    /**
     * Serialization proxy for a Chinese chronology.
     * 农历历法的序列化代理对象。
     * @param typeId the chronology identifier to restore.
     *               要恢复的历法标识符。
     * @param calendarType the calendar type to restore.
     *                     要恢复的日历类型。
     * @param fingerprint the required calendar data fingerprint
     *                    所需的历法数据指纹
     */
    private record Ser(String typeId, String calendarType, String fingerprint) implements Serializable {
        /**
         * Resolves the proxy to the chronology singleton.
         * 将代理对象解析为历法单例。
         *
         * @return the matching chronology singleton
         *         匹配的历法单例
         * @throws ObjectStreamException if the chronology or calendar data does not match
         *                               如果历法或历法数据不匹配
         */
        @Serial
        private Object readResolve() throws ObjectStreamException {
            if (!ID.equals(typeId) || !CALENDAR_TYPE.equals(calendarType)) {
                throw new InvalidObjectException("Unknown Chinese chronology: " + typeId + "-" + calendarType);
            }
            try {
                if (!calendarData().fingerprint.equals(fingerprint)) {
                    throw new InvalidObjectException("Chinese calendar data fingerprint mismatch");
                }
                return INSTANCE;
            } catch (DateTimeException e) {
                throw new InvalidObjectException("Unable to load Chinese calendar data", e);
            }
        }
    }

}
