package io.github.sbracely.chinese.calendar.spi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class TestConfigProvider implements ChineseCalendarConfigProvider {

    public static final List<String> invocations = new ArrayList<>();

    @Override
    public int getOrder() {
        return 500;
    }

    @Override
    public Optional<Path> getPath() throws IOException {
        String path = System.getProperty("test.chinese.calendar.config.spi");
        return path == null ? Optional.empty() : Optional.of(Path.of(path));
    }

    public static class EarlyProvider extends TestConfigProvider {
        @Override
        public int getOrder() {
            return 100;
        }

        @Override
        public Optional<Path> getPath() throws IOException {
            invocations.add("early");
            return Boolean.getBoolean("test.chinese.calendar.config.early.empty")
                    ? Optional.empty() : super.getPath();
        }
    }

    public static class LaterProvider extends TestConfigProvider {
        @Override
        public int getOrder() {
            return 200;
        }

        @Override
        public Optional<Path> getPath() throws IOException {
            invocations.add("later");
            if (Boolean.getBoolean("test.chinese.calendar.config.later.fail")) {
                throw new IOException("Later provider must not be evaluated");
            }
            return super.getPath();
        }
    }

    public static class SameOrderProvider extends EarlyProvider {
    }

    public static class PropertiesProvider implements ChineseCalendarConfigProvider {
        public static Properties supplied;

        @Override
        public Optional<Properties> getProperties() throws IOException {
            Properties defaults = new Properties();
            try (InputStream input = Files.newInputStream(
                    Path.of(System.getProperty("test.chinese.calendar.config.spi")))) {
                defaults.load(input);
            }
            supplied = new Properties(defaults);
            return Optional.of(supplied);
        }
    }

    public static class BothSourcesProvider extends TestConfigProvider {
        @Override
        public int getOrder() {
            return 100;
        }

        @Override
        public Optional<Properties> getProperties() {
            return Optional.of(new Properties());
        }
    }
}
