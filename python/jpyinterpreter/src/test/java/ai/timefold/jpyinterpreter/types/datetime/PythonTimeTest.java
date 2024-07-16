package ai.timefold.jpyinterpreter.types.datetime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.time.LocalTime;

import ai.timefold.jpyinterpreter.types.PythonString;

import org.junit.jupiter.api.Test;

public class PythonTimeTest {
    @Test
    public void testIsoFormat() {
        PythonTime pythonTime = new PythonTime(LocalTime.of(1, 30, 45));
        assertThat(pythonTime.isoformat(PythonString.valueOf("auto"))).isEqualTo(PythonString.valueOf("01:30:45"));
    }
}
