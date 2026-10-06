package io.github.sbracely.chinese.calendar;

import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChineseTemporalFieldTest {
    private static final ChineseChronology CHRONOLOGY = ChineseChronology.INSTANCE;

    @Test
    void exposesFieldMetadata() {
        assertThat(ChineseTemporalField.IS_LEAP_MONTH.getDisplayName(Locale.ENGLISH))
                .isEqualTo("IsLeapMonth");
        assertThat(ChineseTemporalField.IS_LEAP_MONTH.getBaseUnit())
                .isEqualTo(ChronoUnit.MONTHS);
        assertThat(ChineseTemporalField.IS_LEAP_MONTH.getRangeUnit())
                .isEqualTo(ChronoUnit.MONTHS);
        assertThat(ChineseTemporalField.IS_LEAP_MONTH.range())
                .isEqualTo(ValueRange.of(0, 1));

        assertThat(ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR.getDisplayName(Locale.ENGLISH))
                .isEqualTo("MonthAsOrdinalOfYear");
        assertThat(ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR.getBaseUnit())
                .isEqualTo(ChronoUnit.MONTHS);
        assertThat(ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR.getRangeUnit())
                .isEqualTo(ChronoUnit.YEARS);
        assertThat(ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR.range())
                .isEqualTo(ValueRange.of(1, 12, 13));
    }

    @Test
    void identifiesAsDateBasedFields() {
        assertThat(ChineseTemporalField.values())
                .allSatisfy(field -> {
                    assertThat(field.isDateBased()).isTrue();
                    assertThat(field.isTimeBased()).isFalse();
                });
    }

    @Test
    void supportsOnlyChineseDates() {
        ChineseDate chineseDate = CHRONOLOGY.date(2023, 2, true, 1);

        assertThat(ChineseTemporalField.values())
                .allSatisfy(field -> {
                    assertThat(field.isSupportedBy(chineseDate)).isTrue();
                    assertThat(field.isSupportedBy(LocalDate.now())).isFalse();
                });
    }

    @Test
    void refinesRangesForLeapAndCommonYears() {
        ChineseDate leapYear = CHRONOLOGY.date(2023, 2, true, 1);
        ChineseDate commonYear = CHRONOLOGY.date(2024, 1, 1);

        assertThat(ChineseTemporalField.IS_LEAP_MONTH.rangeRefinedBy(leapYear))
                .isEqualTo(ValueRange.of(0, 1));
        assertThat(ChineseTemporalField.IS_LEAP_MONTH.rangeRefinedBy(commonYear))
                .isEqualTo(ValueRange.of(0, 0));
        assertThat(ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR.rangeRefinedBy(leapYear))
                .isEqualTo(ValueRange.of(1, 13));
        assertThat(ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR.rangeRefinedBy(commonYear))
                .isEqualTo(ValueRange.of(1, 12));
    }

    @Test
    void readsValuesFromChineseDates() {
        ChineseDate leapMonth = CHRONOLOGY.date(2023, 2, true, 1);

        assertThat(ChineseTemporalField.IS_LEAP_MONTH.getFrom(leapMonth)).isEqualTo(1);
        assertThat(ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR.getFrom(leapMonth)).isEqualTo(3);
    }

    @Test
    void adjustsChineseDates() {
        ChineseDate leapMonth = CHRONOLOGY.date(2023, 2, true, 1);

        assertThat(ChineseTemporalField.IS_LEAP_MONTH.adjustInto(leapMonth, 0))
                .isEqualTo(CHRONOLOGY.date(2023, 2, 1));
        assertThat(ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR.adjustInto(leapMonth, 4))
                .isEqualTo(CHRONOLOGY.date(2023, 3, 1));
    }

    @Test
    void rejectsInvalidOrUnsupportedValues() {
        ChineseDate commonYear = CHRONOLOGY.date(2024, 1, 1);

        assertThatThrownBy(() ->
                ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR.adjustInto(commonYear, 13))
                .isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() ->
                ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR.rangeRefinedBy(LocalDate.now()))
                .isInstanceOf(UnsupportedTemporalTypeException.class);
        assertThatThrownBy(() ->
                ChineseTemporalField.IS_LEAP_MONTH.getFrom(LocalDate.now()))
                .isInstanceOf(UnsupportedTemporalTypeException.class);
    }
}
