package io.github.sbracely.chinese.calendar;

import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import static io.github.sbracely.chinese.calendar.ChineseDateFormatters.*;
import static io.github.sbracely.chinese.calendar.ChineseTemporalField.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChineseDateFormattersTest {
    private static final ResourceBundle CLDR = ResourceBundle.getBundle("cldr.chinese-calendar", Locale.getDefault());
    private static final String ERA = CLDR.getString("era.1");
    private static final String NAME = CLDR.getString("name");

    @Test
    void printsAllFourRepresentationsExactly() {
        assertThat(ChineseDate.of(2015, 1, 1).format(SEXAGENARY_YEAR_DATE))
                .isEqualTo(expected(ChineseDate.of(2015, 1, 1), SEXAGENARY_YEAR, ChronoField.DAY_OF_MONTH));
        assertThat(ChineseDate.of(2009, 5, true, 11).format(ZODIAC_YEAR_DATE))
                .isEqualTo(expected(ChineseDate.of(2009, 5, true, 11), ZODIAC, ChronoField.DAY_OF_MONTH));
        assertThat(ChineseDate.from(LocalDate.of(2014, 8, 7)).format(SEXAGENARY_YEAR_DAY_DATE))
                .isEqualTo(expected(ChineseDate.from(LocalDate.of(2014, 8, 7)),
                        SEXAGENARY_YEAR, SEXAGENARY_DAY));
        assertThat(ChineseDate.of(2016, 11, 29).format(RELATED_GREGORIAN_YEAR_DATE))
                .isEqualTo(expected(ChineseDate.of(2016, 11, 29), SEXAGENARY_YEAR,
                        ChronoField.DAY_OF_MONTH, true));
    }

    @Test
    void distinguishesLeapMonthsAndLunarYearBoundaries() {
        assertThat(ChineseDate.of(2009, 5, 11).format(ZODIAC_YEAR_DATE))
                .isEqualTo(expected(ChineseDate.of(2009, 5, 11), ZODIAC, ChronoField.DAY_OF_MONTH));
        assertThat(ChineseDate.of(2023, 2, true, 1).format(SEXAGENARY_YEAR_DATE))
                .isEqualTo(expected(ChineseDate.of(2023, 2, true, 1),
                        SEXAGENARY_YEAR, ChronoField.DAY_OF_MONTH));
        assertThat(ChineseDate.from(LocalDate.of(2017, 1, 26)).format(RELATED_GREGORIAN_YEAR_DATE))
                .isEqualTo(expected(ChineseDate.from(LocalDate.of(2017, 1, 26)),
                        SEXAGENARY_YEAR, ChronoField.DAY_OF_MONTH, true));
        assertThat(ChineseDate.from(LocalDate.of(2024, 2, 9)).format(SEXAGENARY_YEAR_DATE))
                .isEqualTo(expected(ChineseDate.from(LocalDate.of(2024, 2, 9)),
                        SEXAGENARY_YEAR, ChronoField.DAY_OF_MONTH));
        assertThat(ChineseDate.from(LocalDate.of(2024, 2, 10)).format(SEXAGENARY_YEAR_DATE))
                .isEqualTo(expected(ChineseDate.from(LocalDate.of(2024, 2, 10)),
                        SEXAGENARY_YEAR, ChronoField.DAY_OF_MONTH));
    }

    @Test
    void printsEveryNumberedMonthAndDayWithoutNumericFallback() {
        DateTimeFormatter month = new DateTimeFormatterBuilder()
                .appendText(ChronoField.MONTH_OF_YEAR, ChineseDateText.getTextMap(ChronoField.MONTH_OF_YEAR))
                .toFormatter(Locale.SIMPLIFIED_CHINESE);
        for (int i = 1; i <= 12; i++) {
            assertThat(ChineseDate.of(2024, i, 1).format(month)).isEqualTo(CLDR.getString("month." + i));
        }
        DateTimeFormatter day = new DateTimeFormatterBuilder()
                .appendText(ChronoField.DAY_OF_MONTH, ChineseDateText.getTextMap(ChronoField.DAY_OF_MONTH))
                .toFormatter(Locale.SIMPLIFIED_CHINESE);
        String[] days = {"初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
                "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
                "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"};
        for (int i = 0; i < days.length; i++) {
            assertThat(ChineseDate.of(2024, 2, i + 1).format(day)).isEqualTo(days[i]);
        }
    }

    @Test
    void providesCompleteImmutableMapsAndRejectsUnsupportedFields() {
        for (var field : List.of(SEXAGENARY_YEAR, SEXAGENARY_DAY, ZODIAC, IS_LEAP_MONTH,
                ChronoField.MONTH_OF_YEAR, ChronoField.DAY_OF_MONTH)) {
            Map<Long, String> map = ChineseDateText.getTextMap(field);
            var range = field == ChronoField.DAY_OF_MONTH
                    ? ChineseChronology.INSTANCE.range(ChronoField.DAY_OF_MONTH) : field.range();
            for (long value = range.getMinimum(); value <= range.getMaximum(); value++) {
                assertThat(map).containsKey(value);
            }
            assertThatThrownBy(() -> map.put(1L, "changed")).isInstanceOf(UnsupportedOperationException.class);
        }
        assertThatThrownBy(() -> ChineseDateText.getTextMap(null)).isInstanceOf(NullPointerException.class);
        for (var field : List.of(ChronoField.YEAR, MONTH_AS_ORDINAL_OF_YEAR)) {
            assertThatThrownBy(() -> ChineseDateText.getTextMap(field))
                    .isInstanceOf(UnsupportedTemporalTypeException.class);
        }
    }

    @Test
    void composesNativeBuildersAndPresetsWithoutChangingChineseTextWithLocale() {
        ChineseDate date = ChineseDate.of(2015, 1, 1);
        DateTimeFormatter composed = new DateTimeFormatterBuilder()
                .appendLiteral('[').append(SEXAGENARY_YEAR_DATE).appendLiteral(']')
                .toFormatter(Locale.ENGLISH).withChronology(ChineseChronology.INSTANCE);
        assertThat(date.format(composed)).isEqualTo("[" + NAME + "乙未年正月初一]");

        DateTimeFormatterBuilder customBuilder = ChineseDateText.appendLeapMonthPrefix(
                new DateTimeFormatterBuilder()
                        .appendText(ZODIAC, ChineseDateText.getTextMap(ZODIAC)).appendLiteral('年'));
        customBuilder.appendText(ChronoField.MONTH_OF_YEAR, ChineseDateText.getTextMap(ChronoField.MONTH_OF_YEAR));
        ChineseDateText.appendLeapMonthSuffix(customBuilder)
                .appendText(ChronoField.DAY_OF_MONTH, ChineseDateText.getTextMap(ChronoField.DAY_OF_MONTH));
        DateTimeFormatter custom = customBuilder.toFormatter(Locale.ENGLISH)
                .withChronology(ChineseChronology.INSTANCE);
        ChineseDate leapDate = ChineseDate.of(2009, 5, true, 11);
        assertThat(leapDate.format(custom)).isEqualTo(text(leapDate, ZODIAC) + "年"
                + monthText(leapDate) + text(leapDate, ChronoField.DAY_OF_MONTH));
        assertThat(date.format(SEXAGENARY_YEAR_DATE.withLocale(Locale.KOREAN)))
                .isEqualTo(expected(date, SEXAGENARY_YEAR, ChronoField.DAY_OF_MONTH));
        assertThat(ChineseDate.of(2016, 11, 29).format(RELATED_GREGORIAN_YEAR_DATE
                .withLocale(Locale.forLanguageTag("ar")))).isEqualTo(expected(
                        ChineseDate.of(2016, 11, 29), SEXAGENARY_YEAR, ChronoField.DAY_OF_MONTH, true));
    }

    @Test
    void doesNotInferAbsoluteDatesFromPresetOutput() {
        ChineseDate date = ChineseDate.of(2015, 1, 1);
        for (DateTimeFormatter format : List.of(SEXAGENARY_YEAR_DATE, ZODIAC_YEAR_DATE,
                SEXAGENARY_YEAR_DAY_DATE)) {
            assertThatThrownBy(() -> format.parse(date.format(format), ChineseDate::from))
                    .isInstanceOf(DateTimeException.class);
        }
    }

    @Test
    void numericYearPresetUsesTheExistingYearFieldForParsingAndFormatting() {
        for (ChineseDate date : List.of(ChineseDate.of(2016, 11, 29), ChineseDate.of(2016, 12, 29),
                ChineseDate.of(2023, 2, true, 1))) {
            String text = date.format(RELATED_GREGORIAN_YEAR_DATE);
            assertThat(text).startsWith(ERA + date.get(ChronoField.YEAR) + "年" + NAME);
            assertThat(RELATED_GREGORIAN_YEAR_DATE.parse(text, ChineseDate::from)).isEqualTo(date);
        }
    }

    private static String expected(ChineseDate date, TemporalField yearField, TemporalField dayField) {
        return expected(date, yearField, dayField, false);
    }

    private static String expected(ChineseDate date, TemporalField yearField, TemporalField dayField,
                                   boolean relatedYear) {
        StringBuilder text = new StringBuilder();
        if (relatedYear) {
            text.append(ERA).append(date.getLong(ChronoField.YEAR)).append('年');
        }
        text.append(NAME)
                .append(text(date, yearField))
                .append('年')
                .append(monthText(date))
                .append(text(date, dayField));
        if (dayField == SEXAGENARY_DAY) {
            text.append('日');
        }
        return text.toString();
    }

    private static String text(ChineseDate date, TemporalField field) {
        return ChineseDateText.getTextMap(field).get(date.getLong(field));
    }

    private static String monthText(ChineseDate date) {
        String month = text(date, ChronoField.MONTH_OF_YEAR);
        return date.getLong(IS_LEAP_MONTH) == 1
                ? CLDR.getString("month.leapPattern").replace("{0}", month) : month;
    }
}
