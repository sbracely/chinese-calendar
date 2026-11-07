package io.github.sbracely.chinese.calendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalQueries;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChineseDateConversionTest {
    private static final ChineseChronology CHRONOLOGY = ChineseChronology.INSTANCE;
    private static final String CALENDAR_DATA_RESOURCE = "calendar-data.csv";

    @Test
    void usesSharedChronologyForDatesCreatedByAnotherInstance() {
        ChineseDate date = new ChineseChronology().date(2023, 2, true, 1);
        ChineseDate expected = CHRONOLOGY.date(2023, 2, true, 1);

        assertThat(date.getChronology()).isSameAs(CHRONOLOGY);
        assertThat(date.query(TemporalQueries.chronology())).isSameAs(CHRONOLOGY);
        assertThat(date).isEqualTo(expected);
        assertThat(date.hashCode()).isEqualTo(expected.hashCode());
        assertThat(date.plusMonths(1)).isEqualTo(expected.plusMonths(1));
    }

    @ParameterizedTest
    @MethodSource("calendarDates")
    void convertsGregorianTemporalsUsingFrom(int year, int month, boolean leapMonth, int day,
                                            String isoDate, boolean leapYear, int ordinalMonth) {
        LocalDate gregorianDate = LocalDate.parse(isoDate);
        ChineseDate expected = CHRONOLOGY.date(year, month, leapMonth, day);

        assertThat(ChineseDate.from(gregorianDate)).isEqualTo(expected);
        assertThat(ChineseDate.from(gregorianDate.atTime(12, 0))).isEqualTo(expected);
    }

    @Test
    void returnsExistingChineseDateUnchangedUsingFrom() {
        ChineseDate date = CHRONOLOGY.date(2023, 2, true, 1);

        assertThat(ChineseDate.from(date)).isSameAs(date);
    }

    @Test
    void rejectsInputsWithoutSupportedDateUsingFrom() {
        assertThatThrownBy(() -> ChineseDate.from(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ChineseDate.from(LocalTime.NOON))
                .isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> ChineseDate.from(LocalDate.of(1800, 1, 1)))
                .isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> ChineseDate.from(LocalDate.of(2101, 1, 1)))
                .isInstanceOf(DateTimeException.class);
    }

    @Test
    void representsDatesWithPaddedFieldsAndDistinctLeapMonths() {
        assertThat(CHRONOLOGY.date(2023, 2, 1).toString())
                .isEqualTo("Chinese 2023-02-01");
        assertThat(CHRONOLOGY.date(2023, 2, true, 1).toString())
                .isEqualTo("Chinese 2023-02bis-01");
        assertThat(CHRONOLOGY.date(2025, 6, 1).toString())
                .isEqualTo("Chinese 2025-06-01");
        assertThat(CHRONOLOGY.date(2025, 6, true, 1).toString())
                .isEqualTo("Chinese 2025-06bis-01");
        assertThat(CHRONOLOGY.date(2023, 12, 30).toString())
                .isEqualTo("Chinese 2023-12-30");
    }

    @Test
    void adjustsWithStandardTemporalAdjusters() {
        ChineseDate date = CHRONOLOGY.date(2023, 2, 10);

        ChineseDate firstDayOfMonth = date.with(TemporalAdjusters.firstDayOfMonth());
        ChineseDate lastDayOfMonth = date.with(TemporalAdjusters.lastDayOfMonth());
        ChineseDate firstDayOfYear = date.with(TemporalAdjusters.firstDayOfYear());

        assertThat(firstDayOfMonth).isEqualTo(CHRONOLOGY.date(2023, 2, 1));
        assertThat(lastDayOfMonth).isEqualTo(CHRONOLOGY.date(2023, 2, date.lengthOfMonth()));
        assertThat(firstDayOfYear).isEqualTo(CHRONOLOGY.dateYearDay(2023, 1));
    }

    @Test
    void adjustsWithinLeapMonth() {
        ChineseDate leapMonth = CHRONOLOGY.date(2017, 6, true, 10);

        ChineseDate adjusted = leapMonth.with(TemporalAdjusters.lastDayOfMonth());

        assertThat(adjusted)
                .isEqualTo(CHRONOLOGY.date(2017, 6, true, leapMonth.lengthOfMonth()));
        assertThat(adjusted.isLeapMonth()).isTrue();
    }

    @Test
    void adjustsToYearBoundariesInLeapMonthYear() {
        ChineseDate date = CHRONOLOGY.date(2017, 6, true, 10);

        ChineseDate firstDayOfYear = date.with(TemporalAdjusters.firstDayOfYear());
        ChineseDate lastDayOfYear = date.with(TemporalAdjusters.lastDayOfYear());

        assertThat(firstDayOfYear).isEqualTo(CHRONOLOGY.dateYearDay(2017, 1));
        assertThat(lastDayOfYear)
                .isEqualTo(CHRONOLOGY.dateYearDay(2017, date.lengthOfYear()));
        assertThat(lastDayOfYear.isLeapMonth()).isFalse();
    }

    @Test
    void appliesCustomTemporalAdjusterAndPreservesChineseDateType() {
        ChineseDate date = CHRONOLOGY.date(2023, 2, 10);

        ChineseDate nextDay = date.with(temporal -> temporal.plus(1, ChronoUnit.DAYS));
        ChineseDate unchanged = date.with(temporal -> temporal);

        assertThat(nextDay).isEqualTo(CHRONOLOGY.date(2023, 2, 11));
        assertThat(unchanged).isSameAs(date);
    }

    @Test
    void rejectsNullTemporalAdjuster() {
        assertThatThrownBy(() -> CHRONOLOGY.date(2023, 2, 10).with(null))
                .isInstanceOf(NullPointerException.class);
    }

    static Stream<Arguments> calendarDates() {
        return Stream.of(
                Arguments.of(2023, 2, false, 1, "2023-02-20", true, 2),
                Arguments.of(2023, 2, true, 1, "2023-03-22", true, 3),
                Arguments.of(2023, 3, false, 1, "2023-04-20", true, 4),
                Arguments.of(2023, 12, false, 30, "2024-02-09", true, 13),
                Arguments.of(2024, 1, false, 1, "2024-02-10", false, 1),
                Arguments.of(2024, 12, false, 29, "2025-01-28", false, 12));
    }

    @ParameterizedTest
    @MethodSource("calendarDates")
    void exposesEraLeapYearAndQueries(int year, int month, boolean leapMonth, int day,
                                     String isoDate, boolean leapYear, int ordinalMonth) {
        ChineseDate date = CHRONOLOGY.date(year, month, leapMonth, day);

        assertThat(date.getEra()).isSameAs(ChineseEra.CHINESE_ERA);
        assertThat(date.isLeapYear()).isEqualTo(leapYear);
        assertThat(date.query(TemporalQueries.chronology())).isSameAs(CHRONOLOGY);
        assertThat(date.query(TemporalQueries.precision())).isEqualTo(ChronoUnit.DAYS);
        assertThat(date.query(TemporalQueries.localDate())).isEqualTo(LocalDate.parse(isoDate));
        assertThat(date.query(TemporalQueries.localTime())).isNull();
        assertThat(date.query(TemporalQueries.zone())).isNull();
        assertThat(date.query(TemporalQueries.offset())).isNull();
        long queriedMonth = date.query(
                temporal -> temporal.getLong(ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR));
        assertThat(queriedMonth).isEqualTo(ordinalMonth);
    }

    @ParameterizedTest
    @MethodSource("calendarDates")
    void supportsDateUnitsButNotTimeOrIsoSpecificUnits(int year, int month, boolean leapMonth, int day,
                                                      String isoDate, boolean leapYear, int ordinalMonth) {
        ChineseDate date = CHRONOLOGY.date(year, month, leapMonth, day);

        for (ChronoUnit unit : ChronoUnit.values()) {
            assertThat(date.isSupported(unit)).as("%s on %s", unit, date)
                    .isEqualTo(unit.isDateBased());
        }
        assertThat(date.isSupported(IsoFields.QUARTER_YEARS)).isFalse();
    }

    @ParameterizedTest
    @MethodSource("calendarDates")
    void adjustsOtherDatesAndCombinesWithTime(int year, int month, boolean leapMonth, int day,
                                             String isoDate, boolean leapYear, int ordinalMonth) {
        ChineseDate date = CHRONOLOGY.date(year, month, leapMonth, day);
        LocalDate expected = LocalDate.parse(isoDate);

        assertThat(date.adjustInto(LocalDate.of(2000, 1, 1))).isEqualTo(expected);
        assertThat(date.adjustInto(CHRONOLOGY.date(2000, 1, 1))).isEqualTo(date);
        var dateTime = date.atTime(LocalTime.of(23, 59, 59));
        assertThat(dateTime.toLocalDate()).isEqualTo(date);
        assertThat(dateTime.toLocalTime()).isEqualTo(LocalTime.of(23, 59, 59));
        assertThat(dateTime.getChronology()).isSameAs(CHRONOLOGY);
        assertThat(LocalDate.from(dateTime)).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource("calendarDates")
    void formatsRegularAndLeapMonthFields(int year, int month, boolean leapMonth, int day,
                                         String isoDate, boolean leapYear, int ordinalMonth) {
        ChineseDate date = CHRONOLOGY.date(year, month, leapMonth, day);
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendPattern("uuuu-MM-dd")
                .appendLiteral('/')
                .appendValue(ChineseTemporalField.IS_LEAP_MONTH)
                .appendLiteral('/')
                .appendValue(ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR)
                .toFormatter(Locale.ROOT);

        assertThat(date.format(formatter)).isEqualTo(
                String.format(Locale.ROOT, "%04d-%02d-%02d/%d/%d",
                        year, month, day, leapMonth ? 1 : 0, ordinalMonth));
    }

    @ParameterizedTest
    @MethodSource("calendarDates")
    void adjustsYearBoundariesBeforeWithinAndAfterLeapMonth(int year, int month, boolean leapMonth, int day,
                                                           String isoDate, boolean leapYear, int ordinalMonth) {
        ChineseDate date = CHRONOLOGY.date(year, month, leapMonth, day);
        LocalDate yearStart = LocalDate.parse(year == 2023 ? "2023-01-22" : "2024-02-10");
        LocalDate nextYearStart = LocalDate.parse(year == 2023 ? "2024-02-10" : "2025-01-29");

        assertThat(LocalDate.from(date.with(TemporalAdjusters.firstDayOfYear()))).isEqualTo(yearStart);
        assertThat(LocalDate.from(date.with(TemporalAdjusters.lastDayOfYear())))
                .isEqualTo(nextYearStart.minusDays(1));
        assertThat(LocalDate.from(date.with(TemporalAdjusters.firstDayOfNextYear())))
                .isEqualTo(nextYearStart);
        assertThat(date.getDayOfYear())
                .isEqualTo(Math.toIntExact(ChronoUnit.DAYS.between(yearStart, LocalDate.parse(isoDate)) + 1));
    }

    @ParameterizedTest
    @MethodSource("calendarDates")
    void comparesSameDayAcrossChronologies(int year, int month, boolean leapMonth, int day,
                                          String isoDate, boolean leapYear, int ordinalMonth) {
        ChineseDate date = CHRONOLOGY.date(year, month, leapMonth, day);
        LocalDate sameDay = LocalDate.parse(isoDate);

        assertThat(date.isEqual(sameDay)).isTrue();
        assertThat(date.isBefore(sameDay)).isFalse();
        assertThat(date.isAfter(sameDay)).isFalse();
        assertThat(date.compareTo(sameDay)).isNotZero();
        assertThat(Integer.signum(date.compareTo(sameDay)))
                .isEqualTo(-Integer.signum(sameDay.compareTo(date)));
        assertThat(date.compareTo(CHRONOLOGY.date(year, month, leapMonth, day))).isZero();
        assertThat(date.isBefore(sameDay.plusDays(1))).isTrue();
        assertThat(date.isAfter(sameDay.minusDays(1))).isTrue();
    }

    @Test
    void ordersDatesAcrossLeapMonthAndYearBoundaries() {
        ChineseDate[][] boundaries = {
                {CHRONOLOGY.date(2023, 2, 30), CHRONOLOGY.date(2023, 2, true, 1)},
                {CHRONOLOGY.date(2023, 2, true, 29), CHRONOLOGY.date(2023, 3, 1)},
                {CHRONOLOGY.date(2023, 12, 30), CHRONOLOGY.date(2024, 1, 1)},
                {CHRONOLOGY.date(2024, 12, 29), CHRONOLOGY.date(2025, 1, 1)}
        };
        for (ChineseDate[] boundary : boundaries) {
            ChineseDate before = boundary[0];
            ChineseDate after = boundary[1];
            assertThat(after.toEpochDay() - before.toEpochDay()).isEqualTo(1);
            assertThat(before.compareTo(after)).isNegative();
            assertThat(after.compareTo(before)).isPositive();
            assertThat(before.isBefore(after)).isTrue();
            assertThat(before.isAfter(after)).isFalse();
            assertThat(after.isAfter(before)).isTrue();
            assertThat(after.isBefore(before)).isFalse();
            assertThat(before.isEqual(after)).isFalse();
        }
    }

    @Test
    void convertsBetweenIsoAndChineseDatesAcrossSupportedRange() throws IOException {
        int minimumYear = (int) CHRONOLOGY.range(ChronoField.YEAR).getMinimum();
        int maximumYear = (int) CHRONOLOGY.range(ChronoField.YEAR).getMaximum();
        ChineseDate minimumDate = CHRONOLOGY.date(minimumYear, 1, 1);
        ChineseDate firstDateOfMaximumYear = CHRONOLOGY.date(maximumYear, 1, 1);
        ChineseDate maximumDate = CHRONOLOGY.dateYearDay(
                maximumYear, firstDateOfMaximumYear.lengthOfYear());
        LocalDate minimumIsoDate = LocalDate.ofEpochDay(minimumDate.toEpochDay());
        LocalDate maximumIsoDate = LocalDate.ofEpochDay(maximumDate.toEpochDay());

        int tested = 0;
        LocalDate expectedIsoDate = minimumIsoDate;
        try (InputStream input = ChineseDateConversionTest.class.getClassLoader()
                .getResourceAsStream(CALENDAR_DATA_RESOURCE)) {
            assertThat(input)
                    .as("calendar data resource %s", CALENDAR_DATA_RESOURCE)
                    .isNotNull();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(input, StandardCharsets.UTF_8))) {
                assertThat(reader.readLine()).isEqualTo("公历,干支,农历,闰月,节气");

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank()) {
                        continue;
                    }
                    String[] columns = line.split(",", -1);
                    assertThat(columns)
                            .as("calendar data row: %s", line)
                            .hasSize(5);
                    LocalDate isoDate = LocalDate.parse(columns[0]);
                    if (isoDate.isBefore(minimumIsoDate) || isoDate.isAfter(maximumIsoDate)) {
                        continue;
                    }

                    String[] chineseDateParts = columns[2].split("-");
                    ChineseDate chineseDate = CHRONOLOGY.date(
                            Integer.parseInt(chineseDateParts[0]),
                            Integer.parseInt(chineseDateParts[1]),
                            "闰".equals(columns[3]),
                            Integer.parseInt(chineseDateParts[2]));

                    assertThat(isoDate)
                            .as("continuous calendar data at %s", expectedIsoDate)
                            .isEqualTo(expectedIsoDate);
                    assertThat(CHRONOLOGY.date(isoDate))
                            .as("Chinese date for ISO date %s", isoDate)
                            .isEqualTo(chineseDate);
                    assertThat(LocalDate.ofEpochDay(chineseDate.toEpochDay()))
                            .as("ISO date for Chinese date %s", chineseDate)
                            .isEqualTo(isoDate);

                    expectedIsoDate = expectedIsoDate.plusDays(1);
                    tested++;
                }
            }
        }

        long expectedDays = maximumIsoDate.toEpochDay() - minimumIsoDate.toEpochDay() + 1;
        assertThat(tested).isEqualTo(Math.toIntExact(expectedDays));
        assertThat(expectedIsoDate).isEqualTo(maximumIsoDate.plusDays(1));
    }
}
