package io.github.sbracely.chinese.calendar;

import java.io.Serial;
import java.io.Serializable;
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
                case YEAR, YEAR_OF_ERA -> resolvePreviousValid((int) newValue, monthOfYear, leapMonth, dayOfMonth);
                case ERA -> this;
                default -> throw new UnsupportedTemporalTypeException("Unsupported field: " + field);
            };
        } else if (field instanceof ChineseTemporalField chineseTemporalField) {
            chineseTemporalField.rangeRefinedBy(this).checkValidValue(newValue, chineseTemporalField);
            return switch (chineseTemporalField) {
                case IS_LEAP_MONTH -> resolvePreviousValid(prolepticYear, monthOfYear, newValue == 1, dayOfMonth);
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

    private ChineseDate resolvePreviousValid(int prolepticYear, int month, boolean leapMonth, int day) {
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
                case YEARS -> chronology.monthsBetween(this, end) / 12;
                default -> throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
            };
        }
        return unit.between(this, end);
    }

    @Override
    public ChronoPeriod until(ChronoLocalDate endDateExclusive) {
        ChineseDate end = chronology.date(endDateExclusive);
        long months = chronology.monthsBetween(this, end);
        long monthDays = end.toEpochDay() - chronology.epochDayAfterMonths(this, months);
        return chronology.period(0, (int) months, (int) monthDays);
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

    // todo: ChineseDateTimeFormatter
    @Override
    public String toString() {
        return getChronology().toString() + " " + prolepticYear + "-" + (leapMonth ? "闰" : "") + monthOfYear + "-" + dayOfMonth;
    }

    public ChineseDate plusDays(long days) {
        return new ChineseDate(chronology, toEpochDay() + days);
    }
}
