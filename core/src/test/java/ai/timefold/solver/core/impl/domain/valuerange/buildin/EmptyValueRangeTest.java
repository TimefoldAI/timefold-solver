package ai.timefold.solver.core.impl.domain.valuerange.buildin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Random;

import org.junit.jupiter.api.Test;

class EmptyValueRangeTest {

    @Test
    void getSize() {
        assertThat(EmptyValueRange.instance().getSize()).isZero();
    }

    @Test
    void get() {
        var range = EmptyValueRange.instance();
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> range.get(0L));
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

    @Test
    void sort() {
        var range = EmptyValueRange.instance();
        assertThatCode(() -> range.sort(null)).doesNotThrowAnyException();
    }

}
