package io.github.sbracely.chinese.calendar;

import io.github.sbracely.chinese.calendar.spi.ConfigProvider;

public class TestConfigProvider implements ConfigProvider {

    @Override
    public String getChineseCalendarConfig() {
        return System.getProperty("test.chinese.calendar.config.spi");
    }
}
