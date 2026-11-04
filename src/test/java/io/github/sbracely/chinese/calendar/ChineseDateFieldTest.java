package io.github.sbracely.chinese.calendar;

import org.junit.jupiter.api.Test;

import java.time.temporal.ChronoField;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChineseDateFieldTest {
    private static final ChineseChronology CHRONOLOGY = ChineseChronology.INSTANCE;

    @Test
    void chainsFieldAdjustmentsWithChineseSpecificReturnTypes() {
        ChineseDate adjusted = CHRONOLOGY.date(2023, 2, 10)
                .with(ChineseTemporalField.IS_LEAP_MONTH, 1)
                .with(ChronoField.DAY_OF_MONTH, 1);
        ChineseChronology chronology = adjusted.getChronology();

        assertThat(adjusted).isEqualTo(CHRONOLOGY.date(2023, 2, true, 1));
        assertThat(chronology).isSameAs(CHRONOLOGY);
    }

    @Test
    void preservesSupportedFieldContractsAcrossCommonAndLeapYears() {
        int minimumYear = (int) CHRONOLOGY.range(ChronoField.YEAR).getMinimum();
        int maximumYear = (int) CHRONOLOGY.range(ChronoField.YEAR).getMaximum();
        for (int year = minimumYear; year <= maximumYear; year++) {
            ChineseDate first = CHRONOLOGY.date(year, 1, 1);
            for (int day = 1; day <= first.lengthOfYear(); day++) {
                ChineseDate date = first.plusDays(day - 1);
                for (ChronoField field : ChronoField.values()) {
                    if (date.isSupported(field)) {
                        long value = date.getLong(field);
                        assertThat(date.range(field).isValidValue(value))
                                .as("%s range on %s", field, date).isTrue();
                        assertThat(date.with(field, value))
                                .as("%s identity on %s", field, date).isEqualTo(date);
                    }
                }
                for (ChineseTemporalField field : ChineseTemporalField.values()) {
                    assertThat(date.range(field).isValidValue(date.getLong(field))).isTrue();
                    assertThat(date.with(field, date.getLong(field))).isEqualTo(date);
                }
            }
        }
    }

    @Test
    void rejectsUnsupportedFieldsConsistently() {
        ChineseDate date = CHRONOLOGY.date(2023, 2, true, 1);
        for (ChronoField field : ChronoField.values()) {
            if (!date.isSupported(field)) {
                assertThatThrownBy(() -> date.range(field))
                        .isInstanceOf(UnsupportedTemporalTypeException.class);
                assertThatThrownBy(() -> date.getLong(field))
                        .isInstanceOf(UnsupportedTemporalTypeException.class);
                assertThatThrownBy(() -> date.with(field, field.range().getMinimum()))
                        .isInstanceOf(UnsupportedTemporalTypeException.class);
            }
        }
    }

    @Test
    void adjustsAlignedWeeksBeyondIsoYearRange() {
        ChineseDate start = CHRONOLOGY.date(2023, 1, 1);
        assertThat(CHRONOLOGY.range(ChronoField.ALIGNED_WEEK_OF_YEAR).getMaximum())
                .isGreaterThanOrEqualTo(55);
        for (int week : new int[]{54, 55}) {
            ChineseDate expected = start.plusDays((week - 1L) * 7);
            assertThat(start.with(ChronoField.ALIGNED_WEEK_OF_YEAR, week)).isEqualTo(expected);
            assertThat(expected.range(ChronoField.ALIGNED_WEEK_OF_YEAR))
                    .isEqualTo(ValueRange.of(1, 55));
            assertThat(expected.with(ChronoField.ALIGNED_DAY_OF_WEEK_IN_YEAR, 2))
                    .isEqualTo(expected.plusDays(1));
        }
    }
}
