package ai.timefold.solver.core.impl.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.Random;
import java.util.stream.Stream;

import ai.timefold.solver.core.impl.util.TriangleElementFactory.TriangleElement;
import ai.timefold.solver.core.testutil.TestRandom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TriangleElementFactoryTest {

    static Stream<Arguments> triangleElementValueOf() {
        return Stream.of(
                arguments(1, 1, 1),
                arguments(2, 2, 1),
                arguments(3, 2, 2),
                arguments(4, 3, 1),
                arguments(5, 3, 2),
                arguments(6, 3, 3),
                arguments(7, 4, 1),
                arguments(8, 4, 2),
                arguments(9, 4, 3),
                arguments(10, 4, 4));
    }

    @ParameterizedTest
    @MethodSource
    void triangleElementValueOf(int index, int level, int indexOnLevel) {
        var triangleElement = TriangleElement.valueOf(index);
        assertThat(triangleElement.index()).isEqualTo(index);
        assertThat(triangleElement.level()).isEqualTo(level);
        assertThat(triangleElement.indexOnLevel()).isEqualTo(indexOnLevel);
    }

    @Test
    void nextElement() {
        var listSize = 7;
        var subListCount = listSize * (listSize + 1) / 2;
        assertThat(subListCount).isEqualTo(28);

        // There is 1 subList of size 7 and 2 subLists of size 6.
        var maxSize = 5;
        subListCount -= 3;
        // There are 7 subLists of size 1.
        var minSize = 2;
        subListCount -= 7;
        assertThat(subListCount).isEqualTo(18);

        var testRandom = new TestRandom(0, subListCount - 1);
        var factory = new TriangleElementFactory(minSize, maxSize, testRandom);

        var first = factory.nextElement(listSize);
        testRandom.assertIntBoundJustRequested(subListCount);
        assertThat(first.index()).isEqualTo(4); // Triangle element index.
        assertThat(first.level()).isEqualTo(3); // 3rd level, there are 3 subLists of size 5.
        assertThat(first.indexOnLevel()).isEqualTo(1); // It's the 1st element on level 3.

        var last = factory.nextElement(listSize);
        testRandom.assertIntBoundJustRequested(subListCount);
        assertThat(last.index()).isEqualTo(21); // Triangle element index.
        assertThat(last.level()).isEqualTo(6); // 6th level, there are 6 subLists of size 2.
        assertThat(last.indexOnLevel()).isEqualTo(6); // It's the 6th element on level 6.
    }

    @Test
    void constructor_invalidBounds() {
        assertThatIllegalArgumentException().isThrownBy(() -> new TriangleElementFactory(5, 4, new Random()))
                .withMessageContaining("less than or equal to");
        assertThatIllegalArgumentException().isThrownBy(() -> new TriangleElementFactory(0, 4, new Random()))
                .withMessageContaining("greater than 0");
    }

    @Test
    void nextElement_invalidListSize() {
        var minSize = 5;
        var factory = new TriangleElementFactory(minSize, minSize + 1, new Random());
        assertThatIllegalArgumentException().isThrownBy(() -> factory.nextElement(minSize - 1));
    }

    @Test
    void nextElement_listSizeFarBelowMinimum() {
        // A listSize this far below minimumSubListSize makes nthTriangle()'s argument negative enough
        // (listSize - minimumSubListSize + 1 == -2) that it returns a small positive, non-throwing result
        // (nthTriangle(-2) == 1) instead of the 0 that the listSize == minimumSubListSize - 1 case above
        // happens to produce. Without an explicit guard, nextElement() would silently return a bogus
        // TriangleElement instead of enforcing its documented contract.
        var minSize = 5;
        var factory = new TriangleElementFactory(minSize, minSize + 1, new Random());
        assertThatIllegalArgumentException().isThrownBy(() -> factory.nextElement(minSize - 3))
                .withMessageContaining("must be at least the minimumSubListSize");
    }
}
