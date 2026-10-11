package io.github.sbracely.chinese.calendar;

import java.io.Serial;
import java.io.Serializable;
import java.time.DateTimeException;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoPeriod;
import java.time.chrono.Chronology;
import java.time.temporal.*;
import java.util.Objects;

import static java.time.temporal.ChronoField.*;

public final class ChineseDate implements ChronoLocalDate, Serializable {

    // TODO
    @Serial
    private static final long serialVersionUID = 1L;

    private final transient ChineseChronology chronology;

    /**
     * The proleptic year.
     */
    private final transient int prolepticYear;

    /**
     * The month-of-year.
     */
    private final transient int monthOfYear;

    /**
     * Whether this month is a leap month.
     */
    private final transient boolean leapMonth;

    /**
     * The day-of-month.
     */
    private final transient int dayOfMonth;

    private ChineseDate(ChineseChronology chronology, int prolepticYear, int monthOfYear, boolean leapMonth, int dayOfMonth) {
        // Computing the Gregorian day checks the valid ranges
        chronology.getEpochDay(prolepticYear, monthOfYear, leapMonth, dayOfMonth);

        this.chronology = chronology;
        this.prolepticYear = prolepticYear;
        this.monthOfYear = monthOfYear;
        this.leapMonth = leapMonth;
        this.dayOfMonth = dayOfMonth;
    }

    //-----------------------------------------------------------------------
    // Override methods from ChronoLocalDate
    // 重写 ChronoLocalDate 接口中的方法
    //-----------------------------------------------------------------------

    @Override
    public ChronoLocalDate with(TemporalField field, long newValue) {
        if (field instanceof ChronoField chronoField) {
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
        return ChronoLocalDate.super.with(field, newValue);
    }

    public int getDayOfYear() {
        return chronology.getDayOfYear(prolepticYear, monthOfYear) + dayOfMonth;
    }

    public DayOfWeek getDayOfWeek() {
        int dow0 = Math.floorMod(toEpochDay() + 3, 7);
        return DayOfWeek.of(dow0 + 1);
    }

    /**
     * Resolves an adjusted date by falling back to the corresponding regular month if the
     * specified leap month does not exist, then clamping the day to the month's length.
     * 解析调整后的日期：如果指定闰月不存在，则回退到同名普通月，再将日数限制在该月份的天数内。
     *
     * @param prolepticYear the target proleptic year
     *                      目标年份
     * @param month the target month-of-year
     *              目标月份
     * @param leapMonth whether the target month is specified as a leap month
     *                  是否指定使用目标月份的闰月
     * @param day the specified day-of-month
     *            指定的日数
     * @return the resolved date
     * 调整后的日期
     */
    private ChineseDate resolvePreviousValid(int prolepticYear, int month, boolean leapMonth, int day) {
        leapMonth = leapMonth && chronology.getLeapMonth(prolepticYear) == month;
        int monthLength = chronology.getMonthLength(prolepticYear, month, leapMonth);
        return ChineseDate.of(chronology, prolepticYear, month, leapMonth, Math.min(day, monthLength));
    }

    static ChineseDate of(ChineseChronology chrono, int prolepticYear, int monthOfYear, int dayOfMonth) {
        return of(chrono, prolepticYear, monthOfYear, false, dayOfMonth);
    }

    static ChineseDate of(ChineseChronology chrono, int prolepticYear, int monthOfYear, boolean leapMonth, int dayOfMonth) {
        return new ChineseDate(chrono, prolepticYear, monthOfYear, leapMonth, dayOfMonth);
    }

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

    public static ChineseDate now() {
        return now(Clock.systemDefaultZone());
    }

    public static ChineseDate now(Clock clock) {
        return ChineseDate.ofEpochDay(ChineseChronology.INSTANCE, LocalDate.now(clock).toEpochDay());
    }

    static ChineseDate ofEpochDay(ChineseChronology chronology, long epochDay) {
        return new ChineseDate(chronology, epochDay);
    }

    @Override
    public Chronology getChronology() {
        return chronology;
    }

    @Override
    public int lengthOfMonth() {
        return chronology.getMonthLength(prolepticYear, monthOfYear, leapMonth);
    }

    public boolean isLeapMonth() {
        return leapMonth;
    }

    int prolepticYear() {
        return prolepticYear;
    }

    int monthOfYear() {
        return monthOfYear;
    }

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

    @Override
    public ChronoPeriod until(ChronoLocalDate endDateExclusive) {
        ChineseDate end = chronology.date(endDateExclusive);
        long years = yearsUntil(end);
        ChineseDate afterYears = plusYears(years);
        long months = chronology.monthsBetween(afterYears, end);
        long monthDays = end.toEpochDay() - afterYears.plusMonths(months).toEpochDay();
        return chronology.period(Math.toIntExact(years), Math.toIntExact(months), Math.toIntExact(monthDays));
    }

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

    @Override
    public ChineseDate plus(TemporalAmount amount) {
        return (ChineseDate) ChronoLocalDate.super.plus(amount);
    }

    @Override
    public ChineseDate minus(TemporalAmount amount) {
        return (ChineseDate) ChronoLocalDate.super.minus(amount);
    }

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
                case ERAS -> (ChineseDate) with(ERA, Math.addExact(getLong(ERA), amountToAdd));
                default -> throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
            };
        }
        return (ChineseDate) ChronoLocalDate.super.plus(amountToAdd, unit);
    }

    @Override
    public ChineseDate minus(long amountToSubtract, TemporalUnit unit) {
        return (ChineseDate) ChronoLocalDate.super.minus(amountToSubtract, unit);
    }

    public ChineseDate plusYears(long years) {
        if (years == 0) {
            return this;
        }
        long targetYear = Math.addExact(prolepticYear, years);
        chronology.checkValidYear(targetYear);
        int year = Math.toIntExact(targetYear);
        return resolvePreviousValid(year, monthOfYear, leapMonth, dayOfMonth);
    }

    public ChineseDate plusMonths(long months) {
        return months == 0 ? this : ofEpochDay(chronology, chronology.epochDayAfterMonths(this, months));
    }

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
                     AMPM_OF_DAY -> false;
                case DAY_OF_WEEK,
                     ALIGNED_DAY_OF_WEEK_IN_MONTH,
                     ALIGNED_DAY_OF_WEEK_IN_YEAR,
                     DAY_OF_MONTH,
                     DAY_OF_YEAR,
                     EPOCH_DAY,
                     ALIGNED_WEEK_OF_MONTH,
                     ALIGNED_WEEK_OF_YEAR,
                     MONTH_OF_YEAR -> true;
                case PROLEPTIC_MONTH -> false;
                case YEAR_OF_ERA,
                     YEAR,
                     ERA -> true;
                case INSTANT_SECONDS,
                     OFFSET_SECONDS -> false;
            };
        } else if (field instanceof ChineseTemporalField) {
            return true;
        }
        return ChronoLocalDate.super.isSupported(field);
    }

    @Override
    public long getLong(TemporalField field) {
        if (field instanceof ChronoField chronoField) {
            return switch (chronoField) {
                case DAY_OF_WEEK -> getDayOfWeek().getValue();
                case ALIGNED_DAY_OF_WEEK_IN_MONTH -> (dayOfMonth - 1) % 7 + 1;
                case ALIGNED_DAY_OF_WEEK_IN_YEAR -> 0L;
                case DAY_OF_MONTH -> dayOfMonth;
                case DAY_OF_YEAR -> getDayOfYear();
                case EPOCH_DAY -> toEpochDay();
                case ALIGNED_WEEK_OF_MONTH -> (dayOfMonth - 1) / 7 + 1;
                case ALIGNED_WEEK_OF_YEAR -> (getDayOfYear() - 1) / 7 + 1;
                case MONTH_OF_YEAR -> monthOfYear;
                case YEAR_OF_ERA -> prolepticYear;
                case YEAR -> prolepticYear;
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

    @Override
    public long toEpochDay() {
        return chronology.getEpochDay(prolepticYear, monthOfYear, leapMonth, dayOfMonth);
    }

    @Override
    public int lengthOfYear() {
        return chronology.getYearLength(prolepticYear);
    }

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


    @Override
    public int hashCode() {
        return Objects.hash(chronology, prolepticYear, monthOfYear, leapMonth, dayOfMonth);
    }

    /**
     * Returns the chronology ID and date, using {@code bis} after leap months
     * and two digits for the month and day.
     * 返回历法 ID 和日期，在闰月月份后添加 {@code bis}，月和日使用两位数字。
     */
    @Override
    public String toString() {
        return getChronology().getId() + " " + prolepticYear
                + "-" + (monthOfYear < 10 ? "0" : "") + monthOfYear + (leapMonth ? "bis" : "")
                + "-" + (dayOfMonth < 10 ? "0" : "") + dayOfMonth;
    }

    public ChineseDate plusDays(long days) {
        return days == 0 ? this : ofEpochDay(chronology, Math.addExact(toEpochDay(), days));
    }
}
