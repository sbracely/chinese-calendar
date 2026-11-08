# 农历

[English](README.md) | 简体中文

基于 Java 日期时间 API（`java.time.chrono`）实现的中国农历库。  
默认数据范围：公元 1900 年农历庚子年正月初一至公元 2099 年农历己未年腊月三十。

## 功能

### 已实现

- Java Date-Time API 支持

  **`ChineseDate` — 实现 `ChronoLocalDate`**

  公历日期转换、闰月识别和日期运算。

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

  ChineseDate leapMonthDate = ChineseChronology.INSTANCE.date(2023, 2, true, 1);
  boolean leapMonth = leapMonthDate.isLeapMonth();
  ChineseDate followingMonth = leapMonthDate.plusMonths(1);
  ChineseDate followingYear = leapMonthDate.plusYears(1);
  ChineseDate followingDay = leapMonthDate.plusDays(1);
  ```

  **`ChineseChronology` — 实现 `Chronology`**

  日期和期间创建、闰年查询及闰月查询。

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

  **`ChinesePeriod` — 实现 `ChronoPeriod`**

  期间工厂方法、分量操作、日期间隔计算和文本解析。

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

  闰月单独计月，年月不自动换算；期间按年、月、日顺序应用。

- 通过 SPI 自动发现历法，支持按 ID 或历法类型查找

  ```java
  ChineseChronology byId = (ChineseChronology) Chronology.of("Chinese");
  ChineseChronology byType = (ChineseChronology) Chronology.of("chinese");
  ```

- 自定义扩展农历数据

  首次访问历法数据前配置。

  <details>
  <summary>配置优先级及示例</summary>

  配置按优先级选择。

  文件示例假定 `my-calendar.properties` 包含有效的历法配置。

    1. `ChineseCalendarConfigLoader.configure(Path)` 或 `ChineseCalendarConfigLoader.configure(Properties)`：
       显式指定文件路径或内存中的配置属性。

       ```java
       ChineseCalendarConfigLoader.configure(Path.of("my-calendar.properties"));
       ```

       或使用内存属性（以下仅包含一个年份）。

       ```java
       Properties properties = new Properties();
       properties.setProperty("id", "Chinese");
       properties.setProperty("type", "chinese");
       properties.setProperty("iso-start", "1900-01-31");
       properties.setProperty("month-length.1900", "29 30 29 29 30 29 30 30 leap29 30 30 29 30");
       ChineseCalendarConfigLoader.configure(properties);
       ```

    2. 通过 Java SPI 注册的 `ChineseCalendarConfigProvider`，按 `getOrder()` 排序（默认为 `0`）。
       重写 `getPath()` 或 `getProperties()` 提供来源，由加载器读取或复制配置。
       两者均为空时跳过此提供者，同时提供两种来源则报错。
       服务注册文件为 `META-INF/services/io.github.sbracely.chinese.calendar.spi.ChineseCalendarConfigProvider`。

       ```java
       package example;

       public final class MyCalendarConfigProvider implements ChineseCalendarConfigProvider {
           @Override
           public Optional<Path> getPath() {
               return Optional.of(Path.of("my-calendar.properties"));
           }
       }
       ```

       内存配置使用以下替代实现，仅提供农历 1900 年的数据，同样需要服务注册：

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

       注册多个提供者时，应为每个提供者设置不同的顺序。数值越小优先级越高，
       默认为 `0`，顺序重复会报错。例如，在提供者类中添加：

       ```java
       @Override
       public int getOrder() {
           return 100;
       }
       ```

       顺序为 `100` 的提供者会先于顺序为 `200` 的提供者被查询。

       在上述服务注册文件中写入提供者类名。

       ```text
       example.MyCalendarConfigProvider
       ```

    3. JVM 系统属性 `chinese.calendar.config`。

       ```java
       System.setProperty("chinese.calendar.config", "my-calendar.properties");
       ```

    4. 环境变量 `CHINESE_CALENDAR_CONFIG`。

       在启动 JVM 前设置（PowerShell）。

       ```powershell
       $env:CHINESE_CALENDAR_CONFIG = "my-calendar.properties"
       ```

    5. JVM 工作目录下的 `chinese-calendar-config.properties` 文件。

       在 JVM 工作目录中执行（PowerShell）。

       ```powershell
       Copy-Item .\my-calendar.properties .\chinese-calendar-config.properties
       ```

    6. JAR 内置的 `chinese-calendar-config.properties` 资源。

       构建前替换内置配置（PowerShell）。

       ```powershell
       Copy-Item .\my-calendar.properties .\src\main\resources\chinese-calendar-config.properties
       ```

  </details>

### 计划实现

- 二十四节气
- 干支纪年
- 生肖
- 农历日期格式化和本地化文本

## 默认数据来源

1. 紫金山天文台《1900—2025年日历》、《2026年日历资料》。
2. 香港天文台《公历与农历日期对照表》（2027–2100 年部分）。

## 本地化数据来源

本地化数据来自 **Unicode CLDR 49**。

## 依赖环境

- JDK 25
- Maven 3.9.x
