package io.github.sbracely.chinese.calendar;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.InvalidObjectException;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Period;
import java.time.chrono.ChronoPeriod;
import java.time.chrono.HijrahChronology;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.List;
import java.util.function.LongFunction;

import static java.time.temporal.ChronoField.*;
import static java.time.temporal.ChronoUnit.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChinesePeriodTest {
    private static final ChineseChronology CHRONOLOGY = ChineseChronology.INSTANCE;

    @Test
    void createsPeriodsUsingFactoriesWithoutNormalizingMonths() {
        assertThat(ChinesePeriod.of(-1, 13, -3)).isEqualTo(new ChinesePeriod(-1, 13, -3));
        assertThat(ChinesePeriod.ofYears(-2)).isEqualTo(new ChinesePeriod(-2, 0, 0));
        assertThat(ChinesePeriod.ofMonths(13)).isEqualTo(new ChinesePeriod(0, 13, 0));
        assertThat(ChinesePeriod.ofDays(-3)).isEqualTo(new ChinesePeriod(0, 0, -3));
        assertThat(ChinesePeriod.ofWeeks(-2)).isEqualTo(new ChinesePeriod(0, 0, -14));
        assertThat(ChinesePeriod.of(0, 0, 0)).isSameAs(ChinesePeriod.ZERO);
        assertThat(ChinesePeriod.ofYears(0)).isSameAs(ChinesePeriod.ZERO);
        assertThat(ChinesePeriod.ofMonths(0)).isSameAs(ChinesePeriod.ZERO);
        assertThat(ChinesePeriod.ofDays(0)).isSameAs(ChinesePeriod.ZERO);
        assertThat(ChinesePeriod.ofWeeks(0)).isSameAs(ChinesePeriod.ZERO);
        for (int weeks : new int[]{Integer.MIN_VALUE, Integer.MAX_VALUE}) {
            assertThatThrownBy(() -> ChinesePeriod.ofWeeks(weeks)).isInstanceOf(ArithmeticException.class);
        }
        ChineseDate start = CHRONOLOGY.date(2023, 1, 1);
        assertThat(start.plus(ChinesePeriod.ofMonths(13))).isEqualTo(start.plusYears(1));
        assertThat(start.plus(ChinesePeriod.ofMonths(12))).isNotEqualTo(start.plusYears(1));
    }

    @Test
    void replacesAndAdjustsOnlyTheSpecifiedComponent() {
        ChinesePeriod period = ChinesePeriod.of(-1, 13, -3);
        assertThat(period.withYears(2)).isEqualTo(new ChinesePeriod(2, 13, -3));
        assertThat(period.withMonths(-2)).isEqualTo(new ChinesePeriod(-1, -2, -3));
        assertThat(period.withDays(4)).isEqualTo(new ChinesePeriod(-1, 13, 4));
        assertThat(period.withYears(-1)).isSameAs(period);
        assertThat(period.withMonths(13)).isSameAs(period);
        assertThat(period.withDays(-3)).isSameAs(period);
        assertThat(period.plusYears(2)).isEqualTo(new ChinesePeriod(1, 13, -3));
        assertThat(period.plusMonths(-2)).isEqualTo(new ChinesePeriod(-1, 11, -3));
        assertThat(period.plusDays(4)).isEqualTo(new ChinesePeriod(-1, 13, 1));
        assertThat(period.minusYears(2)).isEqualTo(new ChinesePeriod(-3, 13, -3));
        assertThat(period.minusMonths(-2)).isEqualTo(new ChinesePeriod(-1, 15, -3));
        assertThat(period.minusDays(4)).isEqualTo(new ChinesePeriod(-1, 13, -7));
        List<LongFunction<ChinesePeriod>> adjustments = List.of(
                period::plusYears, period::plusMonths, period::plusDays,
                period::minusYears, period::minusMonths, period::minusDays);
        for (LongFunction<ChinesePeriod> adjustment : adjustments) {
            assertThat(adjustment.apply(0)).isSameAs(period);
        }
        assertThat(period.withYears(0).withMonths(0).withDays(0)).isSameAs(ChinesePeriod.ZERO);
    }

    @Test
    void checksComponentAdjustmentOverflowWithoutTruncatingLongAmounts() {
        ChinesePeriod maximum = ChinesePeriod.of(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        ChinesePeriod minimum = ChinesePeriod.of(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        List<LongFunction<ChinesePeriod>> additions = List.of(
                maximum::plusYears, maximum::plusMonths, maximum::plusDays);
        List<LongFunction<ChinesePeriod>> subtractions = List.of(
                minimum::minusYears, minimum::minusMonths, minimum::minusDays);
        for (LongFunction<ChinesePeriod> addition : additions) {
            for (long amount : new long[]{1, Long.MIN_VALUE, Long.MAX_VALUE}) {
                assertThatThrownBy(() -> addition.apply(amount)).isInstanceOf(ArithmeticException.class);
            }
        }
        for (LongFunction<ChinesePeriod> subtraction : subtractions) {
            for (long amount : new long[]{1, Long.MIN_VALUE, Long.MAX_VALUE}) {
                assertThatThrownBy(() -> subtraction.apply(amount)).isInstanceOf(ArithmeticException.class);
            }
        }
        long difference = (long) Integer.MAX_VALUE - Integer.MIN_VALUE;
        assertThat(minimum.plusYears(difference).plusMonths(difference).plusDays(difference))
                .isEqualTo(maximum);
        assertThat(maximum.minusYears(difference).minusMonths(difference).minusDays(difference))
                .isEqualTo(minimum);
    }

    @Test
    void calculatesChinesePeriodsBetweenDatesInBothDirections() {
        ChineseDate start = CHRONOLOGY.date(2023, 2, 1);
        ChineseDate end = CHRONOLOGY.date(2023, 2, true, 3);
        ChinesePeriod forward = ChinesePeriod.between(start, end);
        ChinesePeriod backward = ChinesePeriod.between(end, start);
        assertThat(forward).isEqualTo(ChinesePeriod.of(0, 1, 2));
        assertThat(backward).isEqualTo(ChinesePeriod.of(0, -1, -2));
        assertThat(start.plus(forward)).isEqualTo(end);
        assertThat(end.plus(backward)).isEqualTo(start);
        assertThat(ChinesePeriod.between(start, start).isZero()).isTrue();
        assertThatThrownBy(() -> ChinesePeriod.between(null, end)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ChinesePeriod.between(start, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void extractsChinesePeriodComponentsWithoutNormalization() {
        ChinesePeriod period = ChinesePeriod.of(-1, 13, -3);
        assertThat(ChinesePeriod.from(period)).isSameAs(period);
        assertThat(ChinesePeriod.from(new UnitAmount(YEARS, -2))).isEqualTo(ChinesePeriod.ofYears(-2));
        assertThat(ChinesePeriod.from(new UnitAmount(MONTHS, 13))).isEqualTo(ChinesePeriod.ofMonths(13));
        assertThat(ChinesePeriod.from(new UnitAmount(DAYS, -3))).isEqualTo(ChinesePeriod.ofDays(-3));
        assertThat(ChinesePeriod.from(new UnitAmount(DAYS, 0))).isSameAs(ChinesePeriod.ZERO);
    }

    @Test
    void rejectsIncompatibleAndOverflowingAmountsDuringConversion() {
        for (TemporalAmount amount : new TemporalAmount[]{
                Period.ofYears(1), Period.ofDays(1), Period.ZERO,
                HijrahChronology.INSTANCE.period(1, 0, 0), Duration.ofDays(1),
                new UnitAmount(WEEKS, 1)}) {
            assertThatThrownBy(() -> ChinesePeriod.from(amount)).isInstanceOf(DateTimeException.class);
        }
        for (TemporalUnit unit : new TemporalUnit[]{YEARS, MONTHS, DAYS}) {
            for (long value : new long[]{(long) Integer.MIN_VALUE - 1, (long) Integer.MAX_VALUE + 1}) {
                assertThatThrownBy(() -> ChinesePeriod.from(new UnitAmount(unit, value)))
                        .isInstanceOf(ArithmeticException.class);
            }
        }
        assertThatThrownBy(() -> ChinesePeriod.from(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void parsesFormattedPeriodsAndUnprefixedNumericComponents() {
        for (ChinesePeriod period : new ChinesePeriod[]{
                ChinesePeriod.ZERO, ChinesePeriod.of(-1, 13, -3), ChinesePeriod.ofYears(2),
                ChinesePeriod.of(Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE)}) {
            assertThat(ChinesePeriod.parse(period.toString())).isEqualTo(period);
        }
        assertThat(ChinesePeriod.parse("P1Y13M-3D")).isEqualTo(ChinesePeriod.of(1, 13, -3));
        assertThat(ChinesePeriod.parse("Chinese -p1y2m3d")).isEqualTo(ChinesePeriod.of(-1, -2, -3));
        assertThat(ChinesePeriod.parse(new StringBuilder("p2w3d"))).isEqualTo(ChinesePeriod.ofDays(17));
        assertThat(ChinesePeriod.parse("Chinese P0D")).isSameAs(ChinesePeriod.ZERO);
        ChineseDate start = CHRONOLOGY.date(2023, 1, 1);
        assertThat(start.plus(ChinesePeriod.parse("P13M"))).isEqualTo(start.plusYears(1));
    }

    @Test
    void rejectsInvalidPeriodTextAndPreservesTheOriginalInputInErrors() {
        for (String text : new String[]{
                "", "Chinese ", "P", "ISO P1Y", "Japanese P1Y", "Chinese P1D2M",
                "P2147483648Y", "Chinese P2147483647W", "P306783378W2D", "-P-2147483648Y"}) {
            assertThatThrownBy(() -> ChinesePeriod.parse(text))
                    .isInstanceOfSatisfying(DateTimeParseException.class,
                            exception -> assertThat(exception.getParsedString()).isEqualTo(text));
        }
        assertThatThrownBy(() -> ChinesePeriod.parse(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void createsPeriodsWithoutAChronologyArgument() {
        ChinesePeriod period = new ChinesePeriod(1, 2, 3);
        ChinesePeriod fromAnotherInstance = new ChineseChronology().period(1, 2, 3);

        assertThat(period.getChronology()).isSameAs(CHRONOLOGY);
        assertThat(fromAnotherInstance.getChronology()).isSameAs(CHRONOLOGY);
        assertThat(fromAnotherInstance).isEqualTo(period);
        assertThat(fromAnotherInstance.hashCode()).isEqualTo(period.hashCode());
    }

    @Test
    void exposesComponentsAndValueSemantics() {
        ChronoPeriod period = CHRONOLOGY.period(1, 2, 3);
        assertThat(period.getChronology()).isEqualTo(CHRONOLOGY);
        assertThat(period.getUnits()).containsExactly(YEARS, MONTHS, DAYS);
        assertThat(period.get(YEARS)).isEqualTo(1);
        assertThat(period.get(MONTHS)).isEqualTo(2);
        assertThat(period.get(DAYS)).isEqualTo(3);
        assertThat(period).isEqualTo(CHRONOLOGY.period(1, 2, 3));
        assertThat(period.hashCode()).isEqualTo(CHRONOLOGY.period(1, 2, 3).hashCode());
        assertThat(period.toString()).isEqualTo("Chinese P1Y2M3D");
        assertThat(CHRONOLOGY.period(0, 0, 0).toString()).isEqualTo("Chinese P0D");
        assertThat(period.isZero()).isFalse();
        assertThat(CHRONOLOGY.period(0, 0, 0).isZero()).isTrue();
        assertThat(CHRONOLOGY.period(0, -1, 1).isNegative()).isTrue();
        assertThatThrownBy(() -> period.get(WEEKS))
                .isInstanceOf(UnsupportedTemporalTypeException.class);
        assertThatThrownBy(() -> period.get(null))
                .isInstanceOf(UnsupportedTemporalTypeException.class)
                .hasMessage("Unsupported unit: null");
    }

    @Test
    void chainsOperationsWithChineseSpecificReturnTypes() {
        ChinesePeriod period = CHRONOLOGY.period(1, 13, -3)
                .plus(CHRONOLOGY.period(-1, 0, 3))
                .minus(CHRONOLOGY.period(0, 1, 0))
                .negated()
                .normalized();
        ChineseChronology chronology = period.getChronology();

        assertThat(period).isEqualTo(CHRONOLOGY.period(0, -12, 0));
        assertThat(chronology).isSameAs(CHRONOLOGY);
    }

    @Test
    void appliesMixedSignsAcrossLeapMonthAndYearClipping() {
        ChineseDate leapMonthEnd = CHRONOLOGY.date(2023, 2, true, 29);
        ChinesePeriod period = CHRONOLOGY.period(1, -1, 2);

        assertThat(period.addTo(leapMonthEnd))
                .isEqualTo(CHRONOLOGY.date(2024, 1, 29).plusDays(2));
        assertThat(period.subtractFrom(leapMonthEnd))
                .isEqualTo(CHRONOLOGY.date(2022, 3, 27));
        ChineseDate endOfYear = CHRONOLOGY.date(2023, 12, 30);
        ChinesePeriod year = CHRONOLOGY.period(1, 0, 0);
        assertThat(year.subtractFrom(year.addTo(endOfYear)))
                .isNotEqualTo(endOfYear);
    }

    @Test
    void serializesAndDeserializes() throws Exception {
        ChinesePeriod period = CHRONOLOGY.period(1, 2, 3);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(period);
        }

        ChinesePeriod deserialized;
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            deserialized = (ChinesePeriod) input.readObject();
        }

        assertThat(deserialized).isEqualTo(period);
        assertThat(deserialized.getChronology()).isSameAs(CHRONOLOGY);
    }

    @Test
    void rejectsSerializedPeriodWithMismatchedCalendarData() throws Exception {
        Class<?> proxyType = Class.forName(ChineseChronology.class.getName() + "$Ser");
        java.lang.reflect.Constructor<?> constructor = proxyType.getDeclaredConstructor(
                String.class, String.class, String.class);
        constructor.setAccessible(true);
        Object invalidChronology = constructor.newInstance("Chinese", "chinese", "invalid");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes) {
            {
                enableReplaceObject(true);
            }

            @Override
            protected Object replaceObject(Object object) throws IOException {
                return proxyType.isInstance(object) ? invalidChronology : object;
            }
        }) {
            output.writeObject(new ChinesePeriod(1, 2, 3));
        }

        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            assertThatThrownBy(input::readObject)
                    .isInstanceOf(InvalidObjectException.class)
                    .hasMessageContaining("fingerprint mismatch");
        }
    }

    @Test
    void operatesOnComponentsWithoutConvertingMonthsToYears() {
        ChinesePeriod period = CHRONOLOGY.period(1, 13, -3);
        assertThat(period.normalized()).isSameAs(period);
        assertThat(period.plus(CHRONOLOGY.period(2, -1, 4)))
                .isEqualTo(CHRONOLOGY.period(3, 12, 1));
        assertThat(period.minus(CHRONOLOGY.period(2, -1, 4)))
                .isEqualTo(CHRONOLOGY.period(-1, 14, -7));
        assertThat(period.multipliedBy(2)).isEqualTo(CHRONOLOGY.period(2, 26, -6));
        assertThat(period.negated()).isEqualTo(CHRONOLOGY.period(-1, -13, 3));
        assertThat(period.multipliedBy(0).isZero()).isTrue();
    }

    @Test
    void reusesInstancesWhenMultiplicationDoesNotChangeComponents() {
        ChinesePeriod period = CHRONOLOGY.period(Integer.MIN_VALUE, 13, -3);
        assertThat(period.multipliedBy(1)).isSameAs(period);
        ChinesePeriod zero = CHRONOLOGY.period(0, 0, 0);
        for (int scalar : new int[]{Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE}) {
            assertThat(zero.multipliedBy(scalar)).isSameAs(zero);
        }
    }

    @Test
    void appliesYearsThenActualMonthsThenDays() {
        ChineseDate start = CHRONOLOGY.date(2023, 1, 1);
        ChinesePeriod period = CHRONOLOGY.period(1, 1, 2);
        assertThat(start.plus(period)).isEqualTo(CHRONOLOGY.date(2024, 2, 3));
        assertThat(start.minus(period)).isEqualTo(start.plusYears(-1).plusMonths(-1).plusDays(-2));
        assertThat(period.addTo(start)).isEqualTo(start.plus(period));
        assertThat(period.subtractFrom(start)).isEqualTo(start.minus(period));
        assertThat(start.atTime(LocalTime.NOON).plus(period))
                .isEqualTo(CHRONOLOGY.date(2024, 2, 3).atTime(LocalTime.NOON));
        assertThat(start.plus(CHRONOLOGY.period(0, 0, 0))).isEqualTo(start);
        assertThat(start.plusMonths(12)).isNotEqualTo(start.plusYears(1));
        assertThat(start.plusMonths(13)).isEqualTo(start.plusYears(1));
    }

    @Test
    void rejectsOtherChronologiesAndInvalidAmounts() {
        ChinesePeriod period = CHRONOLOGY.period(1, 0, 0);
        assertThatThrownBy(() -> period.addTo(LocalDate.of(2024, 1, 1)))
                .isInstanceOf(DateTimeException.class).hasMessageContaining("Chronology mismatch");
        assertThatThrownBy(() -> CHRONOLOGY.period(0, 0, 0).subtractFrom(LocalDate.now()))
                .isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> period.plus(Period.ofYears(1)))
                .isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> period.minus(HijrahChronology.INSTANCE.period(1, 0, 0)))
                .isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> period.plus(Duration.ofDays(1)))
                .isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> period.plus(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> period.addTo(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void detectsComponentOverflow() {
        ChinesePeriod maximum = CHRONOLOGY.period(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
        for (ChinesePeriod increment : new ChinesePeriod[]{
                CHRONOLOGY.period(1, 0, 0), CHRONOLOGY.period(0, 1, 0), CHRONOLOGY.period(0, 0, 1)}) {
            assertThatThrownBy(() -> maximum.plus(increment)).isInstanceOf(ArithmeticException.class);
        }
        assertThatThrownBy(() -> maximum.multipliedBy(2)).isInstanceOf(ArithmeticException.class);
        assertThatThrownBy(() -> CHRONOLOGY.period(Integer.MIN_VALUE, 0, 0).negated())
                .isInstanceOf(ArithmeticException.class);
        assertThatThrownBy(() -> CHRONOLOGY.period(0, Integer.MIN_VALUE, 0)
                .minus(CHRONOLOGY.period(0, 1, 0))).isInstanceOf(ArithmeticException.class);
    }

    @Test
    void movesThroughLeapMonthsAndCommonYearBoundaries() {
        ChineseDate secondMonth = CHRONOLOGY.date(2023, 2, 1);
        ChineseDate leapMonth = CHRONOLOGY.date(2023, 2, true, 1);
        ChineseDate thirdMonth = CHRONOLOGY.date(2023, 3, 1);
        assertThat(secondMonth.plusMonths(1)).isEqualTo(leapMonth);
        assertThat(leapMonth.plusMonths(1)).isEqualTo(thirdMonth);
        assertThat(thirdMonth.minus(1, MONTHS)).isEqualTo(leapMonth);
        assertThat(leapMonth.minus(1, MONTHS)).isEqualTo(secondMonth);
        assertThat(CHRONOLOGY.date(2024, 12, 1).plusMonths(1))
                .isEqualTo(CHRONOLOGY.date(2025, 1, 1));
        assertThat(CHRONOLOGY.date(2025, 1, 1).plusMonths(-1))
                .isEqualTo(CHRONOLOGY.date(2024, 12, 1));
        assertThat(CHRONOLOGY.date(2024, 1, 1).until(CHRONOLOGY.date(2025, 1, 1), MONTHS))
                .isEqualTo(12);
        assertThat(CHRONOLOGY.date(2023, 1, 1).until(CHRONOLOGY.date(2024, 1, 1), MONTHS))
                .isEqualTo(13);
    }

    @Test
    void adjustsLeapStatusAndClampsDaysWhenMovingYears() {
        ChineseDate leapMonth = CHRONOLOGY.date(2023, 2, true, 1);
        assertThat(leapMonth.plusYears(1)).isEqualTo(CHRONOLOGY.date(2024, 2, 1));
        assertThat(leapMonth.with(YEAR, 2024)).isEqualTo(leapMonth.plusYears(1));
        assertThat(leapMonth.with(YEAR_OF_ERA, 2024)).isEqualTo(leapMonth.plusYears(1));
        assertThat(leapMonth.plusYears(-1)).isEqualTo(CHRONOLOGY.date(2022, 2, 1));
        assertThat(leapMonth.plusYears(19)).isEqualTo(CHRONOLOGY.date(2042, 2, true, 1));
        ChineseDate endOfYear = CHRONOLOGY.date(2023, 12, 30);
        ChineseDate target = endOfYear.plusYears(1);
        assertThat(target.getLong(DAY_OF_MONTH)).isEqualTo(target.lengthOfMonth());
        assertThat(CHRONOLOGY.date(2023, 1, 1).until(CHRONOLOGY.date(2024, 1, 1), YEARS))
                .isEqualTo(1);
        assertThat(leapMonth.until(CHRONOLOGY.date(2024, 2, 1), YEARS)).isEqualTo(1);
    }

    @Test
    void supportsStandardDateUnitsAndRejectsTimeUnits() {
        ChineseDate start = CHRONOLOGY.date(2023, 1, 1);
        assertThat(start.plus(1, WEEKS)).isEqualTo(start.plusDays(7));
        assertThat(start.plus(1, DECADES)).isEqualTo(start.plusYears(10));
        assertThat(CHRONOLOGY.date(1900, 1, 1).plus(1, CENTURIES))
                .isEqualTo(CHRONOLOGY.date(2000, 1, 1));
        assertThat(start.plus(0, MILLENNIA)).isEqualTo(start);
        assertThat(start.until(start.plusYears(10), DECADES)).isEqualTo(1);
        assertThat(start.plus(0, ERAS)).isEqualTo(start);
        assertThat(start.until(start.plusDays(10), ERAS)).isZero();
        assertThatThrownBy(() -> start.plus(1, ERAS)).isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> start.plus(1, HOURS))
                .isInstanceOf(UnsupportedTemporalTypeException.class);
        assertThatThrownBy(() -> start.plus(Period.ofDays(1))).isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> start.until(start, HOURS))
                .isInstanceOf(UnsupportedTemporalTypeException.class);
        assertThatThrownBy(() -> start.plus(Long.MAX_VALUE, WEEKS))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void rejectsOutOfRangeAdjustmentsWithoutWrappingOrUsingPaddingMonths() {
        ChineseDate minimum = CHRONOLOGY.date(1900, 1, 1);
        ChineseDate maximum = CHRONOLOGY.date(2099, 12, 1);
        maximum = maximum.plusDays(maximum.lengthOfMonth() - 1);
        ChineseDate lastDate = maximum;
        assertThatThrownBy(() -> minimum.plusMonths(-1)).isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> minimum.plusYears(-1)).isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> minimum.plusDays(-1)).isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> lastDate.plusMonths(1)).isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> lastDate.plusYears(1)).isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> lastDate.plusDays(1)).isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> minimum.plusDays(1L << 32)).isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> minimum.plusMonths(Long.MAX_VALUE)).isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> minimum.minus(Long.MIN_VALUE, DAYS)).isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> lastDate.plusYears(Long.MAX_VALUE)).isInstanceOf(ArithmeticException.class);
    }

    @Test
    void reconstructsIntervalsAcrossEverySupportedMonth() {
        ChineseDate minimum = CHRONOLOGY.date(1900, 1, 1);
        ChineseDate maximum = CHRONOLOGY.date(2099, 12, 1);
        long maximumDay = maximum.plusDays(maximum.lengthOfMonth() - 1).toEpochDay();
        ChineseDate month = minimum;
        while (true) {
            for (ChineseDate start : new ChineseDate[]{month, month.plusDays(month.lengthOfMonth() - 1)}) {
                for (int offset : new int[]{-800, -400, -60, -30, -1, 0, 1, 30, 60, 400, 800}) {
                    long targetDay = start.toEpochDay() + offset;
                    if (targetDay < minimum.toEpochDay() || targetDay > maximumDay) {
                        continue;
                    }
                    ChineseDate end = CHRONOLOGY.dateEpochDay(targetDay);
                    ChinesePeriod period = start.until(end);
                    assertThat(start.plus(period)).as("%s to %s via %s", start, end, period).isEqualTo(end);
                    assertThat(ChronoPeriod.between(start, end)).isEqualTo(period);
                    assertThat(start.plus(period.normalized())).isEqualTo(end);
                }
            }
            if (month.prolepticYear() == 2099 && month.monthOfYear() == 12) {
                break;
            }
            ChineseDate next = month.plusMonths(1);
            assertThat(month.until(next, MONTHS)).isEqualTo(1);
            assertThat(next.plusMonths(-1)).isEqualTo(month);
            ChineseDate monthEnd = month.plusDays(month.lengthOfMonth() - 1);
            ChineseDate adjusted = monthEnd.plusMonths(1);
            assertThat(adjusted.getLong(DAY_OF_MONTH))
                    .isEqualTo(Math.min(month.lengthOfMonth(), next.lengthOfMonth()));
            assertThat(adjusted.with(DAY_OF_MONTH, 1)).isEqualTo(next);
            month = next;
        }
        assertThat(minimum.plus(minimum.until(maximum))).isEqualTo(maximum);
        assertThat(maximum.plus(maximum.until(minimum))).isEqualTo(minimum);
    }

    @Test
    void reconstructsIntervalsFromEverySupportedDay() {
        ChineseDate minimum = CHRONOLOGY.date(1900, 1, 1);
        ChineseDate lastMonth = CHRONOLOGY.date(2099, 12, 1);
        long maximumDay = lastMonth.plusDays(lastMonth.lengthOfMonth() - 1).toEpochDay();
        for (long epochDay = minimum.toEpochDay(); epochDay <= maximumDay; epochDay++) {
            ChineseDate start = CHRONOLOGY.dateEpochDay(epochDay);
            for (int offset : new int[]{-400, -31, 31, 400}) {
                long targetDay = epochDay + offset;
                if (targetDay >= minimum.toEpochDay() && targetDay <= maximumDay) {
                    ChineseDate end = CHRONOLOGY.dateEpochDay(targetDay);
                    assertThat(start.plus(start.until(end))).as("%s to %s", start, end).isEqualTo(end);
                }
            }
        }
    }

    private record UnitAmount(TemporalUnit unit, long value) implements TemporalAmount {
        @Override
        public long get(TemporalUnit unit) {
            if (!this.unit.equals(unit)) {
                throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
            }
            return value;
        }

        @Override
        public List<TemporalUnit> getUnits() {
            return List.of(unit);
        }

        @Override
        public Temporal addTo(Temporal temporal) {
            return temporal.plus(value, unit);
        }

        @Override
        public Temporal subtractFrom(Temporal temporal) {
            return temporal.minus(value, unit);
        }
    }
}
