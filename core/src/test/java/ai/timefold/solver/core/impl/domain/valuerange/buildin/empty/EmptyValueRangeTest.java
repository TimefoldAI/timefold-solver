package ai.timefold.solver.core.impl.domain.valuerange.buildin.empty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Random;

import org.junit.jupiter.api.Test;

class EmptyValueRangeTest {

    @Test
    void getSize() {
        assertThat(EmptyValueRange.INSTANCE.getSize()).isEqualTo(0L);
    }

    @Test
    void get() {
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> EmptyValueRange.INSTANCE.get(0L));
    }

    @Test
    void contains() {
        assertThat(EmptyValueRange.INSTANCE.contains(5)).isFalse();
        assertThat(EmptyValueRange.INSTANCE.contains(null)).isFalse();
    }

    @Test
    void createOriginalIterator() {
        assertThat(EmptyValueRange.INSTANCE.createOriginalIterator())
                .toIterable()
                .isEmpty();
    }

    @Test
    void createRandomIterator() {
        Random workingRandom = new Random(0);
        assertThat(EmptyValueRange.INSTANCE.createRandomIterator(workingRandom))
                .toIterable()
                .isEmpty();
    }

}
