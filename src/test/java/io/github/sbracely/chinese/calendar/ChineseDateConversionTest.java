package io.github.sbracely.chinese.calendar;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.temporal.ChronoField;

import static org.assertj.core.api.Assertions.assertThat;

class ChineseDateConversionTest {
    private static final ChineseChronology CHRONOLOGY = ChineseChronology.INSTANCE;
    private static final String CALENDAR_DATA_RESOURCE = "calendar-data.csv";

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
