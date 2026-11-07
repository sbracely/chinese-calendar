package io.github.sbracely.chinese.calendar.internal;

import java.util.Arrays;
import java.util.Map;

/**
 * Parsed Chinese calendar data with defensive copies of the array and map.
 * Each year occupies {@code maximumMonthsInYear} slots in the month-start table;
 * shorter years are padded with the next year's start, and the final entry is an exclusive bound.
 * 解析后的农历数据，对数组和映射进行防御性复制。
 * 月初表中每年占 {@code maximumMonthsInYear} 个槽位；
 * 月份较少的年份以次年起始日填充，最后一个条目为不包含在支持范围内的结束边界。
 *
 * @param monthStartEpochDays the padded month starts, in days since ISO 1970-01-01
 *                            含填充槽位的月初表，以自 ISO 日期 1970-01-01 起的天数表示
 * @param minimumEpochDay the first supported epoch day, inclusive
 *                        支持的最早纪元天数，包含该日
 * @param maximumEpochDay the last supported epoch day, inclusive
 *                        支持的最晚纪元天数，包含该日
 * @param epochMonthOffset the minimum year multiplied by the number of slots per year
 *                         最小年份乘以每年槽位数得到的月份索引偏移量
 * @param minimumDaysInMonth the shortest actual month in the data
 *                           数据中实际月份的最少天数
 * @param maximumDaysInMonth the longest actual month in the data
 *                           数据中实际月份的最多天数
 * @param maximumMonthsInYear the maximum actual month count and table slots per year
 *                            每年实际月份数的最大值，也是每年的表槽位数
 * @param minimumDaysInYear the shortest year in the data
 *                          数据中年份的最少天数
 * @param maximumDaysInYear the longest year in the data
 *                          数据中年份的最多天数
 * @param leapMonthByYear leap years mapped to their repeated month numbers; common years are absent
 *                        闰年到其重复月份编号的映射，不包含平年
 * @param minimumYear the first supported Chinese year
 *                    支持的最早农历年份
 * @param maximumYear the last supported Chinese year
 *                    支持的最晚农历年份
 * @param fingerprint the SHA-256 fingerprint of the parsed start date and year/month data
 *                    解析后的起始日期及年份、月份数据的 SHA-256 指纹
 */
public record ChineseCalendarData(int[] monthStartEpochDays, int minimumEpochDay, int maximumEpochDay,
                                  int epochMonthOffset, int minimumDaysInMonth, int maximumDaysInMonth,
                                  int maximumMonthsInYear, int minimumDaysInYear, int maximumDaysInYear,
                                  Map<Integer, Integer> leapMonthByYear, int minimumYear, int maximumYear,
                                  String fingerprint) {
    /**
     * Copies the mutable inputs without validating the calendar data again.
     * 复制可变输入，不再次校验历法数据。
     *
     * @throws NullPointerException if the array, map, or any map key or value is {@code null}
     *                              如果数组、映射或映射中的任意键或值为 {@code null}
     */
    public ChineseCalendarData {
        monthStartEpochDays = monthStartEpochDays.clone();
        leapMonthByYear = Map.copyOf(leapMonthByYear);
    }

    /**
     * Returns a defensive copy of the padded month-start table.
     * 返回含填充槽位的月初表的防御性副本。
     *
     * @return a new array containing all month starts, padding, and the exclusive end bound
     *         包含所有月初、填充槽位和不包含在支持范围内的结束边界的新数组
     */
    @Override
    public int[] monthStartEpochDays() {
        return monthStartEpochDays.clone();
    }

    /**
     * Gets the table size, including padding and the final exclusive bound.
     * 获取表的大小，包括填充槽位及最后的不包含在支持范围内的边界。
     *
     * @return the number of table entries, not the number of actual months
     *         表条目数，并非实际月份数
     */
    public int monthStartEpochDayCount() {
        return monthStartEpochDays.length;
    }

    /**
     * Reads one table entry without copying the array.
     * 读取一个表条目，不复制数组。
     *
     * @param index the zero-based table index
     *              从零开始的表索引
     * @return the epoch day stored at that index
     *         该索引处保存的纪元天数
     * @throws IndexOutOfBoundsException if the index is outside the array
     *                                   如果索引超出数组范围
     */
    public int monthStartEpochDayAt(int index) {
        return monthStartEpochDays[index];
    }

    /**
     * Finds the last table entry whose start is not after the given epoch day.
     * Equal starts are resolved to the last entry to skip padding at year boundaries.
     * The caller must validate the supported date range.
     * 查找起始日不晚于给定纪元天数的最后一个表条目。
     * 起始日重复时取最后一个条目，以跳过年份边界的填充槽位。
     * 调用者须校验日期是否在支持范围内。
     *
     * @param epochDay the epoch day to locate
     *                 要定位的纪元天数
     * @return the padded table index, or {@code -1} if before the first entry
     *         含填充槽位的表索引；早于首个条目时为 {@code -1}
     */
    public int epochMonthForDay(int epochDay) {
        int index = Arrays.binarySearch(monthStartEpochDays, epochDay);
        if (index < 0) {
            index = -index - 2;
        }
        while (index + 1 < monthStartEpochDays.length && monthStartEpochDays[index + 1] == epochDay) {
            index++;
        }
        return index;
    }
}
