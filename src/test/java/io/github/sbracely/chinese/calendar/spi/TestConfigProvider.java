package io.github.sbracely.chinese.calendar.spi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

public class TestConfigProvider implements ChineseCalendarConfigProvider {

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
}
