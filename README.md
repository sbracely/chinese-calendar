# Chinese Calendar 农历

A Chinese calendar library integrated with Java's Date-Time API (`java.time.chrono`),
with a default data range from January 31, 1900 to February 8, 2100 (Gregorian calendar).  
与 Java 日期时间 API（`java.time.chrono`）集成的中国农历库。默认数据范围：公元 1900 年农历庚子年正月初一至公元 2099 年农历己未年腊月三十。

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

### Planned Features 计划实现

- Custom extension of Chinese calendar data  
  自定义扩展农历数据
- The 24 solar terms  
  二十四节气
- Sexagenary cycle dating  
  干支纪年
- Chinese zodiac signs  
  生肖
- Formatting and localized text for calendar dates  
  农历日期格式化和本地化文本

## Source of data 数据来源

1. *Calendars for 1900–2025* and *Calendar Data for 2026*, published by the Purple Mountain Observatory.  
   紫金山天文台 《1900—2025年日历》、《2026年日历资料》。
2. *Gregorian-Lunar Calendar Conversion Table*, published by the Hong Kong Observatory.  
   香港天文台 《公历与农历日期对照表》

## Requirements 依赖环境

- JDK 25
- Maven 3.9.x
