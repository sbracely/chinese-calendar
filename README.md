# Chinese Calendar 农历

A Chinese calendar library built on Java's `java.time.chrono` API. By default, the data covers years 1900–2099
in the Chinese calendar.  
基于 Java `java.time.chrono` API 实现的中国农历日期库，默认农历数据覆盖农历 1900–2099 年。

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
- Automatic `Chronology` registration and lookup by name  
  `Chronology` 自动注册与按名称查找

## Source of data 数据来源

1. *Calendars for 1900–2025* and *Calendar Data for 2026*, published by the Purple Mountain Observatory.  
   紫金山天文台 《1900—2025年日历》、《2026年日历资料》。
2. *Gregorian-Lunar Calendar Conversion Table*, published by the Hong Kong Observatory.  
   香港天文台 《公历与农历日期对照表》

## Requirements 依赖环境

- JDK 25
- Maven 3.9.x
