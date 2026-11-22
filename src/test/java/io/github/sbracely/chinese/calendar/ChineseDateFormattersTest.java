package io.github.sbracely.chinese.calendar;

import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
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
    private static final String ERA = ResourceBundle.getBundle("cldr.chinese-calendar").getString("era.1");
    private static final String NAME = ResourceBundle.getBundle("cldr.chinese-calendar").getString("name");

    @Test
    void printsAllFourRepresentationsExactly() {
        assertThat(ChineseDate.of(2015, 1, 1).format(SEXAGENARY_YEAR_DATE))
                .isEqualTo(NAME + "乙未年正月初一");
        assertThat(ChineseDate.of(2009, 5, true, 11).format(ZODIAC_YEAR_DATE))
                .isEqualTo(NAME + "牛年闰五月十一");
        assertThat(ChineseDate.from(LocalDate.of(2014, 8, 7)).format(SEXAGENARY_YEAR_DAY_DATE))
                .isEqualTo(NAME + "甲午年七月庚戌日");
        assertThat(ChineseDate.of(2016, 11, 29).format(RELATED_GREGORIAN_YEAR_DATE))
                .isEqualTo(ERA + "2016年" + NAME + "丙申年十一月廿九");
    }

    @Test
    void distinguishesLeapMonthsAndLunarYearBoundaries() {
        assertThat(ChineseDate.of(2009, 5, 11).format(ZODIAC_YEAR_DATE)).isEqualTo(NAME + "牛年五月十一");
        assertThat(ChineseDate.of(2023, 2, true, 1).format(SEXAGENARY_YEAR_DATE))
                .isEqualTo(NAME + "癸卯年闰二月初一");
        assertThat(ChineseDate.from(LocalDate.of(2017, 1, 26)).format(RELATED_GREGORIAN_YEAR_DATE))
                .isEqualTo(ERA + "2016年" + NAME + "丙申年腊月廿九");
        assertThat(ChineseDate.from(LocalDate.of(2024, 2, 9)).format(SEXAGENARY_YEAR_DATE))
                .isEqualTo(NAME + "癸卯年腊月三十");
        assertThat(ChineseDate.from(LocalDate.of(2024, 2, 10)).format(SEXAGENARY_YEAR_DATE))
                .isEqualTo(NAME + "甲辰年正月初一");
    }

    @Test
    void printsEveryNumberedMonthAndDayWithoutNumericFallback() {
        DateTimeFormatter month = new DateTimeFormatterBuilder()
                .appendText(ChronoField.MONTH_OF_YEAR, ChineseDateText.getTextMap(ChronoField.MONTH_OF_YEAR))
                .toFormatter(Locale.SIMPLIFIED_CHINESE);
        String[] months = {"正月", "二月", "三月", "四月", "五月", "六月",
                "七月", "八月", "九月", "十月", "十一月", "腊月"};
        for (int i = 0; i < months.length; i++) {
            assertThat(ChineseDate.of(2024, i + 1, 1).format(month)).isEqualTo(months[i]);
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

        DateTimeFormatter custom = new DateTimeFormatterBuilder()
                .appendText(ZODIAC, ChineseDateText.getTextMap(ZODIAC)).appendLiteral('年')
                .appendText(IS_LEAP_MONTH, ChineseDateText.getTextMap(IS_LEAP_MONTH))
                .appendText(ChronoField.MONTH_OF_YEAR, ChineseDateText.getTextMap(ChronoField.MONTH_OF_YEAR))
                .appendText(ChronoField.DAY_OF_MONTH, ChineseDateText.getTextMap(ChronoField.DAY_OF_MONTH))
                .toFormatter(Locale.ENGLISH).withChronology(ChineseChronology.INSTANCE);
        assertThat(ChineseDate.of(2009, 5, true, 11).format(custom)).isEqualTo("牛年闰五月十一");
        assertThat(date.format(SEXAGENARY_YEAR_DATE.withLocale(Locale.KOREAN))).isEqualTo(NAME + "乙未年正月初一");
        assertThat(ChineseDate.of(2016, 11, 29).format(RELATED_GREGORIAN_YEAR_DATE
                .withLocale(Locale.forLanguageTag("ar")))).isEqualTo(ERA + "2016年" + NAME + "丙申年十一月廿九");
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
}
