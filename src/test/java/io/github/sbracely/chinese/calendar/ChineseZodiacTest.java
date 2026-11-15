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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChineseZodiacTest {
    private static final ChineseChronology CHRONOLOGY = ChineseChronology.INSTANCE;
    private static final ChineseTemporalField ZODIAC = ChineseTemporalField.ZODIAC;

    @Test
    void followsLunarYearAndCyclesThroughAllAnimals() {
        ChineseZodiac[] animals = ChineseZodiac.values();
        for (int i = 0; i < animals.length; i++) {
            assertThat(ChineseDate.of(2020 + i, 1, 1).getZodiac()).isEqualTo(animals[i]);
            assertThat(ChineseDate.of(2008 + i, 12, 1).getZodiac()).isEqualTo(animals[i]);
            assertThat(ChineseZodiac.of(i + 1)).isEqualTo(animals[i]);
            assertThat(animals[i].getValue()).isEqualTo(i + 1);
        }
        assertThat(ChineseDate.from(LocalDate.of(2024, 2, 9)).getZodiac())
                .isEqualTo(ChineseZodiac.RABBIT);
        assertThat(ChineseDate.from(LocalDate.of(2024, 2, 10)).getZodiac())
                .isEqualTo(ChineseZodiac.DRAGON);
        assertThat(ChineseDate.of(2023, 2, true, 1).getZodiac()).isEqualTo(ChineseZodiac.RABBIT);
        assertThat(ChineseDate.of(1900, 1, 1).getZodiac()).isEqualTo(ChineseZodiac.RAT);
        assertThat(ChineseDate.of(2099, 12, 1).getZodiac()).isEqualTo(ChineseZodiac.GOAT);
        assertThatThrownBy(() -> ChineseZodiac.of(0)).isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> ChineseZodiac.of(13)).isInstanceOf(DateTimeException.class);
    }

    @Test
    void displaysCldrNamesAndRootFallback() {
        assertThat(ChineseZodiac.RAT.getDisplayName(Locale.CHINESE)).isEqualTo("鼠");
        assertThat(ChineseZodiac.RAT.getDisplayName(Locale.ENGLISH)).isEqualTo("Rat");
        assertThat(ChineseZodiac.RAT.getDisplayName(Locale.JAPANESE)).isEqualTo("鼠");
        assertThat(ChineseZodiac.RAT.getDisplayName(Locale.KOREAN)).isEqualTo("쥐");
        assertThat(ChineseZodiac.RAT.getDisplayName(Locale.ROOT)).isEqualTo("zi");
        assertThatThrownBy(() -> ChineseZodiac.RAT.getDisplayName(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void readsAndAdjustsWithinTheCurrentCycle() {
        ChineseDate rabbit = ChineseDate.of(2023, 2, true, 29);
        assertThat(rabbit.get(ZODIAC)).isEqualTo(4);
        assertThat(rabbit.range(ZODIAC)).isEqualTo(ValueRange.of(1, 12));
        assertThat(ZODIAC.getBaseUnit()).isEqualTo(ChronoUnit.YEARS);
        assertThat(ZODIAC.getRangeUnit()).isEqualTo(ChronoUnit.FOREVER);
        assertThat(rabbit.with(ZODIAC, 4)).isEqualTo(rabbit);
        assertThat(rabbit.with(ZODIAC, 1)).isEqualTo(ChineseDate.of(2020, 2, 29));
        assertThat(ZODIAC.adjustInto(rabbit, 5)).isEqualTo(ChineseDate.of(2024, 2, 29));
        assertThat(ChineseDate.of(2069, 4, true, 1).with(ZODIAC, 10))
                .isEqualTo(ChineseDate.of(2077, 4, true, 1));
        assertThat(ChineseDate.of(2024, 2, 30).with(ZODIAC, 3))
                .isEqualTo(ChineseDate.of(2022, 2, 29));
        assertThatThrownBy(() -> rabbit.with(ZODIAC, 0)).isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> rabbit.with(ZODIAC, 13)).isInstanceOf(DateTimeException.class);
        assertThatThrownBy(() -> ChineseDate.of(2099, 1, 1).with(ZODIAC, 12))
                .isInstanceOf(DateTimeException.class);
    }

    @Test
    void formatsAndParsesNumericAndLocalizedZodiacWithCompleteDate() {
        Map<Long, String> names = Arrays.stream(ChineseZodiac.values())
                .collect(Collectors.toMap(z -> (long) z.getValue(), z -> z.getDisplayName(Locale.CHINESE)));
        DateTimeFormatter numeric = formatter(false, names);
        DateTimeFormatter text = formatter(true, names);
        ChineseDate date = ChineseDate.of(2023, 2, true, 1);
        assertThat(date.format(numeric)).isEqualTo("2023/2/1/1/4");
        assertThat(date.format(text)).isEqualTo("2023/2/1/1/兔");
        for (ResolverStyle style : ResolverStyle.values()) {
            ChineseDate numericDate = numeric.withResolverStyle(style).parse(date.format(numeric), CHRONOLOGY::date);
            ChineseDate textDate = text.withResolverStyle(style).parse(date.format(text), CHRONOLOGY::date);
            assertThat(numericDate).isEqualTo(date);
            assertThat(textDate).isEqualTo(date);
            assertThatThrownBy(() -> text.withResolverStyle(style)
                    .parse("2023/2/1/1/龙", CHRONOLOGY::date)).isInstanceOf(DateTimeException.class);
        }
        assertThatThrownBy(() -> new DateTimeFormatterBuilder().appendValue(ZODIAC)
                .toFormatter().withChronology(CHRONOLOGY).parse("4", CHRONOLOGY::date))
                .isInstanceOf(DateTimeException.class);
    }

    private static DateTimeFormatter formatter(boolean text, Map<Long, String> names) {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR).appendLiteral('/')
                .appendValue(ChronoField.MONTH_OF_YEAR).appendLiteral('/')
                .appendValue(ChronoField.DAY_OF_MONTH).appendLiteral('/')
                .appendValue(ChineseTemporalField.IS_LEAP_MONTH).appendLiteral('/');
        if (text) {
            builder.appendText(ZODIAC, names);
        } else {
            builder.appendValue(ZODIAC);
        }
        return builder.toFormatter(Locale.CHINESE).withChronology(CHRONOLOGY);
    }

    @Test
    void checksZodiacOnDirectResolutionPaths() {
        ChineseDate date = ChineseDate.of(2023, 2, true, 1);
        for (Map<TemporalField, Long> input : List.<Map<TemporalField, Long>>of(
                Map.of(ChronoField.EPOCH_DAY, date.toEpochDay()),
                Map.of(ChronoField.YEAR, 2023L, ChronoField.DAY_OF_YEAR,
                        (long) date.getDayOfYear()),
                Map.of(ChronoField.YEAR, 2023L, ChronoField.MONTH_OF_YEAR, 2L,
                        ChronoField.DAY_OF_MONTH, 1L, ChineseTemporalField.IS_LEAP_MONTH, 1L),
                Map.of(ChronoField.YEAR, 2023L, ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR, 3L,
                        ChronoField.DAY_OF_MONTH, 1L)
        )) {
            for (ResolverStyle style : ResolverStyle.values()) {
                Map<TemporalField, Long> matching = new HashMap<>(input);
                matching.put(ZODIAC, 4L);
                assertThat(CHRONOLOGY.resolveDate(matching, style)).isEqualTo(date);
                assertThat(matching).doesNotContainKey(ZODIAC);
                Map<TemporalField, Long> conflicting = new HashMap<>(input);
                conflicting.put(ZODIAC, 5L);
                assertThatThrownBy(() -> CHRONOLOGY.resolveDate(conflicting, style))
                        .isInstanceOf(DateTimeException.class);
            }
        }
        Map<TemporalField, Long> incomplete = new HashMap<>(Map.of(ZODIAC, 4L));
        assertThat(CHRONOLOGY.resolveDate(incomplete, ResolverStyle.STRICT)).isNull();
        assertThat(incomplete).containsEntry(ZODIAC, 4L);
    }

    @Test
    void formatterChecksZodiacForEpochDayAndOrdinaryDate() {
        ChineseDate date = ChineseDate.of(2024, 1, 1);
        DateTimeFormatter epochFormatter = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.EPOCH_DAY).appendLiteral('/')
                .appendValue(ZODIAC).toFormatter().withChronology(CHRONOLOGY);
        DateTimeFormatter dateFormatter = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR).appendLiteral('/')
                .appendValue(ChronoField.MONTH_OF_YEAR).appendLiteral('/')
                .appendValue(ChronoField.DAY_OF_MONTH).appendLiteral('/')
                .appendValue(ZODIAC).toFormatter().withChronology(CHRONOLOGY);
        for (ResolverStyle style : ResolverStyle.values()) {
            ChineseDate epoch = epochFormatter.withResolverStyle(style)
                    .parse(date.toEpochDay() + "/5", CHRONOLOGY::date);
            ChineseDate ymd = dateFormatter.withResolverStyle(style)
                    .parse("2024/1/1/5", CHRONOLOGY::date);
            assertThat(epoch).isEqualTo(date);
            assertThat(ymd).isEqualTo(date);
            assertThatThrownBy(() -> epochFormatter.withResolverStyle(style)
                    .parse(date.toEpochDay() + "/4", CHRONOLOGY::date)).isInstanceOf(DateTimeException.class);
            assertThatThrownBy(() -> dateFormatter.withResolverStyle(style)
                    .parse("2024/1/1/4", CHRONOLOGY::date)).isInstanceOf(DateTimeException.class);
        }
    }
}
