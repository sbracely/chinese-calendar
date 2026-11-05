package io.github.sbracely.chinese.calendar;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamConstants;
import java.time.DateTimeException;
import java.time.temporal.ChronoField;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChineseDateSerializationTest {
    private static final ChineseChronology CHRONOLOGY = ChineseChronology.INSTANCE;

    static Stream<ChineseDate> dates() {
        int minimumYear = (int) CHRONOLOGY.range(ChronoField.YEAR).getMinimum();
        int maximumYear = (int) CHRONOLOGY.range(ChronoField.YEAR).getMaximum();
        return Stream.of(
                CHRONOLOGY.date(2024, 1, 1),
                CHRONOLOGY.date(2023, 2, 30),
                CHRONOLOGY.date(2023, 2, true, 1),
                CHRONOLOGY.date(2023, 2, true, 29),
                CHRONOLOGY.date(2023, 3, 1),
                CHRONOLOGY.date(2023, 12, 30),
                CHRONOLOGY.date(minimumYear, 1, 1),
                CHRONOLOGY.dateYearDay(maximumYear, CHRONOLOGY.date(maximumYear, 1, 1).lengthOfYear()));
    }

    @ParameterizedTest
    @MethodSource("dates")
    void restoresDateFieldsAndChronology(ChineseDate date) throws Exception {
        ChineseDate restored = (ChineseDate) deserialize(serialize(date));

        assertThat(restored).isEqualTo(date);
        assertThat(restored.getChronology()).isSameAs(CHRONOLOGY);
        assertThat(restored.isLeapMonth()).isEqualTo(date.isLeapMonth());
        assertThat(restored.toEpochDay()).isEqualTo(date.toEpochDay());
        assertThat(restored.plusDays(0)).isSameAs(restored);
    }

    static Stream<Arguments> invalidDates() {
        int maximumYear = (int) CHRONOLOGY.range(ChronoField.YEAR).getMaximum();
        return Stream.of(
                Arguments.of(CHRONOLOGY, 2023, 0, false, 1, DateTimeException.class),
                Arguments.of(CHRONOLOGY, 2023, 13, false, 1, DateTimeException.class),
                Arguments.of(CHRONOLOGY, 2023, 2, false, 0, DateTimeException.class),
                Arguments.of(CHRONOLOGY, 2023, 2, true, 30, DateTimeException.class),
                Arguments.of(CHRONOLOGY, 2024, 2, true, 1, DateTimeException.class),
                Arguments.of(CHRONOLOGY, 2023, 3, true, 1, DateTimeException.class),
                Arguments.of(CHRONOLOGY, maximumYear + 1, 1, false, 1, DateTimeException.class),
                Arguments.of(null, 2023, 2, false, 1, NullPointerException.class));
    }

    @ParameterizedTest
    @MethodSource("invalidDates")
    void rejectsInvalidProxyData(ChineseChronology chronology, int year, int month,
                                boolean leapMonth, int day, Class<? extends Throwable> cause) throws Exception {
        var constructor = Class.forName(ChineseDate.class.getName() + "$Ser").getDeclaredConstructor(
                ChineseChronology.class, int.class, int.class, boolean.class, int.class);
        constructor.setAccessible(true);
        byte[] bytes = serialize(constructor.newInstance(chronology, year, month, leapMonth, day));

        assertThatThrownBy(() -> deserialize(bytes))
                .isInstanceOf(InvalidObjectException.class)
                .hasCauseInstanceOf(cause);
    }

    @Test
    void rejectsDirectDeserialization() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        // Model the old default serialized form, whose transient fields were omitted.
        // 模拟旧的默认序列化格式，其中所有 transient 字段均被省略。
        try (DataOutputStream output = new DataOutputStream(bytes)) {
            output.writeShort(ObjectStreamConstants.STREAM_MAGIC);
            output.writeShort(ObjectStreamConstants.STREAM_VERSION);
            output.writeByte(ObjectStreamConstants.TC_OBJECT);
            output.writeByte(ObjectStreamConstants.TC_CLASSDESC);
            output.writeUTF(ChineseDate.class.getName());
            output.writeLong(1L);
            output.writeByte(ObjectStreamConstants.SC_SERIALIZABLE);
            output.writeShort(0);
            output.writeByte(ObjectStreamConstants.TC_ENDBLOCKDATA);
            output.writeByte(ObjectStreamConstants.TC_NULL);
        }

        assertThatThrownBy(() -> deserialize(bytes.toByteArray()))
                .isInstanceOf(InvalidObjectException.class)
                .hasMessage("Deserialization via serialization delegate");
    }

    @ParameterizedTest
    @CsvSource({"Unknown,chinese,invalid", "Chinese,unknown,invalid", "Chinese,chinese,invalid",
            "Chinese,chinese,"})
    void rejectsUnknownOrUnverifiableChronology(String id, String type, String fingerprint) throws Exception {
        var constructor = Class.forName(ChineseChronology.class.getName() + "$Ser")
                .getDeclaredConstructor(String.class, String.class, String.class);
        constructor.setAccessible(true);
        byte[] bytes = serialize(constructor.newInstance(id, type, fingerprint));

        assertThatThrownBy(() -> deserialize(bytes)).isInstanceOf(InvalidObjectException.class);
    }

    private static byte[] serialize(Object value) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(value);
        }
        return bytes.toByteArray();
    }

    private static Object deserialize(byte[] bytes) throws Exception {
        try (ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return input.readObject();
        }
    }
}
