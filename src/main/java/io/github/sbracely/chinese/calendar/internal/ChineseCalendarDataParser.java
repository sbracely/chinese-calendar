package io.github.sbracely.chinese.calendar.internal;

import io.github.sbracely.chinese.calendar.ChineseTemporalField;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.ValueRange;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Parses and validates Chinese calendar configuration properties.
 * 解析并校验农历配置属性。
 */
public final class ChineseCalendarDataParser {

    private static final String KEY_ID = "id";
    private static final String KEY_TYPE = "type";
    private static final String KEY_VERSION = "version";
    private static final String KEY_ISO_START = "iso-start";
    private static final String KEY_MONTH_LENGTH_PREFIX = "month-length.";
    private static final String LEAP_PREFIX = "leap";

    private ChineseCalendarDataParser() {
    }

    public static Result parse(Properties properties, String expectedId, String expectedType) {
        String id = properties.getProperty(KEY_ID);
        if (!expectedId.equals(id)) {
            throw new IllegalArgumentException("Configuration is for a different calendar: " + id);
        }
        String type = properties.getProperty(KEY_TYPE);
        if (!expectedType.equals(type)) {
            throw new IllegalArgumentException("Configuration is for a different calendar type: " + type);
        }
        String version = properties.getProperty(KEY_VERSION);
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("Configuration does not contain a version");
        }

        String isoStartValue = properties.getProperty(KEY_ISO_START);
        if (isoStartValue == null || isoStartValue.isEmpty()) {
            throw new IllegalArgumentException("Configuration does not contain a ISO start date");
        }
        int minimumEpochDay;
        try {
            minimumEpochDay = Math.toIntExact(LocalDate.parse(
                    isoStartValue, DateTimeFormatter.ISO_LOCAL_DATE).toEpochDay());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid ISO start date, expected format yyyy-MM-dd", e);
        }

        Map<Integer, int[]> monthsByYear = new TreeMap<>();
        Map<Integer, Integer> leapMonthByYear = new HashMap<>();
        int minimumYear = Integer.MAX_VALUE;
        int maximumYear = Integer.MIN_VALUE;
        int maximumMonthsInYear = Integer.MIN_VALUE;
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String key = (String) entry.getKey();
            if (!key.startsWith(KEY_MONTH_LENGTH_PREFIX)) {
                continue;
            }
            int year;
            try {
                year = Integer.parseInt(key.substring(KEY_MONTH_LENGTH_PREFIX.length()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("bad key: " + key, e);
            }
            ChronoField.YEAR_OF_ERA.checkValidValue(year);
            ParsedMonths parsedMonths = parseMonthLengths(year, (String) entry.getValue());
            minimumYear = Math.min(minimumYear, year);
            maximumYear = Math.max(maximumYear, year);
            maximumMonthsInYear = Math.max(maximumMonthsInYear, parsedMonths.monthLengths.length);
            if (parsedMonths.leapMonth != 0) {
                leapMonthByYear.put(year, parsedMonths.leapMonth);
            }
            if (monthsByYear.put(year, parsedMonths.monthLengths) != null) {
                throw new IllegalArgumentException("Duplicate calendar year: " + year);
            }
        }

        if (monthsByYear.isEmpty()
                || (long) maximumYear - minimumYear + 1 != monthsByYear.size()) {
            throw new IllegalArgumentException("Configuration must contain consecutive calendar years");
        }
        int epochMonthOffset = Math.multiplyExact(minimumYear, maximumMonthsInYear);
        Math.multiplyExact(Math.addExact(maximumYear, 1), maximumMonthsInYear);
        MonthStartData monthStartData = createMonthStartEpochDays(
                monthsByYear.values(), minimumEpochDay, maximumMonthsInYear);
        int[] monthStartEpochDays = monthStartData.monthStartEpochDays;
        int maximumEpochDay = monthStartEpochDays[monthStartEpochDays.length - 1] - 1;
        return new Result(monthStartEpochDays, minimumEpochDay, maximumEpochDay, epochMonthOffset,
                monthStartData.minimumDaysInMonth, monthStartData.maximumDaysInMonth,
                maximumMonthsInYear, monthStartData.minimumDaysInYear, monthStartData.maximumDaysInYear,
                leapMonthByYear, minimumYear, maximumYear,
                dataFingerprint(minimumEpochDay, monthsByYear, leapMonthByYear));
    }

    private static ParsedMonths parseMonthLengths(int year, String monthLengthConfigLine) {
        String[] monthLengthStrings = monthLengthConfigLine.strip().split("\\s+");
        int leapMonthCount = (int) Arrays.stream(monthLengthStrings)
                .filter(monthLength -> monthLength.startsWith(LEAP_PREFIX)).count();
        int monthCount = monthLengthStrings.length;
        ValueRange monthRange = ChineseTemporalField.MONTH_AS_ORDINAL_OF_YEAR.range();
        boolean validCommonYear = monthCount == monthRange.getSmallestMaximum() && leapMonthCount == 0;
        boolean validLeapYear = monthCount == monthRange.getMaximum() && leapMonthCount == 1;
        if (!validCommonYear && !validLeapYear) {
            throw new IllegalArgumentException("Invalid month length config year: " + year
                    + ", line: " + monthLengthConfigLine);
        }

        int[] monthLengths = new int[monthCount];
        int leapMonth = 0;
        for (int i = 0; i < monthCount; i++) {
            String monthLength = monthLengthStrings[i];
            String numericMonthLength = monthLength;
            if (numericMonthLength.startsWith(LEAP_PREFIX)) {
                if (i == 0) {
                    throw new IllegalArgumentException("Leap month must follow a regular month in year: " + year);
                }
                numericMonthLength = numericMonthLength.substring(LEAP_PREFIX.length());
                leapMonth = i;
            }
            try {
                monthLengths[i] = Integer.parseInt(numericMonthLength);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("bad month length: " + monthLength + " in year: " + year, e);
            }
            if (monthLengths[i] != 29 && monthLengths[i] != 30) {
                throw new IllegalArgumentException("Invalid month length: " + monthLength + " in year: " + year);
            }
        }
        return new ParsedMonths(monthLengths, leapMonth);
    }

    private static MonthStartData createMonthStartEpochDays(Collection<int[]> daysOfMonthsCollection,
                                                            int minimumEpochDay, int maximumMonthsInYear) {
        int[] monthStartEpochDays = new int[Math.addExact(
                Math.multiplyExact(daysOfMonthsCollection.size(), maximumMonthsInYear), 1)];
        int minimumDaysInMonth = Integer.MAX_VALUE;
        int maximumDaysInMonth = Integer.MIN_VALUE;
        int minimumDaysInYear = Integer.MAX_VALUE;
        int maximumDaysInYear = Integer.MIN_VALUE;
        int index = 0;
        for (int[] daysOfMonths : daysOfMonthsCollection) {
            int daysInYear = 0;
            for (int dayOfMonth : daysOfMonths) {
                if (dayOfMonth < 0) {
                    throw new IllegalArgumentException("Invalid month length: " + dayOfMonth);
                }
                monthStartEpochDays[index++] = minimumEpochDay;
                minimumEpochDay = Math.addExact(minimumEpochDay, dayOfMonth);
                daysInYear += dayOfMonth;
                minimumDaysInMonth = Math.min(minimumDaysInMonth, dayOfMonth);
                maximumDaysInMonth = Math.max(maximumDaysInMonth, dayOfMonth);
            }
            minimumDaysInYear = Math.min(minimumDaysInYear, daysInYear);
            maximumDaysInYear = Math.max(maximumDaysInYear, daysInYear);
            for (int i = daysOfMonths.length; i < maximumMonthsInYear; i++) {
                monthStartEpochDays[index++] = minimumEpochDay;
            }
        }
        monthStartEpochDays[index++] = minimumEpochDay;
        if (index != monthStartEpochDays.length) {
            throw new IllegalStateException("Did not fill epochMonths exactly: ndx = " + index
                    + " should be " + monthStartEpochDays.length);
        }
        return new MonthStartData(monthStartEpochDays, minimumDaysInMonth,
                maximumDaysInMonth, minimumDaysInYear, maximumDaysInYear);
    }

    private static String dataFingerprint(int minimumEpochDay, Map<Integer, int[]> months,
                                          Map<Integer, Integer> leapMonths) {
        StringBuilder content = new StringBuilder().append(minimumEpochDay);
        months.forEach((year, lengths) -> content.append('|').append(year).append(':')
                .append(Arrays.toString(lengths)).append(':').append(leapMonths.getOrDefault(year, 0)));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(content.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    public record Result(int[] monthStartEpochDays, int minimumEpochDay, int maximumEpochDay,
                         int epochMonthOffset, int minimumDaysInMonth, int maximumDaysInMonth,
                         int maximumMonthsInYear, int minimumDaysInYear, int maximumDaysInYear,
                         Map<Integer, Integer> leapMonthByYear, int minimumYear, int maximumYear,
                         String fingerprint) {
        public Result {
            monthStartEpochDays = monthStartEpochDays.clone();
            leapMonthByYear = Map.copyOf(leapMonthByYear);
        }

        @Override
        public int[] monthStartEpochDays() {
            return monthStartEpochDays.clone();
        }
    }

    private record ParsedMonths(int[] monthLengths, int leapMonth) {
    }

    private record MonthStartData(int[] monthStartEpochDays, int minimumDaysInMonth, int maximumDaysInMonth,
                                  int minimumDaysInYear, int maximumDaysInYear) {
    }
}
