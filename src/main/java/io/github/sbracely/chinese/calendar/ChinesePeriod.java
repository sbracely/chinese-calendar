package io.github.sbracely.chinese.calendar;

import java.io.Serializable;
import java.time.DateTimeException;
import java.time.chrono.ChronoPeriod;
import java.time.chrono.Chronology;
import java.time.temporal.*;
import java.util.List;
import java.util.Objects;

import static java.time.temporal.ChronoUnit.*;

public record ChinesePeriod(ChineseChronology chronology, int years, int months, int days)
        implements ChronoPeriod, Serializable {

    private static final List<TemporalUnit> UNITS = List.of(YEARS, MONTHS, DAYS);

    public ChinesePeriod {
        Objects.requireNonNull(chronology, "chronology");
    }

    @Override
    public long get(TemporalUnit unit) {
        Objects.requireNonNull(unit, "unit");
        return switch (unit) {
            case ChronoUnit.YEARS -> years;
            case ChronoUnit.MONTHS -> months;
            case ChronoUnit.DAYS -> days;
            default -> throw new UnsupportedTemporalTypeException("Unsupported unit: " + unit);
        };
    }

    @Override
    public List<TemporalUnit> getUnits() {
        return UNITS;
    }

    @Override
    public Chronology getChronology() {
        return chronology;
    }

    @Override
    public ChronoPeriod plus(TemporalAmount amount) {
        ChronoPeriod other = validateAmount(amount);
        return new ChinesePeriod(chronology,
                Math.addExact(years, Math.toIntExact(other.get(YEARS))),
                Math.addExact(months, Math.toIntExact(other.get(MONTHS))),
                Math.addExact(days, Math.toIntExact(other.get(DAYS))));
    }

    @Override
    public ChronoPeriod minus(TemporalAmount amount) {
        ChronoPeriod other = validateAmount(amount);
        return new ChinesePeriod(chronology,
                Math.subtractExact(years, Math.toIntExact(other.get(YEARS))),
                Math.subtractExact(months, Math.toIntExact(other.get(MONTHS))),
                Math.subtractExact(days, Math.toIntExact(other.get(DAYS))));
    }

    private ChronoPeriod validateAmount(TemporalAmount amount) {
        Objects.requireNonNull(amount, "amount");
        if (!(amount instanceof ChronoPeriod period)) {
            throw new DateTimeException("Amount must be a ChronoPeriod: " + amount);
        }
        if (!chronology.equals(period.getChronology())) {
            throw new DateTimeException("Chronology mismatch, expected: " + chronology.getId()
                    + ", actual: " + period.getChronology().getId());
        }
        return period;
    }

    @Override
    public ChronoPeriod multipliedBy(int scalar) {
        return new ChinesePeriod(chronology, Math.multiplyExact(years, scalar),
                Math.multiplyExact(months, scalar), Math.multiplyExact(days, scalar));
    }

    @Override
    public ChronoPeriod normalized() {
        // A lunar year has 12 or 13 actual months, so no date-independent conversion is possible.
        return this;
    }

    @Override
    public Temporal addTo(Temporal temporal) {
        validateChronology(temporal);
        if (years != 0) {
            temporal = temporal.plus(years, YEARS);
        }
        if (months != 0) {
            temporal = temporal.plus(months, MONTHS);
        }
        if (days != 0) {
            temporal = temporal.plus(days, DAYS);
        }
        return temporal;
    }

    @Override
    public Temporal subtractFrom(Temporal temporal) {
        validateChronology(temporal);
        if (years != 0) {
            temporal = temporal.minus(years, YEARS);
        }
        if (months != 0) {
            temporal = temporal.minus(months, MONTHS);
        }
        if (days != 0) {
            temporal = temporal.minus(days, DAYS);
        }
        return temporal;
    }

    private void validateChronology(Temporal temporal) {
        Objects.requireNonNull(temporal, "temporal");
        Chronology actual = temporal.query(TemporalQueries.chronology());
        if (actual != null && !chronology.equals(actual)) {
            throw new DateTimeException("Chronology mismatch, expected: " + chronology.getId()
                    + ", actual: " + actual.getId());
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(chronology.getId()).append(" P");
        if (isZero()) {
            return result.append("0D").toString();
        }
        if (years != 0) {
            result.append(years).append('Y');
        }
        if (months != 0) {
            result.append(months).append('M');
        }
        if (days != 0) {
            result.append(days).append('D');
        }
        return result.toString();
    }
}
