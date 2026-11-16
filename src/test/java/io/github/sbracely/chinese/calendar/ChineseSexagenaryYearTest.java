package io.github.sbracely.chinese.calendar;

import org.junit.jupiter.api.Test;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalField;
import java.time.temporal.ValueRange;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChineseSexagenaryYearTest {
    private static final ChineseChronology CHRONOLOGY = ChineseChronology.INSTANCE;
    private static final ChineseTemporalField SEXAGENARY_YEAR = ChineseTemporalField.SEXAGENARY_YEAR;
    private static final ChineseTemporalField ZODIAC = ChineseTemporalField.ZODIAC;

    @Test
    void followsTheTraditionalOrderAndRepeatsEverySixtyYears() {
        String[] stems = {"甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"};
        String[] branches = {"子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"};
        for (int i = 0; i < 60; i++) {
            ChineseSexagenaryYear year = ChineseSexagenaryYear.of(i + 1);
            assertThat(year.getValue()).isEqualTo(i + 1);
            assertThat(year.getDisplayName(Locale.CHINESE)).isEqualTo(stems[i % 10] + branches[i % 12]);
            assertThat(ChineseDate.of(1984 + i, 1, 1).getSexagenaryYear()).isEqualTo(year);
            assertThat(ChineseDate.of(1924 + i, 12, 1).getSexagenaryYear()).isEqualTo(year);
        }
        assertThat(ChineseDate.of(2024, 1, 1).getSexagenaryYear()).isEqualTo(ChineseSexagenaryYear.JIA_CHEN);
        assertThat(ChineseDate.of(2044, 1, 1).getSexagenaryYear()).isEqualTo(ChineseSexagenaryYear.JIA_ZI);
        assertThat(ChineseDate.of(1900, 1, 1).getSexagenaryYear()).isEqualTo(ChineseSexagenaryYear.GENG_ZI);
        assertThat(ChineseDate.of(2099, 12, 1).getSexagenaryYear()).isEqualTo(ChineseSexagenaryYear.JI_WEI);
        assertThat(ChineseSexagenaryYear.fromYear(0)).isEqualTo(ChineseSexagenaryYear.GENG_SHEN);
        assertThat(ChineseSexagenaryYear.fromYear(-1)).isEqualTo(ChineseSexagenaryYear.JI_WEI);
        assertThat(ChineseSexagenaryYear.fromYear(Integer.MIN_VALUE)).isEqualTo(ChineseSexagenaryYear.REN_ZI);
        assertThat(ChineseSexagenaryYear.fromYear(Integer.MAX_VALUE)).isEqualTo(ChineseSexagenaryYear.DING_MAO);
        assertThatThrownBy(() -> ChineseSexagenaryYear.of(0)).isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> ChineseSexagenaryYear.of(61)).isInstanceOf(DateTimeException.class);
    }

    @Test
    void changesOnlyAtLunarNewYear() {
        for (LocalDate day : List.of(LocalDate.of(2023, 12, 31), LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 2, 5), LocalDate.of(2024, 2, 9))) {
            assertThat(ChineseDate.from(day).getSexagenaryYear()).isEqualTo(ChineseSexagenaryYear.GUI_MAO);
        }
        assertThat(ChineseDate.from(LocalDate.of(2024, 2, 10)).getSexagenaryYear())
                .isEqualTo(ChineseSexagenaryYear.JIA_CHEN);
        assertThat(ChineseDate.of(2023, 2, true, 1).getSexagenaryYear())
                .isEqualTo(ChineseSexagenaryYear.GUI_MAO);
    }

    @Test
    void displaysCldrNamesAndInheritedRootNames() {
        ChineseSexagenaryYear year = ChineseSexagenaryYear.JIA_ZI;
        assertThat(year.getDisplayName(Locale.CHINESE)).isEqualTo("甲子");
        assertThat(year.getDisplayName(Locale.JAPANESE)).isEqualTo("甲子");
        assertThat(year.getDisplayName(Locale.KOREAN)).isEqualTo("갑자");
        assertThat(year.getDisplayName(Locale.forLanguageTag("vi"))).isEqualTo("Giáp Tý");
        assertThat(year.getDisplayName(Locale.forLanguageTag("lo"))).isEqualTo("ເຈຍ-ຊິ");
        for (Locale locale : List.of(Locale.ROOT, Locale.ENGLISH, Locale.forLanguageTag("my"),
                Locale.forLanguageTag("es"), Locale.GERMAN, Locale.FRENCH)) {
            assertThat(year.getDisplayName(locale)).isEqualTo("jia-zi");
        }
        assertThatThrownBy(() -> year.getDisplayName(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void readsAndAdjustsWithinTheCurrentJiaZiToGuiHaiCycle() {
        ChineseDate leap = ChineseDate.of(2023, 2, true, 29);
        assertThat(leap.get(SEXAGENARY_YEAR)).isEqualTo(40);
        assertThat(SEXAGENARY_YEAR.getFrom(leap)).isEqualTo(40);
        assertThat(leap.range(SEXAGENARY_YEAR)).isEqualTo(ValueRange.of(1, 60));
        assertThat(CHRONOLOGY.range(SEXAGENARY_YEAR)).isEqualTo(ValueRange.of(1, 60));
        assertThat(SEXAGENARY_YEAR.getBaseUnit()).isEqualTo(ChronoUnit.YEARS);
        assertThat(SEXAGENARY_YEAR.getRangeUnit()).isEqualTo(ChronoUnit.FOREVER);
        assertThat(leap.with(SEXAGENARY_YEAR, 40)).isEqualTo(leap);
        assertThat(SEXAGENARY_YEAR.adjustInto(leap, 41)).isEqualTo(ChineseDate.of(2024, 2, 29));
        assertThat(ChineseDate.of(2024, 2, 30).with(SEXAGENARY_YEAR, 39))
                .isEqualTo(ChineseDate.of(2022, 2, 29));
        assertThat(ChineseDate.of(2069, 4, true, 1).with(SEXAGENARY_YEAR, 34))
                .isEqualTo(ChineseDate.of(2077, 4, true, 1));
        assertThat(ChineseDate.of(1984, 1, 1).with(SEXAGENARY_YEAR, 60))
                .isEqualTo(ChineseDate.of(2043, 1, 1));
        assertThat(ChineseDate.of(2043, 1, 1).with(SEXAGENARY_YEAR, 1))
                .isEqualTo(ChineseDate.of(1984, 1, 1));
        assertThatThrownBy(() -> leap.with(SEXAGENARY_YEAR, 0)).isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> leap.with(SEXAGENARY_YEAR, 61)).isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> ChineseDate.of(1900, 1, 1).with(SEXAGENARY_YEAR, 1))
                .isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> ChineseDate.of(2099, 1, 1).with(SEXAGENARY_YEAR, 60))
                .isInstanceOf(DateTimeException.class);
    }

    @Test
    void formatsAndParsesNumericAndLocalizedYearsWithCompleteDates() {
        DateTimeFormatter numeric = formatter(false);
        DateTimeFormatter text = formatter(true);
        ChineseDate date = ChineseDate.of(2024, 1, 1);
        assertThat(date.format(numeric)).isEqualTo("2024/1/1/0/41");
        assertThat(date.format(text)).isEqualTo("2024/1/1/0/甲辰");
        ChineseDate leap = ChineseDate.of(2023, 2, true, 1);
        for (ResolverStyle style : ResolverStyle.values()) {
            for (DateTimeFormatter format : List.of(numeric, text)) {
                for (ChineseDate input : List.of(date, leap)) {
                    assertThat(format.withResolverStyle(style).parse(input.format(format), ChineseDate::from))
                            .isEqualTo(input);
                }
            }
            assertThatThrownBy(() -> text.withResolverStyle(style)
                    .parse("2024/1/1/0/乙巳", ChineseDate::from)).isInstanceOf(DateTimeException.class);
            for (int invalid : new int[]{0, 42, 61}) {
                assertThatThrownBy(() -> numeric.withResolverStyle(style)
                        .parse("2024/1/1/0/" + invalid, ChineseDate::from)).isInstanceOf(DateTimeException.class);
            }
        }
    }

    @Test
    void checksAndConsumesCycleFieldsOnEveryResolutionPath() {
        for (ChineseDate date : List.of(ChineseDate.of(2024, 1, 1), ChineseDate.of(2023, 2, true, 1))) {
            for (Map<TemporalField, Long> input : resolutionInputs(date)) {
                for (ResolverStyle style : ResolverStyle.values()) {
                    Map<TemporalField, Long> matching = new HashMap<>(input);
                    matching.put(SEXAGENARY_YEAR, (long) date.getSexagenaryYear().getValue());
                    matching.put(ZODIAC, (long) date.getZodiac().getValue());
                    assertThat(CHRONOLOGY.resolveDate(matching, style)).isEqualTo(date);
                    assertThat(matching).doesNotContainKeys(SEXAGENARY_YEAR, ZODIAC);
                    for (int invalid : new int[]{0, 42, 61}) {
                        Map<TemporalField, Long> conflicting = new HashMap<>(input);
                        conflicting.put(SEXAGENARY_YEAR, (long) invalid);
                        conflicting.put(ZODIAC, (long) date.getZodiac().getValue());
                        assertThatThrownBy(() -> CHRONOLOGY.resolveDate(conflicting, style))
                                .isInstanceOf(DateTimeException.class);
                    }
                    Map<TemporalField, Long> conflictingZodiac = new HashMap<>(input);
                    conflictingZodiac.put(SEXAGENARY_YEAR, (long) date.getSexagenaryYear().getValue());
                    conflictingZodiac.put(ZODIAC, 1L);
                    assertThatThrownBy(() -> CHRONOLOGY.resolveDate(conflictingZodiac, style))
                            .isInstanceOf(DateTimeException.class);

                    DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
                    StringJoiner text = new StringJoiner("/");
                    input.forEach((field, value) -> {
                        builder.appendValue(field).appendLiteral('/');
                        text.add(value.toString());
                    });
                    DateTimeFormatter format = builder.appendValue(SEXAGENARY_YEAR)
                            .toFormatter(Locale.ROOT).withChronology(CHRONOLOGY).withResolverStyle(style);
                    assertThat(format.parse(text + "/" + date.getSexagenaryYear().getValue(), ChineseDate::from))
                            .isEqualTo(date);
                    assertThatThrownBy(() -> format.parse(text + "/42", ChineseDate::from))
                            .isInstanceOf(DateTimeException.class);
                }
            }
        }
    }

    @Test
    void doesNotInferAnAbsoluteYearFromCycleFields() {
        DateTimeFormatter format = new DateTimeFormatterBuilder()
                .appendText(SEXAGENARY_YEAR, names()).appendLiteral('/')
                .appendValue(ChronoField.MONTH_OF_YEAR).appendLiteral('/')
                .appendValue(ChronoField.DAY_OF_MONTH)
                .toFormatter(Locale.CHINESE).withChronology(CHRONOLOGY);
        Map<TemporalField, Long> input = Map.of(SEXAGENARY_YEAR, 41L, ZODIAC, 5L,
                ChronoField.MONTH_OF_YEAR, 1L, ChronoField.DAY_OF_MONTH, 1L);
        for (ResolverStyle style : ResolverStyle.values()) {
            Map<TemporalField, Long> fields = new HashMap<>(input);
            assertThat(CHRONOLOGY.resolveDate(fields, style)).isNull();
            assertThat(fields).isEqualTo(input);
            assertThatThrownBy(() -> format.withResolverStyle(style).parse("甲辰/1/1", ChineseDate::from))
                    .isInstanceOf(DateTimeException.class);
        }
    }

    private static List<Map<TemporalField, Long>> resolutionInputs(ChineseDate date) {
        List<Map<TemporalField, Long>> inputs = new ArrayList<>();
        inputs.add(Map.of(ChronoField.EPOCH_DAY, date.toEpochDay()));
        inputs.add(Map.of(ChronoField.YEAR, (long) date.getYear(),
                ChronoField.DAY_OF_YEAR, (long) date.getDayOfYear()));
        inputs.add(Map.of(ChronoField.YEAR, (long) date.getYear(),
                ChronoField.MONTH_OF_YEAR, (long) date.getMonthValue(),
                ChronoField.DAY_OF_MONTH, (long) date.getDayOfMonth(),
                ChineseTemporalField.IS_LEAP_MONTH, date.isLeapMonth() ? 1L : 0L));
        inputs.add(Map.of(ChronoField.YEAR, (long) date.getYear(),
                ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR,
                date.getLong(ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR),
                ChronoField.DAY_OF_MONTH, (long) date.getDayOfMonth()));
        if (!date.isLeapMonth()) {
            inputs.add(Map.of(ChronoField.YEAR, (long) date.getYear(),
                    ChronoField.MONTH_OF_YEAR, (long) date.getMonthValue(),
                    ChronoField.DAY_OF_MONTH, (long) date.getDayOfMonth()));
        }
        return inputs;
    }

    private static Map<Long, String> names() {
        return Arrays.stream(ChineseSexagenaryYear.values())
                .collect(Collectors.toMap(y -> (long) y.getValue(), y -> y.getDisplayName(Locale.CHINESE)));
    }

    private static DateTimeFormatter formatter(boolean text) {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR).appendLiteral('/')
                .appendValue(ChronoField.MONTH_OF_YEAR).appendLiteral('/')
                .appendValue(ChronoField.DAY_OF_MONTH).appendLiteral('/')
                .appendValue(ChineseTemporalField.IS_LEAP_MONTH).appendLiteral('/');
        if (text) {
            builder.appendText(SEXAGENARY_YEAR, names());
        } else {
            builder.appendValue(SEXAGENARY_YEAR);
        }
        return builder.toFormatter(Locale.CHINESE).withChronology(CHRONOLOGY);
    }
}
