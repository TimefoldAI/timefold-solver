package ai.timefold.solver.core.impl.domain.valuerange.buildin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Random;

import org.junit.jupiter.api.Test;

class EmptyValueRangeTest {

    @Test
    void getSize() {
        assertThat(EmptyValueRange.instance().getSize()).isEqualTo(0L);
    }

    @Test
    void get() {
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> EmptyValueRange.instance().get(0L));
    }

    @Test
    void contains() {
        assertThat(EmptyValueRange.instance().contains(5)).isFalse();
        assertThat(EmptyValueRange.instance().contains(null)).isFalse();
    }

    @Test
    void createOriginalIterator() {
        assertThat(EmptyValueRange.instance().createOriginalIterator())
                .toIterable()
                .isEmpty();
    }

    @Test
    void createRandomIterator() {
        Random workingRandom = new Random(0);
        assertThat(EmptyValueRange.instance().createRandomIterator(workingRandom))
                .toIterable()
                .isEmpty();
    }

}
