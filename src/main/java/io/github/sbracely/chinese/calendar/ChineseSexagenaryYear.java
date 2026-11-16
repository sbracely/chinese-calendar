package io.github.sbracely.chinese.calendar;

import java.time.DateTimeException;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * The sixty years of the Chinese sexagenary cycle, in their customary order.
 * 按传统顺序排列的六十甲子。
 */
public enum ChineseSexagenaryYear {
    /** 甲子。 */
    JIA_ZI,
    /** 乙丑。 */
    YI_CHOU,
    /** 丙寅。 */
    BING_YIN,
    /** 丁卯。 */
    DING_MAO,
    /** 戊辰。 */
    WU_CHEN,
    /** 己巳。 */
    JI_SI,
    /** 庚午。 */
    GENG_WU,
    /** 辛未。 */
    XIN_WEI,
    /** 壬申。 */
    REN_SHEN,
    /** 癸酉。 */
    GUI_YOU,
    /** 甲戌。 */
    JIA_XU,
    /** 乙亥。 */
    YI_HAI,
    /** 丙子。 */
    BING_ZI,
    /** 丁丑。 */
    DING_CHOU,
    /** 戊寅。 */
    WU_YIN,
    /** 己卯。 */
    JI_MAO,
    /** 庚辰。 */
    GENG_CHEN,
    /** 辛巳。 */
    XIN_SI,
    /** 壬午。 */
    REN_WU,
    /** 癸未。 */
    GUI_WEI,
    /** 甲申。 */
    JIA_SHEN,
    /** 乙酉。 */
    YI_YOU,
    /** 丙戌。 */
    BING_XU,
    /** 丁亥。 */
    DING_HAI,
    /** 戊子。 */
    WU_ZI,
    /** 己丑。 */
    JI_CHOU,
    /** 庚寅。 */
    GENG_YIN,
    /** 辛卯。 */
    XIN_MAO,
    /** 壬辰。 */
    REN_CHEN,
    /** 癸巳。 */
    GUI_SI,
    /** 甲午。 */
    JIA_WU,
    /** 乙未。 */
    YI_WEI,
    /** 丙申。 */
    BING_SHEN,
    /** 丁酉。 */
    DING_YOU,
    /** 戊戌。 */
    WU_XU,
    /** 己亥。 */
    JI_HAI,
    /** 庚子。 */
    GENG_ZI,
    /** 辛丑。 */
    XIN_CHOU,
    /** 壬寅。 */
    REN_YIN,
    /** 癸卯。 */
    GUI_MAO,
    /** 甲辰。 */
    JIA_CHEN,
    /** 乙巳。 */
    YI_SI,
    /** 丙午。 */
    BING_WU,
    /** 丁未。 */
    DING_WEI,
    /** 戊申。 */
    WU_SHEN,
    /** 己酉。 */
    JI_YOU,
    /** 庚戌。 */
    GENG_XU,
    /** 辛亥。 */
    XIN_HAI,
    /** 壬子。 */
    REN_ZI,
    /** 癸丑。 */
    GUI_CHOU,
    /** 甲寅。 */
    JIA_YIN,
    /** 乙卯。 */
    YI_MAO,
    /** 丙辰。 */
    BING_CHEN,
    /** 丁巳。 */
    DING_SI,
    /** 戊午。 */
    WU_WU,
    /** 己未。 */
    JI_WEI,
    /** 庚申。 */
    GENG_SHEN,
    /** 辛酉。 */
    XIN_YOU,
    /** 壬戌。 */
    REN_XU,
    /** 癸亥。 */
    GUI_HAI;

    /**
     * Gets the cycle value, from 1 for Jia-Zi to 60 for Gui-Hai.
     * 获取六十甲子序号，甲子为 1，癸亥为 60。
     *
     * @return the cycle value
     *         六十甲子序号
     */
    public int getValue() {
        return ordinal() + 1;
    }

    /**
     * Obtains a sexagenary year by its cycle value.
     * 根据六十甲子序号获取干支年。
     *
     * @param value the value from 1 to 60
     *              1 至 60 的数值
     * @return the sexagenary year
     *         干支年
     * @throws DateTimeException if the value is outside 1 to 60
     *                           如果数值不在 1 至 60 之间
     */
    public static ChineseSexagenaryYear of(int value) {
        if (value < 1 || value > 60) {
            throw new DateTimeException("Invalid Chinese sexagenary year: " + value);
        }
        return values()[value - 1];
    }

    /**
     * Calculates the cycle value from a Chinese calendar year, using 1984 as Jia-Zi.
     * 根据农历年份计算干支，以 1984 农历年为甲子年；不能直接传入公历年份。
     *
     * @param year the Chinese calendar year
     *             农历年份
     * @return the sexagenary year
     *         干支年
     */
    static ChineseSexagenaryYear fromYear(int year) {
        return of(Math.floorMod(year - 4L, 60) + 1);
    }

    /**
     * Gets the CLDR display name, without a year suffix.
     * 获取 CLDR 本地化名称，不附加“年”字。
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
                .getString("sexagenary." + getValue());
    }
}
