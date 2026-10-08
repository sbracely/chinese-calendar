package io.github.sbracely.chinese.calendar;

import java.time.format.TextStyle;
import java.util.Locale;

public class Main {
    static void main() {
        IO.println(ChineseChronology.INSTANCE.getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        IO.println(ChineseChronology.INSTANCE.getDisplayName(TextStyle.FULL, Locale.CHINA));
        IO.println(ChineseChronology.INSTANCE);
    }
}
