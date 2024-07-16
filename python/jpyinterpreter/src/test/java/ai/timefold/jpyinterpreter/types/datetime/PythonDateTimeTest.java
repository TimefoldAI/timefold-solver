package ai.timefold.jpyinterpreter.types.datetime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.Duration;
import java.time.LocalDateTime;

import ai.timefold.jpyinterpreter.types.PythonString;

import org.junit.jupiter.api.Test;

public class PythonDateTimeTest {
    @Test
    public void testIsoFormat() {
        PythonDateTime pythonDateTime = new PythonDateTime(LocalDateTime.of(2002, 3, 11, 1, 30, 45));
        assertThat(pythonDateTime.iso_format()).isEqualTo(PythonString.valueOf("2002-03-11T01:30:45"));
    }

    @Test
    public void testCTime() {
        PythonDateTime pythonDateTime = new PythonDateTime(LocalDateTime.of(2002, 3, 11, 1, 30, 45));
        assertThat(pythonDateTime.ctime()).isEqualTo(PythonString.valueOf("Mon Mar 11 01:30:45 2002"));
    }

    @Test
    public void testAddTimeDelta() {
        PythonDateTime a = new PythonDateTime(LocalDateTime.of(2000, 1, 1, 0, 0, 0));
        PythonTimeDelta timeDelta = new PythonTimeDelta(Duration.ofDays(2).plusHours(1));
        assertThat(a.add_time_delta(timeDelta)).isEqualTo(new PythonDateTime(LocalDateTime.of(2000, 1, 3, 1, 0, 0)));
    }

    @Test
    public void testSubtractTimeDelta() {
        PythonDateTime a = new PythonDateTime(LocalDateTime.of(2000, 1, 3, 1, 0, 0));
        PythonTimeDelta timeDelta = new PythonTimeDelta(Duration.ofDays(2).plusHours(1));
        assertThat(a.subtract_time_delta(timeDelta)).isEqualTo(new PythonDateTime(LocalDateTime.of(2000, 1, 1, 0, 0, 0)));
    }

    @Test
    public void testSubtractDateTime() {
        PythonDateTime a = new PythonDateTime(LocalDateTime.of(2000, 1, 1, 0, 0, 0));
        PythonDateTime b = new PythonDateTime(LocalDateTime.of(2000, 1, 2, 2, 3, 4));
        assertThat(b.subtract_date_time(a)).isEqualTo(new PythonTimeDelta(Duration.ofDays(1L)
                .plusHours(2L)
                .plusMinutes(3L)
                .plusSeconds(4L)));
        assertThat(a.subtract_date_time(b)).isEqualTo(new PythonTimeDelta(Duration.ofDays(1L)
                .plusHours(2L)
                .plusMinutes(3L)
                .plusSeconds(4L)
                .negated()));
    }

    @Test
    public void testCompareDateTime() {
        PythonDateTime a = new PythonDateTime(LocalDateTime.of(2000, 1, 1, 0, 0, 0));
        PythonDateTime b = new PythonDateTime(LocalDateTime.of(2000, 1, 3, 0, 0, 0));
        assertThat(b.compareTo(a)).isGreaterThan(0);
        assertThat(a.compareTo(b)).isLessThan(0);
        assertThat(a.compareTo(a)).isEqualTo(0);
        assertThat(a.equals(b)).isFalse();
        assertThat(a.equals(a)).isTrue();
    }
}
