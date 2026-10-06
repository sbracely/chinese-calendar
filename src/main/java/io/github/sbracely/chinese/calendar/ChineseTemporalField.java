package io.github.sbracely.chinese.calendar;

import java.time.format.ResolverStyle;
import java.time.temporal.*;
import java.util.Locale;
import java.util.Map;

public enum ChineseTemporalField implements TemporalField {
    IS_LEAP_MONTH("IsLeapMonth", ChronoUnit.MONTHS, ChronoUnit.MONTHS, ValueRange.of(0, 1)),
    MONTH_AS_ORDINAL_OF_YEAR("MonthAsOrdinalOfYear", ChronoUnit.MONTHS, ChronoUnit.YEARS, ValueRange.of(1, 12, 13));

    private final String name;
    private final TemporalUnit baseUnit;
    private final TemporalUnit rangeUnit;
    private final ValueRange range;

    ChineseTemporalField(String name, TemporalUnit baseUnit, TemporalUnit rangeUnit, ValueRange range) {
        this.name = name;
        this.baseUnit = baseUnit;
        this.rangeUnit = rangeUnit;
        this.range = range;
    }

    @Override
    public String getDisplayName(Locale locale) {
        return name;
    }

    @Override
    public TemporalUnit getBaseUnit() {
        return baseUnit;
    }

    @Override
    public TemporalUnit getRangeUnit() {
        return rangeUnit;
    }

    @Override
    public ValueRange range() {
        return range;
    }

    @Override
    public boolean isDateBased() {
        return true;
    }

    @Override
    public boolean isTimeBased() {
        return false;
    }

    @Override
    public boolean isSupportedBy(TemporalAccessor temporal) {
        return temporal instanceof ChineseDate date && date.isSupported(this);
    }

    @Override
    public ValueRange rangeRefinedBy(TemporalAccessor temporal) {
        if (!(temporal instanceof ChineseDate date)) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + this);
        }
        int year = date.prolepticYear();
        return switch (this) {
            case IS_LEAP_MONTH -> ValueRange.of(0, date.getChronology().isLeapYear(year) ? 1 : 0);
            case MONTH_AS_ORDINAL_OF_YEAR ->
                    ValueRange.of(1, date.getChronology().isLeapYear(year) ? 13 : 12);
        };
    }

    @Override
    public long getFrom(TemporalAccessor temporal) {
        if (!isSupportedBy(temporal)) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + this);
        }
        return temporal.getLong(this);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R extends Temporal> R adjustInto(R temporal, long newValue) {
        if (!isSupportedBy(temporal)) {
            throw new UnsupportedTemporalTypeException("Unsupported field: " + this);
        }
        rangeRefinedBy(temporal).checkValidValue(newValue, this);
        return (R) temporal.with(this, newValue);
    }

    @Override
    public TemporalAccessor resolve(Map<TemporalField, Long> fieldValues, TemporalAccessor partialTemporal, ResolverStyle resolverStyle) {
        return TemporalField.super.resolve(fieldValues, partialTemporal, resolverStyle);
    }
}
