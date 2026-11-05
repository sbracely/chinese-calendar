package io.github.sbracely.chinese.calendar;

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
    void readsRelativePathFromApplicationProperties() throws Exception {
        fixture("custom");
        application("application-relative");
        run("application", null);
    }

    @Test
    void readsAbsolutePathFromApplicationProperties() throws Exception {
        Path data = fixture("custom");
        application("application-relative");
        Properties properties = new Properties();
        Path application = directory.resolve("application.properties");
        try (var reader = Files.newBufferedReader(application)) {
            properties.load(reader);
        }
        properties.setProperty("chinese.calendar.config", data.toAbsolutePath().toString());
        try (var writer = Files.newBufferedWriter(application)) {
            properties.store(writer, null);
        }
        run("application", null);
    }

    @Test
    void systemPropertyOverridesApplicationProperties() throws Exception {
        application("application-missing");
        run("application", fixture("custom").toString());
    }

    @Test
    void acceptsRelativeSystemPropertyPath() throws Exception {
        fixture("custom");
        run("application", "custom.properties");
    }

    @Test
    void blankSystemPropertyDoesNotFallBackToApplicationConfiguration() throws Exception {
        fixture("custom");
        application("application-relative");
        run("invalid", "");
    }

    @Test
    void missingApplicationKeyUsesBuiltInData() throws Exception {
        application("application-default");
        run("default", null);
    }

    @ParameterizedTest
    @ValueSource(strings = {"application-missing", "application-blank"})
    void invalidApplicationPathDoesNotFallBack(String name) throws Exception {
        application(name);
        run("application-invalid", null);
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

    private void application(String name) throws Exception {
        Files.copy(fixture(name), directory.resolve("application.properties"),
                StandardCopyOption.REPLACE_EXISTING);
    }

    private void run(String action, String config, String... arguments) throws Exception {
        var command = new ArrayList<String>();
        String executable = System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java";
        command.add(Path.of(System.getProperty("java.home"), "bin", executable).toString());
        if (config != null) {
            command.add("-Dchinese.calendar.config=" + config);
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
        Process process = new ProcessBuilder(command).directory(directory.toFile())
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
        ChineseChronology chronology = (ChineseChronology) Chronology.of("Chinese");
        System.clearProperty("chinese.calendar.config");
        switch (args[0]) {
            case "shared" -> {
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
                Files.delete(Path.of(config));
                assertThat(ChineseChronology.INSTANCE.date(2023, 1, 1).toEpochDay()).isEqualTo(first);
                assertThat(Chronology.of("chinese").date(2023, 1, 1).toEpochDay()).isEqualTo(first);
                assertThat(new ChineseChronology().date(2023, 1, 1).toEpochDay()).isEqualTo(first);
                assertThatThrownBy(() -> chronology.dateEpochDay(first - 1)).isInstanceOf(DateTimeException.class);
                assertThatThrownBy(() -> chronology.dateEpochDay(last + 1)).isInstanceOf(DateTimeException.class);
            }
            case "common" -> {
                ChineseDate first = chronology.date(2024, 1, 1);
                ChineseDate last = chronology.date(2024, 12, 29);
                assertThat(first.toEpochDay()).isEqualTo(LocalDate.of(2024, 2, 10).toEpochDay());
                assertThat(first.lengthOfYear()).isEqualTo(354);
                assertThat(first.plusMonths(11).plusDays(28)).isEqualTo(last);
                assertThat(chronology.dateEpochDay(last.toEpochDay())).isEqualTo(last);
                assertThatThrownBy(() -> last.plusDays(1)).isInstanceOf(DateTimeException.class);
            }
            case "application" -> {
                assertThat(chronology.date(2023, 1, 1).toEpochDay())
                        .isEqualTo(LocalDate.of(2023, 1, 23).toEpochDay());
                assertThat(ChineseChronology.INSTANCE.date(2023, 1, 1))
                        .isEqualTo(chronology.date(2023, 1, 1));
            }
            case "default" -> assertThat(chronology.date(1900, 1, 1).toEpochDay())
                    .isEqualTo(LocalDate.of(1900, 1, 31).toEpochDay());
            case "application-invalid" -> assertThatThrownBy(() -> chronology.date(2023, 1, 1))
                    .isInstanceOf(DateTimeException.class);
            case "invalid" -> {
                Throwable failure = catchThrowable(() -> chronology.date(2023, 1, 1));
                assertThat(failure).isInstanceOf(DateTimeException.class);
                if (!config.isBlank()) {
                    try (var input = ChineseChronologyCustomDataTest.class
                            .getResourceAsStream("/calendar-config/custom.properties")) {
                        Files.copy(input, Path.of(config), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                assertThat(catchThrowable(() -> new ChineseChronology().date(2023, 1, 1))).isSameAs(failure);
            }
            case "write" -> {
                try (var output = new ObjectOutputStream(Files.newOutputStream(Path.of(args[1])))) {
                    output.writeObject(chronology.date(2023, 2, true, 29));
                    output.writeObject(chronology);
                    output.writeObject(chronology.period(1, 2, 3));
                }
            }
            case "read" -> {
                try (var input = new ObjectInputStream(Files.newInputStream(Path.of(args[1])))) {
                    ChineseDate restored = (ChineseDate) input.readObject();
                    assertThat(restored).isEqualTo(chronology.date(2023, 2, true, 29));
                    assertThat(restored.getChronology()).isSameAs(ChineseChronology.INSTANCE);
                    assertThat(input.readObject()).isSameAs(ChineseChronology.INSTANCE);
                    assertThat(input.readObject()).isEqualTo(chronology.period(1, 2, 3));
                }
            }
            case "mismatch" -> {
                try (var input = new ObjectInputStream(Files.newInputStream(Path.of(args[1])))) {
                    assertThatThrownBy(input::readObject).isInstanceOf(InvalidObjectException.class)
                            .hasMessageContaining("fingerprint mismatch");
                }
            }
            default -> throw new AssertionError("Unknown action: " + args[0]);
        }
    }
}
