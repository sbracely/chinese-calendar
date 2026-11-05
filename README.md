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

The built-in data is stored in `chinese-calendar-data-config.properties`. To replace it,
create an external properties file, for example `/etc/chinese-calendar/calendar.properties`:<br>
内置数据保存在 `chinese-calendar-data-config.properties` 中。如需替换，
请创建外部配置文件，例如 `/etc/chinese-calendar/calendar.properties`：

```properties
version=1
id=Chinese
type=chinese
iso-start=2024-02-10
month-length.2024=29 30 29 29 30 29 30 30 29 30 30 29
```

Select it using a JVM system property:<br>
通过 JVM 系统属性指定文件：

```bash
java -Dchinese.calendar.config=/etc/chinese-calendar/calendar.properties -jar app.jar
```

Alternatively, set the same key in the UTF-8 `application.properties` resource at the
classpath root (for example, `src/main/resources/application.properties`):<br>
也可在类路径根目录的 UTF-8 `application.properties` 资源中指定同名键
（例如 `src/main/resources/application.properties`）：

```properties
chinese.calendar.config=data/calendar.properties
```

Both absolute and relative paths are supported. Relative paths use the JVM working directory,
not the directory containing `application.properties`.
System properties take precedence. If neither source supplies the key, built-in data is used.
The library reads the first classpath `application.properties` directly; framework profiles,
YAML, placeholder substitution, and configuration merging are not supported.<br>
支持绝对路径和相对路径。相对路径基于 JVM 工作目录，而非 `application.properties` 所在目录。
系统属性优先；两处均未提供该键时使用内置数据。
库直接读取类路径中的首个 `application.properties`，不处理框架 profile、YAML、占位符替换或配置合并。

The ID and type remain `Chinese` and `chinese`. No subclass or additional SPI declaration is needed:<br>
ID 和类型保持为 `Chinese` 和 `chinese`，无需继承或添加额外的 SPI 声明：

```java
Chronology chronology = Chronology.of("Chinese");
var date = chronology.date(2024, 1, 1);
```

`iso-start` is the Gregorian date of the first configured lunar year's first day. Years must be
positive and consecutive. A common year has 12 months; a leap year has 13, with one leap month
marked by the `leap` prefix (for example, `29 30 leap29 ...` for a leap second month).
Each month must have 29 or 30 days, and the leap month cannot be the first entry.<br>
`iso-start` 是首个配置农历年正月初一对应的公历日期。年份必须为正数且连续。
平年为 12 个月，闰年为 13 个月且恰有一个闰月，用 `leap` 前缀标记（如 `29 30 leap29 ...` 表示闰二月）。
每月只能为 29 或 30 天，闰月不能是首项。

The path is selected when `ChineseChronology` is initialized. Data is loaded and validated on
first use, then shared by all instances, including SPI-created instances. It cannot be switched
or reloaded at runtime. A missing or invalid external file causes an error without falling back
to built-in data. The file replaces the entire data set; it is not merged with built-in years.<br>
初始化 `ChineseChronology` 时选定路径，首次使用数据时加载并校验，之后所有实例（包括 SPI 创建的实例）
共享同一套数据，不支持运行时切换或重新加载。外部文件不存在或内容无效时直接报错，不回退到内置数据。
自定义文件替换整套数据，不与内置年份合并。

Serialized chronologies, dates, and periods include a data fingerprint. Deserialization requires
matching effective data, not matching file paths or version labels. Old streams without a
fingerprint are rejected. Calendar data is not embedded in the serialized stream.<br>
序列化的历法、日期和期间包含数据指纹；反序列化要求有效数据一致，不要求文件路径或版本标签相同。
不含指纹的旧序列化数据会被拒绝，序列化流不包含历法数据。

## Source of data 数据来源

1. *Calendars for 1900–2025* and *Calendar Data for 2026*, published by the Purple Mountain Observatory.  
   紫金山天文台 《1900—2025年日历》、《2026年日历资料》。
2. *Gregorian-Lunar Calendar Conversion Table*, published by the Hong Kong Observatory.  
   香港天文台 《公历与农历日期对照表》

## Requirements 依赖环境

- JDK 25
- Maven 3.9.x
