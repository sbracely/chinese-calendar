package io.github.sbracely.chinese.calendar;

import java.time.DateTimeException;
import java.time.chrono.Era;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalField;
import java.time.temporal.ValueRange;
import java.util.Locale;

/**
 * The placeholder era for the Chinese calendar.
 * 农历的占位纪元。
 * <p>
 * The Chinese calendar has no era division in this implementation. A single era
 * with the value {@code 1} is provided to satisfy the chronology API.
 * 此实现中的农历不划分纪元，提供数值为 {@code 1} 的唯一纪元以适配历法 API。
 */
public enum ChineseEra implements Era {
    /**
     * The sole placeholder era used to satisfy the chronology API.
     * 用于适配历法 API 的唯一占位纪元。
     */
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


    /**
     * Returns the localized calendar name as the display name of this placeholder era.
     * 返回本地化历法名称，作为此占位纪元的显示名称。
     *
     * @param style  the text style, not null
     *               文本样式，不为 null
     * @param locale the locale to use, not null
     *               使用的语言环境，不为 null
     * @return the display name, not null
     * 显示名称，不为 null
     */
    @Override
    public String getDisplayName(TextStyle style, Locale locale) {
        return ChineseChronology.INSTANCE.getDisplayName(style, locale);
    }
}
