package io.github.sbracely.chinese.calendar;

import java.time.DateTimeException;
import java.time.chrono.Era;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.time.temporal.ValueRange;
import java.util.Locale;

public enum ChineseEra implements Era {
    CHINESE_ERA;

    @Override
    public int getValue() {
        return 1;
    }

    public static ChineseEra of(int eraValue) {
        if (eraValue == CHINESE_ERA.getValue()) {
            return CHINESE_ERA;
        }
        throw new DateTimeException("Invalid Chinese era: " + eraValue);
    }

    @Override
    public ValueRange range(TemporalField field) {
        if (field == ChronoField.ERA) {
            return ValueRange.of(CHINESE_ERA.getValue(), CHINESE_ERA.getValue());
        }
        return Era.super.range(field);
    }

    // todo: CLDR
    @Override
    public String getDisplayName(TextStyle style, Locale locale) {
        return new DateTimeFormatterBuilder()
                .appendText(ChronoField.ERA, style)
                .toFormatter(locale)
                .withChronology(ChineseChronology.INSTANCE)
                .format(ChineseDate.now());
    }
}
