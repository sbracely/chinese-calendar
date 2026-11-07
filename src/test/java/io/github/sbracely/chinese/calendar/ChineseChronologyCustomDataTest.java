package io.github.sbracely.chinese.calendar;

import io.github.sbracely.chinese.calendar.spi.ChineseCalendarConfigData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.chrono.Chronology;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

class ChineseChronologyCustomDataTest {
    @TempDir
    Path directory;

    @Test
    void sharesSelectedDataAcrossSingletonConstructedAndSpiInstances() throws Exception {
        run("shared", fixture("custom").toString());
    }

    @Test
    void supportsDataContainingOnlyOneCommonYear() throws Exception {
        run("common", fixture("common-only").toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"empty", "gap", "month", "leap", "month-count", "id", "type",
            "year", "duplicate-year", "overflow"})
    void rejectsInvalidDataWithoutFallingBackOrRetrying(String name) throws Exception {
        run("invalid", fixture("invalid-" + name).toString());
    }

    @Test
    void rejectsMissingAndBlankPaths() throws Exception {
        run("invalid", directory.resolve("missing.properties").toString());
        run("invalid", "");
        run("invalid", "   ");
    }

    @Test
    void configuresAbsolutePathBeforeLoadingData() throws Exception {
        run("configure", null, fixture("custom").toString());
    }

    @Test
    void configuresPropertiesBeforeLoadingData() throws Exception {
        runWithEnvironment("configure-properties", fixture("common-only").toString(),
                fixture("common-only").toString(), new String[]{fixture("custom").toString()});
    }

    @Test
    void copiesInheritedPropertiesWithOverridesAndWithoutSharingMutableState() {
        Properties defaults = new Properties();
        defaults.setProperty("id", "Chinese");
        defaults.setProperty("iso-start", "2023-01-22");
        Properties properties = new Properties(defaults);
        properties.setProperty("iso-start", "2023-01-23");

        ChineseCalendarConfigData data = new ChineseCalendarConfigData("test", properties);
        defaults.setProperty("id", "changed");
        properties.clear();

        Properties copy = data.properties();
        assertThat(copy.getProperty("id")).isEqualTo("Chinese");
        assertThat(copy.getProperty("iso-start")).isEqualTo("2023-01-23");
        assertThat(copy.entrySet()).hasSize(2);
        copy.clear();
        assertThat(data.properties().getProperty("id")).isEqualTo("Chinese");
    }

    @Test
    void configuresRelativePathAgainstJvmWorkingDirectory() throws Exception {
        fixture("custom");
        run("configure-relative", null);
    }

    @Test
    void environmentVariableSelectsDataFile() throws Exception {
        String path = fixture("custom").toString();
        runWithEnvironment("shared", null, path, new String[]{path});
    }

    @Test
    void systemPropertyTakesPrecedenceOverEnvironmentVariable() throws Exception {
        runWithEnvironment("shared", fixture("custom").toString(), fixture("common-only").toString(),
                new String[0]);
    }

    @Test
    void spiProviderTakesPrecedenceOverSystemProperty() throws Exception {
        runWithSpiProvider("shared", fixture("common-only").toString(), fixture("custom").toString());
    }

    @Test
    void cachesSpiLoadingFailureWithoutFallingBackOrRetrying() throws Exception {
        Path descriptor = directory.resolve(Path.of("META-INF", "services",
                "io.github.sbracely.chinese.calendar.spi.ChineseCalendarConfigProvider"));
        Files.createDirectories(descriptor.getParent());
        Files.writeString(descriptor, "io.github.sbracely.chinese.calendar.spi.MissingConfigProvider");
        run("invalid-spi", null, descriptor.toString());
    }

    @Test
    void usesWorkingDirectoryConfigurationBeforeBuiltInData() throws Exception {
        Path localConfig = workingDirectoryConfiguration("custom");
        run("shared", null, localConfig.toString());
    }

    @Test
    void systemPropertyTakesPrecedenceOverWorkingDirectoryConfiguration() throws Exception {
        workingDirectoryConfiguration("custom");
        run("common", fixture("common-only").toString());
    }

    @Test
    void environmentVariableTakesPrecedenceOverWorkingDirectoryConfiguration() throws Exception {
        Path localConfig = workingDirectoryConfiguration("common-only");
        String externalConfig = fixture("custom").toString();
        runWithEnvironment("shared", null, externalConfig, new String[]{localConfig.toString()});
    }

    @Test
    void invalidWorkingDirectoryConfigurationDoesNotFallBackToBuiltInData() throws Exception {
        Path localConfig = workingDirectoryConfiguration("invalid-month");
        run("invalid", null, localConfig.toString());
    }

    @Test
    void explicitConfigurationTakesPrecedenceOverSystemPropertyAndEnvironment() throws Exception {
        String custom = fixture("custom").toString();
        String common = fixture("common-only").toString();
        runWithEnvironment("configure-common", custom, custom, new String[]{common});
    }

    @Test
    void missingConfigurationSourcesUseBuiltInData() throws Exception {
        run("default", null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void blankSystemPropertyDoesNotFallBackToEnvironment(String path) throws Exception {
        runWithEnvironment("invalid", path, fixture("custom").toString(), new String[0]);
    }

    @Test
    void restoresDatesAndPeriodsOnlyWithMatchingEffectiveData() throws Exception {
        Path serialized = directory.resolve("calendar.ser");
        String config = fixture("custom").toString();
        run("write", config, serialized.toString());
        run("read", config, serialized.toString());
        run("mismatch", null, serialized.toString());
        run("read", fixture("equivalent").toString(), serialized.toString());
        run("mismatch", fixture("changed-leap").toString(), serialized.toString());
    }

    private Path fixture(String name) throws Exception {
        Path destination = directory.resolve(name + ".properties");
        try (var input = getClass().getResourceAsStream("/calendar-config/" + name + ".properties")) {
            assertThat(input).isNotNull();
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        return destination;
    }

    private Path workingDirectoryConfiguration(String name) throws Exception {
        Path destination = directory.resolve("chinese-calendar-config.properties");
        try (var input = getClass().getResourceAsStream("/calendar-config/" + name + ".properties")) {
            assertThat(input).isNotNull();
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        }
        return destination;
    }

    private void run(String action, String config, String... arguments) throws Exception {
        runInChild(action, config, null, arguments);
    }

    private void runWithEnvironment(String action, String config, String environmentPath, String[] arguments)
            throws Exception {
        runInChild(action, config, environmentPath, arguments);
    }

    private void runInChild(String action, String config, String environmentPath, String... arguments)
            throws Exception {
        runInChild(action, config, environmentPath, null, arguments);
    }

    private void runWithSpiProvider(String action, String config, String providerPath, String... arguments)
            throws Exception {
        runInChild(action, config, null, providerPath, arguments);
    }

    private void runInChild(String action, String config, String environmentPath, String providerPath,
                            String[] arguments) throws Exception {
        var command = new ArrayList<String>();
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        command.add(Path.of(System.getProperty("java.home"), "bin", executable).toString());
        if (config != null) {
            command.add("-Dchinese.calendar.config=" + config);
        }
        if (providerPath != null) {
            command.add("-Dtest.chinese.calendar.config.spi=" + providerPath);
        }
        String separator = System.getProperty("path.separator");
        String classpath = Stream.of(System.getProperty("surefire.test.class.path",
                        System.getProperty("java.class.path")).split(separator))
                .map(entry -> Path.of(entry).toAbsolutePath().toString()).collect(Collectors.joining(separator));
        command.add("-cp");
        command.add(directory + separator + classpath);
        command.add(getClass().getName());
        command.add(action);
        command.addAll(List.of(arguments));
        Path log = directory.resolve("child-jvm.log");
        ProcessBuilder builder = new ProcessBuilder(command).directory(directory.toFile());
        builder.environment().remove("CHINESE_CALENDAR_CONFIG");
        if (environmentPath != null) {
            builder.environment().put("CHINESE_CALENDAR_CONFIG", environmentPath);
        }
        Process process = builder
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertThat(process.waitFor(30, TimeUnit.SECONDS)).as("child JVM completed").isTrue();
            assertThat(process.exitValue()).as(Files.readString(log)).isZero();
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String config = System.getProperty("chinese.calendar.config");
        ChineseChronology chronology;
        switch (args[0]) {
            case "configure" -> {
                ChineseCalendarConfigLoader.configure(Path.of(args[1]));
                System.clearProperty("chinese.calendar.config");
                chronology = (ChineseChronology) Chronology.of("Chinese");
                assertThat(chronology.date(2023, 1, 1).toEpochDay())
                        .isEqualTo(LocalDate.of(2023, 1, 23).toEpochDay());
                assertThatThrownBy(() -> ChineseCalendarConfigLoader.configure(Path.of(args[1])))
                        .isInstanceOf(IllegalStateException.class);
            }
            case "configure-relative" -> {
                ChineseCalendarConfigLoader.configure(Path.of("custom.properties"));
                chronology = ChineseChronology.INSTANCE;
                assertThat(chronology.date(2023, 1, 1).toEpochDay())
                        .isEqualTo(LocalDate.of(2023, 1, 23).toEpochDay());
            }
            case "configure-properties" -> {
                Properties properties = new Properties();
                try (var input = Files.newInputStream(Path.of(args[1]))) {
                    properties.load(input);
                }
                ChineseCalendarConfigLoader.configure(properties);
                properties.clear();
                chronology = (ChineseChronology) Chronology.of("Chinese");
                assertThat(chronology.date(2023, 1, 1).toEpochDay())
                        .isEqualTo(LocalDate.of(2023, 1, 23).toEpochDay());
                assertThatThrownBy(() -> ChineseCalendarConfigLoader.configure(new Properties()))
                        .isInstanceOf(IllegalStateException.class);
            }
            case "configure-common" -> {
                ChineseCalendarConfigLoader.configure(Path.of(args[1]));
                chronology = (ChineseChronology) Chronology.of("Chinese");
                assertThat(chronology.range(ChronoField.YEAR).getMinimum()).isEqualTo(2024);
                assertThat(chronology.date(2024, 1, 1).toEpochDay())
                        .isEqualTo(LocalDate.of(2024, 2, 10).toEpochDay());
                assertThatThrownBy(() -> ChineseCalendarConfigLoader.configure(Path.of(args[1])))
                        .isInstanceOf(IllegalStateException.class);
            }
            case "shared" -> {
                chronology = (ChineseChronology) Chronology.of("Chinese");
                long first = LocalDate.of(2023, 1, 23).toEpochDay();
                IntStream.range(0, 32).parallel().forEach(i ->
                        assertThat(new ChineseChronology().date(2023, 1, 1).toEpochDay()).isEqualTo(first));
                assertThat(chronology.range(ChronoField.YEAR).getMinimum()).isEqualTo(2023);
                assertThat(chronology.range(ChronoField.YEAR).getMaximum()).isEqualTo(2024);
                long last = chronology.date(2024, 12, 29).toEpochDay();
                for (long day = first; day <= last; day++) {
                    ChineseDate date = chronology.dateEpochDay(day);
                    assertThat(chronology.date(date.get(ChronoField.YEAR), date.get(ChronoField.MONTH_OF_YEAR),
                            date.isLeapMonth(), date.get(ChronoField.DAY_OF_MONTH)).toEpochDay()).isEqualTo(day);
                }
                assertThat(chronology.date(2023, 2, 1).plusMonths(1))
                        .isEqualTo(chronology.date(2023, 2, true, 1));
                assertThat(chronology.date(2023, 12, 30).plusDays(1)).isEqualTo(chronology.date(2024, 1, 1));
                Path selectedConfig = Path.of(args.length > 1 ? args[1] : config);
                Files.delete(selectedConfig);
                assertThatThrownBy(() -> ChineseCalendarConfigLoader.configure(selectedConfig))
                        .isInstanceOf(IllegalStateException.class);
                assertThat(ChineseChronology.INSTANCE.date(2023, 1, 1).toEpochDay()).isEqualTo(first);
                assertThat(Chronology.of("chinese").date(2023, 1, 1).toEpochDay()).isEqualTo(first);
                assertThat(new ChineseChronology().date(2023, 1, 1).toEpochDay()).isEqualTo(first);
                assertThatThrownBy(() -> chronology.dateEpochDay(first - 1)).isInstanceOf(DateTimeException.class);
                assertThatThrownBy(() -> chronology.dateEpochDay(last + 1)).isInstanceOf(DateTimeException.class);
            }
            case "common" -> {
                chronology = (ChineseChronology) Chronology.of("Chinese");
                ChineseDate first = chronology.date(2024, 1, 1);
                ChineseDate last = chronology.date(2024, 12, 29);
                assertThat(first.toEpochDay()).isEqualTo(LocalDate.of(2024, 2, 10).toEpochDay());
                assertThat(first.lengthOfYear()).isEqualTo(354);
                assertThat(first.plusMonths(11).plusDays(28)).isEqualTo(last);
                assertThat(chronology.dateEpochDay(last.toEpochDay())).isEqualTo(last);
                assertThatThrownBy(() -> last.plusDays(1)).isInstanceOf(DateTimeException.class);
            }
            case "default" -> {
                chronology = (ChineseChronology) Chronology.of("Chinese");
                assertThat(chronology.date(1900, 1, 1).toEpochDay())
                        .isEqualTo(LocalDate.of(1900, 1, 31).toEpochDay());
            }
            case "invalid" -> {
                chronology = (ChineseChronology) Chronology.of("Chinese");
                Throwable failure = catchThrowable(() -> chronology.date(2023, 1, 1));
                assertThat(failure).isInstanceOf(DateTimeException.class);
                assertThatThrownBy(() -> ChineseCalendarConfigLoader.configure(new Properties()))
                        .isInstanceOf(IllegalStateException.class);
                if (config != null && !config.isBlank()) {
                    Files.writeString(Path.of(config), "");
                }
                assertThat(catchThrowable(() -> new ChineseChronology().date(2023, 1, 1))).isSameAs(failure);
            }
            case "invalid-spi" -> {
                chronology = (ChineseChronology) Chronology.of("Chinese");
                Throwable failure = catchThrowable(() -> chronology.date(2023, 1, 1));
                assertThat(failure).isInstanceOf(DateTimeException.class)
                        .hasCauseInstanceOf(ServiceConfigurationError.class);
                Files.delete(Path.of(args[1]));
                assertThat(catchThrowable(() -> new ChineseChronology().date(2023, 1, 1))).isSameAs(failure);
                assertThat(catchThrowable(() -> ChineseChronology.INSTANCE.date(2023, 1, 1))).isSameAs(failure);
                assertThatThrownBy(() -> ChineseCalendarConfigLoader.configure(new Properties()))
                        .isInstanceOf(IllegalStateException.class);
            }
            case "mismatch" -> {
                chronology = (ChineseChronology) Chronology.of("Chinese");
                try (var input = new ObjectInputStream(Files.newInputStream(Path.of(args[1])))) {
                    assertThatThrownBy(input::readObject).isInstanceOf(InvalidObjectException.class)
                            .hasMessageContaining("fingerprint mismatch");
                }
            }
            case "write" -> {
                chronology = (ChineseChronology) Chronology.of("Chinese");
                try (var output = new ObjectOutputStream(Files.newOutputStream(Path.of(args[1])))) {
                    output.writeObject(chronology.date(2023, 2, true, 29));
                    output.writeObject(chronology);
                    output.writeObject(chronology.period(1, 2, 3));
                }
            }
            case "read" -> {
                chronology = (ChineseChronology) Chronology.of("Chinese");
                try (var input = new ObjectInputStream(Files.newInputStream(Path.of(args[1])))) {
                    ChineseDate restored = (ChineseDate) input.readObject();
                    assertThat(restored).isEqualTo(chronology.date(2023, 2, true, 29));
                    assertThat(restored.getChronology()).isSameAs(ChineseChronology.INSTANCE);
                    assertThat(input.readObject()).isSameAs(ChineseChronology.INSTANCE);
                    assertThat(input.readObject()).isEqualTo(chronology.period(1, 2, 3));
                }
            }
            default -> throw new AssertionError("Unknown action: " + args[0]);
        }
    }
}
