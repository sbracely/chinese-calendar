# Chinese Calendar 农历

A Chinese calendar library built on Java's `java.time.chrono` API. By default, the data covers years 1900–2099
in the Chinese calendar.  
基于 Java `java.time.chrono` API 实现的中国农历日期库，默认农历数据覆盖农历 1900–2099 年。

The project is currently under development, and its API may change.  
项目目前处于开发阶段，API 可能发生变化。

## Source of data 数据来源

1. *Calendars for 1900–2025* and *Calendar Data for 2026*, published by the Purple Mountain Observatory.<br>
   紫金山天文台发布的《1900—2025年日历》及《2026年日历资料》。
   > The Purple Mountain Observatory may publish calendar data for additional years in the future. The data
   > can be extended through a custom configuration file.<br>
   > 后续紫金山天文台可能会发布新年份的日历资料。通过自定义配置文件可以进行扩展。  
   > Some solar-term dates in *Calendars for 1900–2025* differ from those recorded in older Chinese
   > almanacs, as noted in the footnotes. A custom configuration file can be used to select the dates from
   > the older almanacs instead.<br>
   > 《1900—2025年日历》中有几天节气日期与旧历书所载有偏差，在注脚中有标记说明。通过自定义配置文件可以选择采用旧历书的节气日期。

2. The 2027–2100 section of the *Gregorian-Lunar Calendar Conversion Table*, published by the Hong Kong
   Observatory.<br>
   香港天文台发布的《公历与农历日期对照表》中的 2027-2100 部分。
   > Calculations of lunar phases and solar-term times several decades in advance may have errors of a few
   > minutes. Therefore, future calendar data in the *Gregorian-Lunar Calendar Conversion Table* may be
   > revised and can be updated through a custom configuration file.<br>
   > 由于计算数十年后的月相及节气时间可能会有误差，因此《公历与农历日期对照表》中未来的日历数据可能会被修订。通过自定义配置文件可以进行更新。

## Features 功能

### Implemented 已实现

- Conversion between Gregorian calendar dates and Chinese calendar dates  
  公历与农历相互转换
- Calculation of month and year lengths  
  月份、年份长度计算
- Java Date-Time API support through `Chronology` and `ChronoLocalDate`  
  基于 `Chronology`、`ChronoLocalDate` 的 Java Date-Time API 支持

### Planned Features 计划实现

- Custom extension of Chinese calendar data  
  自定义扩展农历数据
- Date arithmetic and period calculations  
  日期加减和日期间隔计算
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

## Requirements 依赖环境

- JDK 25
- Maven 3.9.x
