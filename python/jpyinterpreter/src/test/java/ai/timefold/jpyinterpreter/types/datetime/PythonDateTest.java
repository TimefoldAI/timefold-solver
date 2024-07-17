package ai.timefold.jpyinterpreter.types.datetime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.Duration;
import java.time.LocalDate;

import ai.timefold.jpyinterpreter.types.PythonString;

import org.junit.jupiter.api.Test;

public class PythonDateTest {
    @Test
    public void testIsoFormat() {
        PythonDate pythonDate = new PythonDate(LocalDate.of(2002, 3, 11));
        assertThat(pythonDate.iso_format()).isEqualTo(PythonString.valueOf("2002-03-11"));
    }

    @Test
    public void testCTime() {
        PythonDate pythonDate = new PythonDate(LocalDate.of(2002, 3, 11));
        assertThat(pythonDate.ctime()).isEqualTo(PythonString.valueOf("Mon Mar 11 00:00:00 2002"));
    }

    @Test
    public void testAddTimeDelta() {
        PythonDate a = new PythonDate(LocalDate.of(2000, 1, 1));
        PythonTimeDelta timeDelta = new PythonTimeDelta(Duration.ofDays(2));
        assertThat(a.add_time_delta(timeDelta)).isEqualTo(new PythonDate(LocalDate.of(2000, 1, 3)));
    }

    @Test
    public void testSubtractTimeDelta() {
        PythonDate a = new PythonDate(LocalDate.of(2000, 1, 1));
        PythonTimeDelta timeDelta = new PythonTimeDelta(Duration.ofDays(2));
        assertThat(a.subtract_time_delta(timeDelta)).isEqualTo(new PythonDate(LocalDate.of(1999, 12, 30)));
    }

    @Test
    public void testSubtractDate() {
        PythonDate a = new PythonDate(LocalDate.of(2000, 1, 1));
        PythonDate b = new PythonDate(LocalDate.of(2000, 1, 3));
        assertThat(b.subtract_date(a)).isEqualTo(new PythonTimeDelta(Duration.ofDays(2L)));
        assertThat(a.subtract_date(b)).isEqualTo(new PythonTimeDelta(Duration.ofDays(-2L)));
    }

    @Test
    public void testCompareDate() {
        PythonDate a = new PythonDate(LocalDate.of(2000, 1, 1));
        PythonDate b = new PythonDate(LocalDate.of(2000, 1, 3));
        assertThat(b.compareTo(a)).isGreaterThan(0);
        assertThat(a.compareTo(b)).isLessThan(0);
        assertThat(a.compareTo(a)).isEqualTo(0);
        assertThat(a.equals(b)).isFalse();
        assertThat(a.equals(a)).isTrue();
    }
}
