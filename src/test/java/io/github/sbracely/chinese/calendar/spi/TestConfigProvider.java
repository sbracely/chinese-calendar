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
    public Optional<ChineseCalendarConfigData> findConfig() throws IOException {
        String path = System.getProperty("test.chinese.calendar.config.spi");
        if (path == null) {
            return Optional.empty();
        }
        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(Path.of(path))) {
            properties.load(input);
        }
        return Optional.of(new ChineseCalendarConfigData("test SPI Properties", properties));
    }

    public static class EarlyProvider extends TestConfigProvider {
        @Override
        public int getOrder() {
            return 100;
        }

        @Override
        public Optional<ChineseCalendarConfigData> findConfig() throws IOException {
            invocations.add("early");
            return Boolean.getBoolean("test.chinese.calendar.config.early.empty")
                    ? Optional.empty() : super.findConfig();
        }
    }

    public static class LaterProvider extends TestConfigProvider {
        @Override
        public int getOrder() {
            return 200;
        }

        @Override
        public Optional<ChineseCalendarConfigData> findConfig() throws IOException {
            invocations.add("later");
            if (Boolean.getBoolean("test.chinese.calendar.config.later.fail")) {
                throw new IOException("Later provider must not be evaluated");
            }
            return super.findConfig();
        }
    }

    public static class SameOrderProvider extends EarlyProvider {
    }
}
