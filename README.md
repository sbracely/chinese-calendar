# Chinese Calendar 农历

A Chinese calendar library built on Java's Date-Time API (`java.time.chrono`),
with a default data range from January 31, 1900 to February 8, 2100 (Gregorian calendar).  
基于 Java 日期时间 API（`java.time.chrono`）实现的中国农历库。默认数据范围：公元 1900 年农历庚子年正月初一至公元 2099
年农历己未年腊月三十。

## Features 功能

### Implemented 已实现

- Java Date-Time API support<br>
  Java Date-Time API 支持

  **`ChronoLocalDate`**

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
  ```

  **`Chronology`**

  ```java
  ChineseChronology chineseChronology = ChineseChronology.INSTANCE;
  ChineseDate date = chineseChronology.dateNow();
  LocalDate gregorianDate = LocalDate.now();
  ChineseDate fromIsoDate = chineseChronology.date(gregorianDate);
  ChineseDate fromEpochDay = chineseChronology.dateEpochDay(gregorianDate.toEpochDay());
  ChineseDate fromFields = chineseChronology.date(2026, 1, 1);
  ```

  **`ChronoPeriod`**

  ```java
  ChineseDate chineseDate = ChineseDate.now();
  ChronoPeriod period = new ChinesePeriod(1, 2, 3);
  long monthComponent = period.get(ChronoUnit.MONTHS);
  ChineseDate result = chineseDate.plus(period);
  ChineseDate earlier = chineseDate.minus(period);
  ChronoPeriod elapsed = chineseDate.until(result);
  ```

- Automatic `Chronology` discovery via SPI and lookup by ID or calendar type<br>
  通过 SPI 自动发现历法，支持按 ID 或历法类型查找

  ```java
  Chronology byId = Chronology.of("Chinese");
  Chronology byType = Chronology.of("chinese");
  ```

- Custom extension of Chinese calendar data<br>
  自定义扩展农历数据

  Configure before the first calendar-data access.<br>
  首次访问历法数据前配置。

  <details>
  <summary>Configuration priorities and examples 配置优先级及示例</summary>

  Configuration is selected in priority order.<br>
  配置按优先级选择。

  File-based examples assume `my-calendar.properties` contains valid calendar configuration.<br>
  文件示例假定 `my-calendar.properties` 包含有效的历法配置。

    1. `ChineseCalendarConfigLoader.configure(Path)` or `ChineseCalendarConfigLoader.configure(Properties)` — explicit
       file path or in-memory properties.<br>
       显式指定文件路径或内存中的配置属性。

       ```java
       ChineseCalendarConfigLoader.configure(Path.of("my-calendar.properties"));
       ```

       Or use in-memory properties (a single-year example).<br>
       或使用内存属性（以下仅包含一个年份）。

       ```java
       Properties properties = new Properties();
       properties.setProperty("id", "Chinese");
       ...
       ChineseCalendarConfigLoader.configure(properties);
       ```

    2. `ChineseCalendarConfigProvider` implementations registered through
       `META-INF/services/io.github.sbracely.chinese.calendar.spi.ChineseCalendarConfigProvider`, ordered by
       `getOrder()`.<br>
       通过 Java SPI 注册的 `ChineseCalendarConfigProvider`，按 `getOrder()` 排序。

       ```java
       package example;
  
       public final class MyCalendarConfigProvider implements ChineseCalendarConfigProvider {
           @Override
           public int getOrder() {
               return 0;
           }
  
           @Override
           public Optional<ChineseCalendarConfigData> findConfig() throws IOException {
               return Optional.of(ChineseCalendarConfigData.fromPath(Path.of("my-calendar.properties")));
           }
       }
       ```

       Add the provider name to the service registration file.<br>
       在上述服务注册文件中写入提供者类名。

       ```text
       example.MyCalendarConfigProvider
       ```

    3. JVM property `chinese.calendar.config`.<br>
       JVM 系统属性 `chinese.calendar.config`。

       ```java
       System.setProperty("chinese.calendar.config", "my-calendar.properties");
       ```

    4. Environment variable `CHINESE_CALENDAR_CONFIG`.<br>
       环境变量 `CHINESE_CALENDAR_CONFIG`。

       Set it before starting the JVM (PowerShell).<br>
       在启动 JVM 前设置（PowerShell）。

       ```powershell
       $env:CHINESE_CALENDAR_CONFIG = "my-calendar.properties"
       ```

    5. `chinese-calendar-config.properties` in the JVM working directory.<br>
       JVM 工作目录下的同名文件。

       Run in the JVM working directory (PowerShell).<br>
       在 JVM 工作目录中执行（PowerShell）。

       ```powershell
       Copy-Item .\my-calendar.properties .\chinese-calendar-config.properties
       ```

    6. Bundled `chinese-calendar-config.properties` resource.<br>
       JAR 内置的同名资源。

       Replace the bundled configuration before building (PowerShell).<br>
       构建前替换内置配置（PowerShell）。

       ```powershell
       Copy-Item .\my-calendar.properties .\src\main\resources\chinese-calendar-config.properties
       ```

  </details>

### Planned Features 计划实现

- The 24 solar terms  
  二十四节气
- Sexagenary cycle dating  
  干支纪年
- Chinese zodiac signs  
  生肖
- Formatting and localized text for calendar dates  
  农历日期格式化和本地化文本

## Default data sources 默认数据来源

1. *Calendars for 1900–2025* and *Calendar Data for 2026*, published by the Purple Mountain Observatory.  
   紫金山天文台 《1900—2025年日历》、《2026年日历资料》。
2. The 2027–2100 portion of the *Gregorian-Lunar Calendar Conversion Table*, published by the
   Hong Kong Observatory.
   香港天文台 《公历与农历日期对照表》(2027 - 2100 年部分)

## Localization data source 本地化数据来源

Localization data is sourced from **Unicode CLDR 49**.<br>
本地化数据来自 **Unicode CLDR 49**。

## Requirements 依赖环境

- JDK 25
- Maven 3.9.x
