package io.github.sbracely.chinese.calendar;

import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalQueries;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.time.temporal.ValueRange;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChineseEraTest {
    private static final ChineseEra ERA = ChineseEra.CHINESE_ERA;
    private static final ChineseChronology CHRONOLOGY = ChineseChronology.INSTANCE;

    @Test
    void exposesOnlyTheSingleSupportedEra() {
        assertThat(ChineseEra.values()).containsExactly(ERA);
        assertThat(ChineseEra.of(1)).isSameAs(ERA);
        assertThat(ERA.getValue()).isEqualTo(1);
        assertThat(ERA.getLong(ChronoField.ERA)).isEqualTo(1);
        assertThat(ERA.range(ChronoField.ERA)).isEqualTo(ValueRange.of(1, 1));
        for (int value : new int[]{Integer.MIN_VALUE, -1, 0, 2, Integer.MAX_VALUE}) {
            assertThatThrownBy(() -> ChineseEra.of(value)).isInstanceOf(DateTimeException.class);
        }
    }

    @Test
    void rejectsUnsupportedFieldsAndHandlesStandardQueries() {
        for (ChronoField field : ChronoField.values()) {
            if (field != ChronoField.ERA) {
                assertThat(ERA.isSupported(field)).isFalse();
                assertThatThrownBy(() -> ERA.range(field))
                        .isInstanceOf(UnsupportedTemporalTypeException.class);
                assertThatThrownBy(() -> ERA.getLong(field))
                        .isInstanceOf(UnsupportedTemporalTypeException.class);
            }
        }
        assertThat(ERA.isSupported(ChronoField.ERA)).isTrue();
        assertThat(ERA.query(TemporalQueries.precision())).isEqualTo(ChronoUnit.ERAS);
        assertThat(ERA.query(TemporalQueries.chronology())).isNull();
        assertThat(ERA.query(TemporalQueries.localDate())).isNull();
        assertThatThrownBy(() -> ERA.range(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void adjustsCommonAndLeapMonthDatesWithoutChangingTheirFields() {
        for (ChineseDate date : new ChineseDate[]{
                CHRONOLOGY.date(2024, 1, 1),
                CHRONOLOGY.date(2023, 2, false, 1),
                CHRONOLOGY.date(2023, 2, true, 1)}) {
            assertThat(ERA.adjustInto(date)).isEqualTo(date);
            assertThat(date.with(ERA)).isEqualTo(date);
        }
        LocalDate isoDate = LocalDate.of(2024, 1, 1);
        assertThat(ERA.adjustInto(isoDate)).isEqualTo(isoDate);
    }

    @Test
    void usesCalendarDisplayNameForEveryTextStyle() {
        for (Locale locale : new Locale[]{Locale.CHINESE, Locale.KOREAN, Locale.JAPANESE,
                Locale.forLanguageTag("vi"), Locale.forLanguageTag("lo"), Locale.forLanguageTag("my"),
                Locale.forLanguageTag("es"), Locale.GERMAN, Locale.FRENCH}) {
            for (TextStyle style : TextStyle.values()) {
                assertThat(ERA.getDisplayName(style, locale))
                        .isEqualTo(CHRONOLOGY.getDisplayName(style, locale));
            }
        }
        assertThatThrownBy(() -> ERA.getDisplayName(null, Locale.ROOT))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> ERA.getDisplayName(TextStyle.FULL, null))
                .isInstanceOf(NullPointerException.class);
    }
}
