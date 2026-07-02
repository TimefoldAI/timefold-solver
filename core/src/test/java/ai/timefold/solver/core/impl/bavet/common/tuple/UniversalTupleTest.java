package ai.timefold.solver.core.impl.bavet.common.tuple;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UniversalTupleTest {

    @Test
    void storeSizeZero_constructsWithoutThrowing() {
        assertThat(UniTuple.<Object> of(0)).isNotNull();
    }

    @Test
    void storeSizeOne_index0GoesThroughStore0() {
        Tuple tuple = UniTuple.of(1);

        tuple.setStore(0, "a");
        assertThat(tuple.<String> getStore(0)).isEqualTo("a");

        assertThat(tuple.<String> removeStore(0)).isEqualTo("a");
        assertThat(tuple.<String> getStore(0)).isNull();
    }

    @Test
    void storeSizeTwo_overflowBoundary() {
        Tuple tuple = UniTuple.of(2);

        tuple.setStore(0, "a");
        tuple.setStore(1, "b");

        assertThat(tuple.<String> getStore(0)).isEqualTo("a");
        assertThat(tuple.<String> getStore(1)).isEqualTo("b");
    }

    @Test
    void storeSizeMany_index0AndOverflowAreIndependent() {
        Tuple tuple = UniTuple.of(3);

        tuple.setStore(0, "a");
        tuple.setStore(1, "b");
        tuple.setStore(2, "c");

        assertThat(tuple.<String> getStore(0)).isEqualTo("a");
        assertThat(tuple.<String> getStore(1)).isEqualTo("b");
        assertThat(tuple.<String> getStore(2)).isEqualTo("c");

        assertThat(tuple.<String> removeStore(1)).isEqualTo("b");
        assertThat(tuple.<String> getStore(1)).isNull();
        assertThat(tuple.<String> getStore(0)).isEqualTo("a");
        assertThat(tuple.<String> getStore(2)).isEqualTo("c");
    }

}
