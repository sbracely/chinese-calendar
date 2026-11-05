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

The built-in data is stored in `chinese-calendar-config.properties`. To replace it,
create an external properties file, for example `/etc/chinese-calendar/calendar.properties`:<br>
内置数据保存在 `chinese-calendar-config.properties` 中。如需替换，
请创建外部配置文件，例如 `/etc/chinese-calendar/calendar.properties`：

```properties
version=1
id=Chinese
type=chinese
iso-start=2024-02-10
month-length.2024=29 30 29 29 30 29 30 30 29 30 30 29
```

Select it programmatically before the first calendar data access:<br>
在首次访问历法数据前，通过代码指定：

```java
ChineseChronology.configure(Path.of("/etc/chinese-calendar/calendar.properties"));
Chronology chronology = Chronology.of("Chinese");
```

Or use a JVM system property or environment variable:<br>
也可通过 JVM 系统属性或环境变量指定：

```bash
java -Dchinese.calendar.config=/etc/chinese-calendar/calendar.properties -jar app.jar
```

```bash
CHINESE_CALENDAR_CONFIG=/etc/chinese-calendar/calendar.properties java -jar app.jar
```

An optional Java SPI provider can supply the configuration path under the
`chinese.calendar.config` property. Implement `ConfigProvider` and register its public
no-argument implementation in
`META-INF/services/io.github.sbracely.chinese.calendar.spi.ConfigProvider`:<br>
也可通过 Java SPI 提供配置路径，属性名为 `chinese.calendar.config`。实现 `ConfigProvider`，
并在 `META-INF/services/io.github.sbracely.chinese.calendar.spi.ConfigProvider` 中注册其公开无参实现类：

```java
public class CalendarConfigProvider implements ConfigProvider {
    @Override
    public String getChineseCalendarConfig() {
        return "/etc/chinese-calendar/calendar.properties";
    }
}
```

```text
com.example.CalendarConfigProvider
```

`ServiceLoader` creates the provider itself; it is not a Spring bean and does not receive
constructor injection. Register at most one provider. For Spring `Environment` configuration,
use Spring startup code to resolve the path and call `configure(Path)` before calendar data is
first accessed.<br>
`ServiceLoader` 会自行创建提供者，因此它不是 Spring Bean，也不会进行构造器注入。最多注册一个提供者。
若配置来自 Spring `Environment`，应在 Spring 启动阶段解析路径，并在首次访问历法数据前调用
`configure(Path)`。

Both absolute and relative paths are supported. Relative paths use the JVM working directory.
Configuration precedence is `ChineseChronology.configure(Path)`, SPI provider,
system property, environment variable, `chinese-calendar-config.properties` in the JVM working
directory, then the built-in resource. If the working-directory file exists but is unreadable or
invalid, initialization fails without falling back. The library does not read application
configuration files. Frameworks such as Spring Boot can resolve the active profile and pass the
selected path to `configure` during application startup, before any chronology or date accesses
calendar data.<br>
支持绝对路径和相对路径。相对路径基于 JVM 工作目录。配置优先级依次为
`ChineseChronology.configure(Path)`、SPI 提供者、系统属性、环境变量、JVM 工作目录下的
`chinese-calendar-config.properties`，最后是 JAR 内置资源。若工作目录下的文件存在但无法读取或无效，
初始化会失败，不会回退。库本身不读取应用配置文件；Spring Boot 等框架可先解析当前 profile，
再于应用启动期间、任何历法或日期访问数据之前，将选定路径传给 `configure`。

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

The SPI provider, system property, and environment variable are read when `ChineseChronology` is
initialized; the working-directory file is checked on first data access. The selected file is
loaded and validated on first use, then shared by all instances, including SPI-created instances.
`configure` must be called before that first use and can only be called once. Data cannot be
switched or reloaded at runtime. A missing or invalid explicitly selected file causes an error
without falling back. The file replaces the entire data set; it is not merged with built-in years.<br>
SPI 提供者、系统属性和环境变量在 `ChineseChronology` 初始化时读取；工作目录配置文件在首次访问数据时检查。选定文件
在首次使用时加载、校验，并由所有实例（包括 SPI 创建的实例）共享。`configure` 必须在首次使用数据前调用，
且只能调用一次。运行时不能切换或重新加载数据。显式选定的外部文件不存在或无效时直接报错，不会回退；
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
