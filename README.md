# Chinese Calendar 农历

A Chinese calendar library integrated with Java's Date-Time API (`java.time.chrono`),
with a default data range from January 31, 1900 to February 8, 2100 (Gregorian calendar).  
与 Java 日期时间 API（`java.time.chrono`）集成的中国农历库。默认数据范围：公元 1900 年农历庚子年正月初一至公元 2099
年农历己未年腊月三十。

## Features 功能

### Implemented 已实现

- Conversion between Gregorian calendar dates and Chinese calendar dates  
  公历与农历相互转换
- Calculation of month and year lengths  
  月份、年份长度计算
- Java Date-Time API support through `Chronology` and `ChronoLocalDate`  
  基于 `Chronology`、`ChronoLocalDate` 的 Java Date-Time API 支持
- Date arithmetic and calendar-specific periods<br>
  日期加减和农历日期间隔计算
- Automatic `Chronology` discovery via SPI and lookup by ID or calendar type  
  通过 SPI 自动发现历法，支持按 ID 或历法类型查找
- Custom extension of Chinese calendar data<br>
  自定义扩展农历数据

### Planned Features 计划实现

- The 24 solar terms  
  二十四节气
- Sexagenary cycle dating  
  干支纪年
- Chinese zodiac signs  
  生肖
- Formatting and localized text for calendar dates  
  农历日期格式化和本地化文本

## Custom Chinese calendar data extensions 自定义扩展农历数据

Configuration is selected in priority order. Call `ConfigLoader.configure(...)` before first use.<br>
配置按优先级选择。须在首次使用前调用 `ConfigLoader.configure(...)`。

1. `ConfigLoader.configure(Path)` or `ConfigLoader.configure(Properties)` — explicit file path or in-memory properties.<br>
   显式指定文件路径或内存中的配置属性。
2. `ConfigSource` implementations registered through
   `META-INF/services/io.github.sbracely.chinese.calendar.spi.ConfigSource`, ordered by
   `getOrder()` (lower values come first).<br>
   通过 Java SPI 注册的 `ConfigSource`，按 `getOrder()` 升序排列（数值越小越优先）。
3. JVM property `chinese.calendar.config` (order 100).<br>
   JVM 系统属性 `chinese.calendar.config`（顺序值 100）。
4. Environment variable `CHINESE_CALENDAR_CONFIG` (order 200).<br>
   环境变量 `CHINESE_CALENDAR_CONFIG`（顺序值 200）。
5. `chinese-calendar-config.properties` in the JVM working directory (order 300).<br>
   JVM 工作目录下的同名文件（顺序值 300）。
6. Bundled `chinese-calendar-config.properties` resource (order 400).<br>
   JAR 内置的同名资源（顺序值 400）。

## Default data sources 默认数据来源

1. *Calendars for 1900–2025* and *Calendar Data for 2026*, published by the Purple Mountain Observatory.  
   紫金山天文台 《1900—2025年日历》、《2026年日历资料》。
2. The 2027–2100 portion of the *Gregorian-Lunar Calendar Conversion Table*, published by the
   Hong Kong Observatory.
   香港天文台 《公历与农历日期对照表》(2027 - 2100 年部分)

## Requirements 依赖环境

- JDK 25
- Maven 3.9.x
