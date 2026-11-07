package io.github.sbracely.chinese.calendar.internal;

import java.util.Arrays;
import java.util.Map;

/**
 * Immutable parsed Chinese calendar data.
 * 解析后的不可变农历数据。
 */
public record ChineseCalendarData(int[] monthStartEpochDays, int minimumEpochDay, int maximumEpochDay,
                                  int epochMonthOffset, int minimumDaysInMonth, int maximumDaysInMonth,
                                  int maximumMonthsInYear, int minimumDaysInYear, int maximumDaysInYear,
                                  Map<Integer, Integer> leapMonthByYear, int minimumYear, int maximumYear,
                                  String fingerprint) {
    public ChineseCalendarData {
        monthStartEpochDays = monthStartEpochDays.clone();
        leapMonthByYear = Map.copyOf(leapMonthByYear);
    }

    @Override
    public int[] monthStartEpochDays() {
        return monthStartEpochDays.clone();
    }

    public int monthStartEpochDayCount() {
        return monthStartEpochDays.length;
    }

    public int monthStartEpochDayAt(int index) {
        return monthStartEpochDays[index];
    }

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
