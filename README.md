# Chinese Calendar

English | [简体中文](README.zh-CN.md)

A Chinese calendar library built on Java's Date-Time API (`java.time.chrono`),
with a default data range from January 31, 1900 to February 8, 2100 (Gregorian calendar).

## Features

### Implemented

- Java Date-Time API support

  **`ChineseDate` — `ChronoLocalDate` implementation**

  Gregorian-date conversion, leap-month identification, and date arithmetic.

  ```java
  ChineseDate chineseDate = ChineseDate.now();
  LocalDate gregorianDate = LocalDate.from(chineseDate);
  ChineseDate convertedDate = ChineseDate.from(gregorianDate);
  
  int year = chineseDate.get(ChronoField.YEAR);
  int lengthOfMonth = chineseDate.lengthOfMonth();
  int lengthOfYear = chineseDate.lengthOfYear();
  long epochDay = chineseDate.toEpochDay();
  
  ChineseDate nextMonth = chineseDate.plus(1, ChronoUnit.MONTHS);
  ChineseDate previousDay = chineseDate.plus(-1, ChronoUnit.DAYS);
  ChineseDate firstDayOfMonth = chineseDate.with(ChronoField.DAY_OF_MONTH, 1);
  long daysUntilNextMonth = chineseDate.until(nextMonth, ChronoUnit.DAYS);

  ChineseDate leapMonthDate = ChineseDate.of(2023, 2, true, 1);
  boolean leapMonth = leapMonthDate.isLeapMonth();
  boolean leapYear = leapMonthDate.isLeapYear();
  ChineseDate followingMonth = leapMonthDate.plusMonths(1);
  ChineseDate followingYear = leapMonthDate.plusYears(1);
  ChineseDate followingDay = leapMonthDate.plusDays(1);
  ```

  **`ChineseChronology` — `Chronology` implementation**

  Date and period creation, leap-year queries, and leap-month lookup.

  ```java
  ChineseChronology chineseChronology = ChineseChronology.INSTANCE;
  ChineseDate date = chineseChronology.dateNow();
  LocalDate gregorianDate = LocalDate.now();
  ChineseDate fromIsoDate = chineseChronology.date(gregorianDate);
  ChineseDate fromEpochDay = chineseChronology.dateEpochDay(gregorianDate.toEpochDay());
  ChineseDate fromFields = chineseChronology.date(2026, 1, 1);
  ChineseDate leapMonthDate = chineseChronology.date(2023, 2, true, 1);
  boolean leapYear = chineseChronology.isLeapYear(2023);
  int leapMonth = chineseChronology.getLeapMonth(2023);
  ChinesePeriod period = chineseChronology.period(1, 2, 3);
  ```

  **`ChinesePeriod` — `ChronoPeriod` implementation**

  Period factories, component operations, interval calculation, and text parsing.

  ```java
  ChineseDate chineseDate = ChineseDate.now();
  ChinesePeriod period = ChinesePeriod.of(1, 2, 3);
  int monthComponent = period.months();
  ChineseDate result = chineseDate.plus(period);
  ChineseDate earlier = chineseDate.minus(period);
  ChinesePeriod elapsed = chineseDate.until(result);
  ChinesePeriod between = ChinesePeriod.between(chineseDate, result);
  ChinesePeriod parsed = ChinesePeriod.parse(period.toString());

  ChinesePeriod zero = ChinesePeriod.ZERO;
  ChinesePeriod months = ChinesePeriod.ofMonths(13);
  ChinesePeriod weeks = ChinesePeriod.ofWeeks(2);
  ChinesePeriod adjusted = period.withYears(2).plusMonths(1).minusDays(1);
  ChinesePeriod copied = ChinesePeriod.from(elapsed);
  ```

  Leap months count as separate months; years and months are not automatically converted.
  Periods are applied in year-month-day order.

- Automatic `Chronology` discovery via SPI and lookup by ID or calendar type

  ```java
  ChineseChronology byId = (ChineseChronology) Chronology.of("Chinese");
  ChineseChronology byType = (ChineseChronology) Chronology.of("chinese");
  ```

- Custom extension of Chinese calendar data

  Configure before the first calendar-data access.

  <details>
  <summary>Configuration priorities and examples</summary>

  Configuration is selected in priority order.

  File-based examples assume `my-calendar.properties` contains valid calendar configuration.

    1. `ChineseCalendarConfigLoader.configure(Path)` or `ChineseCalendarConfigLoader.configure(Properties)` — explicit
       file path or in-memory properties.

       ```java
       ChineseCalendarConfigLoader.configure(Path.of("my-calendar.properties"));
       ```

       Or use in-memory properties (a single-year example).

       ```java
       Properties properties = new Properties();
       properties.setProperty("id", "Chinese");
       properties.setProperty("type", "chinese");
       properties.setProperty("iso-start", "1900-01-31");
       properties.setProperty("month-length.1900", "29 30 29 29 30 29 30 30 leap29 30 30 29 30");
       ChineseCalendarConfigLoader.configure(properties);
       ```

    2. `ChineseCalendarConfigProvider` implementations registered through
       `META-INF/services/io.github.sbracely.chinese.calendar.spi.ChineseCalendarConfigProvider`, ordered by
       `getOrder()` (default: `0`). Override `getPath()` or `getProperties()` to supply a source;
       the loader handles reading and copying the configuration. Both empty skips the provider;
       supplying both sources is rejected.

       ```java
       package example;
  
       public final class MyCalendarConfigProvider implements ChineseCalendarConfigProvider {
           @Override
           public Optional<Path> getPath() {
               return Optional.of(Path.of("my-calendar.properties"));
           }
       }
       ```

       For in-memory configuration, use this alternative provider implementation.
       It supplies only lunar year 1900 and requires the same service registration:

       ```java
       package example;

       public final class MyCalendarConfigProvider implements ChineseCalendarConfigProvider {
           @Override
           public Optional<Properties> getProperties() {
               Properties properties = new Properties();
               properties.setProperty("id", "Chinese");
               properties.setProperty("type", "chinese");
               properties.setProperty("iso-start", "1900-01-31");
               properties.setProperty("month-length.1900",
                       "29 30 29 29 30 29 30 30 leap29 30 30 29 30");
               return Optional.of(properties);
           }
       }
       ```

       When registering multiple providers, assign each a distinct order. Lower values have
       higher priority; the default is `0`, and duplicate orders are rejected.
       For example, add this method to a provider:

       ```java
       @Override
       public int getOrder() {
           return 100;
       }
       ```

       A provider with order `100` is evaluated before one with order `200`.

       Add the provider name to the service registration file.

       ```text
       example.MyCalendarConfigProvider
       ```

    3. JVM property `chinese.calendar.config`.

       ```java
       System.setProperty("chinese.calendar.config", "my-calendar.properties");
       ```

       Or set it when starting your application (`-D` must precede `-jar`):

       ```powershell
       java -Dchinese.calendar.config=my-calendar.properties -jar my-app.jar
       ```

    4. Environment variable `CHINESE_CALENDAR_CONFIG`.

       Set it before starting the JVM (PowerShell).

       ```powershell
       $env:CHINESE_CALENDAR_CONFIG = "my-calendar.properties"
       ```

    5. `chinese-calendar-config.properties` in the JVM working directory.

    6. Bundled `chinese-calendar-config.properties` resource.

  </details>

### Planned Features

- The 24 solar terms
- Sexagenary cycle dating
- Chinese zodiac signs
- Formatting and localized text for calendar dates

## Default data sources

1. *Calendars for 1900–2025* and *Calendar Data for 2026*, published by the Purple Mountain Observatory.
2. The 2027–2100 portion of the *Gregorian-Lunar Calendar Conversion Table*, published by the
   Hong Kong Observatory.

## Localization data source

Localization data is sourced from **Unicode CLDR 49**.

## Requirements

- JDK 25
- Maven 3.9.x
