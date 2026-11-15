package io.github.sbracely.chinese.calendar;

import java.time.DateTimeException;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * The twelve animals of the Chinese zodiac, in their customary order.
 * 按传统顺序排列的十二生肖。
 */
public enum ChineseZodiac {
    /** Rat. 鼠。 */
    RAT,
    /** Ox. 牛。 */
    OX,
    /** Tiger. 虎。 */
    TIGER,
    /** Rabbit. 兔。 */
    RABBIT,
    /** Dragon. 龙。 */
    DRAGON,
    /** Snake. 蛇。 */
    SNAKE,
    /** Horse. 马。 */
    HORSE,
    /** Goat. 羊。 */
    GOAT,
    /** Monkey. 猴。 */
    MONKEY,
    /** Rooster. 鸡。 */
    ROOSTER,
    /** Dog. 狗。 */
    DOG,
    /** Pig. 猪。 */
    PIG;

    /**
     * Gets the zodiac value, from 1 for the rat to 12 for the pig.
     * 获取生肖数值，鼠为 1，猪为 12。
     *
     * @return the zodiac value
     *         生肖数值
     */
    public int getValue() {
        return ordinal() + 1;
    }

    /**
     * Obtains a zodiac animal by its value.
     * 根据数值获取生肖。
     *
     * @param value the value from 1 to 12
     *              1 至 12 的数值
     * @return the zodiac animal
     *         生肖
     * @throws DateTimeException if the value is outside 1 to 12
     *                           如果数值不在 1 至 12 之间
     */
    public static ChineseZodiac of(int value) {
        if (value < 1 || value > 12) {
            throw new DateTimeException("Invalid Chinese zodiac: " + value);
        }
        return values()[value - 1];
    }

    /**
     * Calculates the zodiac from a Chinese calendar year, using 2020 as a rat year.
     * 根据农历年份计算生肖，以 2020 农历年为鼠年；不能直接传入公历年份。
     *
     * @param year the Chinese calendar year
     *             农历年份
     * @return the zodiac animal
     *         生肖
     */
    static ChineseZodiac fromYear(int year) {
        return of(Math.floorMod(year - 4L, 12) + 1);
    }

    /**
     * Gets the CLDR display name for the requested locale.
     * 获取指定语言环境的 CLDR 显示名称。
     *
     * @param locale the locale to use
     *               使用的语言环境
     * @return the localized name
     *         本地化名称
     * @throws NullPointerException if locale is {@code null}
     *                              如果语言环境为 {@code null}
     */
    public String getDisplayName(Locale locale) {
        Objects.requireNonNull(locale, "locale");
        return ResourceBundle.getBundle("cldr.chinese-calendar", locale)
                .getString("zodiac." + getValue());
    }
}
