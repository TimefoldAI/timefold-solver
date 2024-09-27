package ai.timefold.jpyinterpreter.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TracebackUtilsTest {
    @Test
    void getTraceback() {
        try {
            throw new RuntimeException("A runtime error has occurred.");
        } catch (RuntimeException e) {
            assertThat(TracebackUtils.getTraceback(e))
                    .contains("A runtime error has occurred.")
                    .contains(TracebackUtilsTest.class.getCanonicalName());
        }
    }
}
