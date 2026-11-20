package ai.timefold.solver.core.impl.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TriangularNumbersTest {

    @Test
    void overflow() {
        assertThatThrownBy(() -> TriangularNumbers.nthTriangle(1L << 32))
                .isInstanceOf(ArithmeticException.class)
                .hasMessage("long overflow");
    }

    static Stream<Arguments> nthProvider() {
        return Stream.of(
                arguments(0, 0),
                arguments(1, 1),
                arguments(2, 3),
                arguments(3, 6),
                arguments(4, 10),
                arguments(5, 15),
                arguments(23169, 268_412_865),
                arguments(46340, 1_073_720_970),
                arguments(46341, 1_073_767_311L),
                arguments(100_000, 5_000_050_000L),
                arguments(Integer.MAX_VALUE, 2_305_843_008_139_952_128L));
    }

    @ParameterizedTest
    @MethodSource("nthProvider")
    void nthTriangle(long n, long nthTriangularNumber) {
        assertThat(TriangularNumbers.nthTriangle(n)).isEqualTo(nthTriangularNumber);
    }

    @ParameterizedTest
    @MethodSource("nthProvider")
    void triangularRoot(long n, long nthTriangularNumber) {
        assertThat(TriangularNumbers.triangularRoot(nthTriangularNumber)).isEqualTo((double) n);
    }

    static Stream<Arguments> nonTriangularRoots() {
        return Stream.of(
                arguments(2, 1),
                arguments(4, 2),
                arguments(5, 2),
                arguments(7, 3),
                arguments(8, 3),
                arguments(9, 3),
                arguments(268_435_456, 23169));
    }

    @ParameterizedTest
    @MethodSource
    void nonTriangularRoots(int x, int rootFloor) {
        assertThat(TriangularNumbers.triangularRoot(x)).isStrictlyBetween((double) rootFloor, rootFloor + 1.0);
    }
}
