package io.github.sbracely.chinese.calendar;

public class Main {
    static void main() {

        ChineseDate date = ChineseChronology.INSTANCE.date(1999, 9, 1);
        IO.println(date);


    }
}
