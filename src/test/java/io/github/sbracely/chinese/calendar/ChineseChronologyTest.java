package io.github.sbracely.chinese.calendar;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.chrono.IsoEra;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.time.temporal.ValueRange;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChineseChronologyTest {
    private static final ChineseChronology CHRONOLOGY = ChineseChronology.INSTANCE;
    private static final String CALENDAR_DATA_RESOURCE = "calendar-data.csv";

    @Test
    void exposesConfiguredIdentity() {
        assertThat(CHRONOLOGY.getId()).isEqualTo("Chinese");
        assertThat(CHRONOLOGY.getCalendarType()).isEqualTo("chinese");
    }

    @Test
    void createsDatesFromChineseFieldsAcrossSupportedRange() throws IOException {
        int minimumYear = Math.toIntExact(CHRONOLOGY.range(ChronoField.YEAR).getMinimum());
        int maximumYear = Math.toIntExact(CHRONOLOGY.range(ChronoField.YEAR).getMaximum());
        ChineseDate minimumDate = CHRONOLOGY.date(minimumYear, 1, 1);
        ChineseDate firstDateOfMaximumYear = CHRONOLOGY.date(maximumYear, 1, 1);
        ChineseDate maximumDate = CHRONOLOGY.dateYearDay(
                maximumYear, firstDateOfMaximumYear.lengthOfYear());
        LocalDate minimumIsoDate = LocalDate.ofEpochDay(minimumDate.toEpochDay());
        LocalDate maximumIsoDate = LocalDate.ofEpochDay(maximumDate.toEpochDay());

        int tested = 0;
        LocalDate expectedIsoDate = minimumIsoDate;
        try (InputStream input = ChineseChronologyTest.class.getClassLoader()
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
                    int year = Integer.parseInt(chineseDateParts[0]);
                    int month = Integer.parseInt(chineseDateParts[1]);
                    boolean leapMonth = "闰".equals(columns[3]);
                    int day = Integer.parseInt(chineseDateParts[2]);

                    assertThat(isoDate)
                            .as("continuous calendar data at %s", expectedIsoDate)
                            .isEqualTo(expectedIsoDate);
                    assertChineseDate(CHRONOLOGY.date(year, month, leapMonth, day),
                            year, month, leapMonth, day);
                    assertChineseDate(CHRONOLOGY.date(
                                    ChineseEra.CHINESE_ERA, year, month, leapMonth, day),
                            year, month, leapMonth, day);

                    expectedIsoDate = expectedIsoDate.plusDays(1);
                    tested++;
                }
            }
        }

        long expectedDays = maximumIsoDate.toEpochDay() - minimumIsoDate.toEpochDay() + 1;
        assertThat(tested).isEqualTo(Math.toIntExact(expectedDays));
        assertThat(expectedIsoDate).isEqualTo(maximumIsoDate.plusDays(1));
    }

    @Test
    void createsDateFromDayOfYear() {
        ChineseDate newYear = CHRONOLOGY.date(2023, 1, 1);

        assertThat(CHRONOLOGY.dateYearDay(2023, 1)).isEqualTo(newYear);
        assertThat(CHRONOLOGY.dateYearDay(ChineseEra.CHINESE_ERA, 2023, 60))
                .isEqualTo(CHRONOLOGY.date(2023, 2, true, 1));
        assertThat(CHRONOLOGY.dateYearDay(2023, newYear.lengthOfYear()))
                .isEqualTo(CHRONOLOGY.date(2023, 12, 30));
    }

    @Test
    void calculatesEpochSecondForLeapMonthDates() {
        ChineseDate leapMonthDate = CHRONOLOGY.date(2023, 2, true, 1);
        ZoneOffset zoneOffset = ZoneOffset.ofHours(8);
        long expected = LocalDate.ofEpochDay(leapMonthDate.toEpochDay())
                .atTime(12, 34, 56)
                .toEpochSecond(zoneOffset);

        assertThat(CHRONOLOGY.epochSecond(2023, 2, true, 1, 12, 34, 56, zoneOffset))
                .isEqualTo(expected);
        assertThat(CHRONOLOGY.epochSecond(
                ChineseEra.CHINESE_ERA, 2023, 2, true, 1, 12, 34, 56, zoneOffset))
                .isEqualTo(expected);
        assertThat(CHRONOLOGY.epochSecond(2023, 2, false, 1, 12, 34, 56, zoneOffset))
                .isNotEqualTo(expected);
    }

    @Test
    void rejectsInvalidDayOfYear() {
        int yearLength = CHRONOLOGY.date(2023, 1, 1).lengthOfYear();

        assertThatThrownBy(() -> CHRONOLOGY.dateYearDay(2023, 0))
                .isInstanceOf(DateTimeException.class)
                .hasMessage("Invalid day of year: 0");
        assertThatThrownBy(() -> CHRONOLOGY.dateYearDay(2023, yearLength + 1))
                .isInstanceOf(DateTimeException.class)
                .hasMessage("Invalid day of year: " + (yearLength + 1));
    }

    @Test
    void reportsLeapYearsAndEra() {
        assertThat(CHRONOLOGY.isLeapYear(2023)).isTrue();
        assertThat(CHRONOLOGY.isLeapYear(2024)).isFalse();
        assertThat(CHRONOLOGY.prolepticYear(ChineseEra.CHINESE_ERA, 2024)).isEqualTo(2024);
        assertThat(CHRONOLOGY.eraOf(1)).isEqualTo(ChineseEra.CHINESE_ERA);
        assertThat(CHRONOLOGY.eras()).containsExactly(ChineseEra.CHINESE_ERA);
    }

    @Test
    void rejectsUnsupportedEra() {
        assertThatThrownBy(() -> CHRONOLOGY.prolepticYear(IsoEra.CE, 2024))
                .isInstanceOf(ClassCastException.class)
                .hasMessageContaining("Era must be ChineseEra");
        assertThatThrownBy(() -> CHRONOLOGY.eraOf(0))
                .isInstanceOf(DateTimeException.class)
                .hasMessage("Invalid Chinese era: 0");
    }

    @Test
    void exposesConfiguredFieldRanges() {
        assertThat(CHRONOLOGY.range(ChronoField.DAY_OF_MONTH))
                .isEqualTo(ValueRange.of(1, 29, 30));
        assertThat(CHRONOLOGY.range(ChronoField.YEAR))
                .isEqualTo(ValueRange.of(1900, 2099));
        assertThat(CHRONOLOGY.range(ChronoField.YEAR_OF_ERA))
                .isEqualTo(ValueRange.of(1900, 2099));
        assertThat(CHRONOLOGY.range(ChronoField.ERA))
                .isEqualTo(ValueRange.of(1, 1));
        assertThat(CHRONOLOGY.range(ChronoField.DAY_OF_WEEK))
                .isEqualTo(ChronoField.DAY_OF_WEEK.range());
    }

    @Test
    void resolvesDateFields() {
        Map<TemporalField, Long> fields = new HashMap<>();
        fields.put(ChronoField.YEAR, 2024L);
        fields.put(ChronoField.MONTH_OF_YEAR, 1L);
        fields.put(ChronoField.DAY_OF_MONTH, 1L);

        ChineseDate resolved = CHRONOLOGY.resolveDate(fields, ResolverStyle.STRICT);

        assertChineseDate(resolved, 2024, 1, false, 1);
        assertThat(fields).isEmpty();
    }

    @Test
    void rejectsInvalidDates() {
        assertThatThrownBy(() -> CHRONOLOGY.date(2024, 0, 1))
                .isInstanceOf(DateTimeException.class)
                .hasMessage("Invalid value for MonthOfYear (valid values 1 - 12): 0");
        assertThatThrownBy(() -> CHRONOLOGY.date(2024, 13, 1))
                .isInstanceOf(DateTimeException.class)
                .hasMessage("Invalid value for MonthOfYear (valid values 1 - 12): 13");
        assertThatThrownBy(() -> CHRONOLOGY.date(2024, 1, 0))
                .isInstanceOf(DateTimeException.class)
                .hasMessage("Invalid Chinese day of month: 0");
        assertThatThrownBy(() -> CHRONOLOGY.date(2024, 2, true, 1))
                .isInstanceOf(DateTimeException.class)
                .hasMessage("Invalid leap month: 2 in year 2024");
        assertThatThrownBy(() -> CHRONOLOGY.date(2100, 1, 1))
                .isInstanceOf(DateTimeException.class)
                .hasMessage("Invalid value for Year (valid values 1900 - 2099): 2100");
    }

    private static void assertChineseDate(
            ChineseDate actual, int year, int month, boolean leapMonth, int day) {
        assertThat(actual.getLong(ChronoField.YEAR)).isEqualTo(year);
        assertThat(actual.getLong(ChronoField.MONTH_OF_YEAR)).isEqualTo(month);
        assertThat(actual.isLeapMonth()).isEqualTo(leapMonth);
        assertThat(actual.getLong(ChronoField.DAY_OF_MONTH)).isEqualTo(day);
    }
}
