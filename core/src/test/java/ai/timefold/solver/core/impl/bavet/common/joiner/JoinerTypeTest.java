package ai.timefold.solver.core.impl.bavet.common.joiner;

import static ai.timefold.solver.core.impl.bavet.common.joiner.JoinerType.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;

class JoinerTypeTest {

    @Test
    void equal() {
        assertThat(EQUAL.matches(1, 1)).isTrue();
        assertThat(EQUAL.matches(1, 2)).isFalse();
        assertThat(EQUAL.matches(1, null)).isFalse();
        assertThat(EQUAL.matches(null, 1)).isFalse();
    }

    @Test
    void lessThan() {
        assertThat(LESS_THAN.matches(1, 1)).isFalse();
        assertThat(LESS_THAN.matches(1, 2)).isTrue();
        assertThat(LESS_THAN.matches(2, 1)).isFalse();
    }

    @Test
    void lessThanOrEquals() {
        assertThat(LESS_THAN_OR_EQUAL.matches(1, 1)).isTrue();
        assertThat(LESS_THAN_OR_EQUAL.matches(1, 2)).isTrue();
        assertThat(LESS_THAN_OR_EQUAL.matches(2, 1)).isFalse();
    }

    @Test
    void greaterThan() {
        assertThat(GREATER_THAN.matches(1, 1)).isFalse();
        assertThat(GREATER_THAN.matches(2, 1)).isTrue();
        assertThat(GREATER_THAN.matches(1, 2)).isFalse();
    }

    @Test
    void greaterThanOrEquals() {
        assertThat(GREATER_THAN_OR_EQUAL.matches(1, 1)).isTrue();
        assertThat(GREATER_THAN_OR_EQUAL.matches(2, 1)).isTrue();
        assertThat(GREATER_THAN_OR_EQUAL.matches(1, 2)).isFalse();
    }

    @Test
    void contain() {
        Collection<Integer> collection = Arrays.asList(1, 3);
        assertThat(CONTAIN.matches(collection, 1)).isTrue();
        assertThat(CONTAIN.matches(collection, 2)).isFalse();
    }

    @Test
    void containedIn() {
        Collection<Integer> collection = Arrays.asList(1, 3);
        assertThat(CONTAINED_IN.matches(1, collection)).isTrue();
        assertThat(CONTAINED_IN.matches(2, collection)).isFalse();
    }

    @Test
    void intersect() {
        Collection<Integer> left = Arrays.asList(1, 2, 3);
        Collection<Integer> right = Arrays.asList(3, 4, 5);
        assertThat(INTERSECT.matches(left, right)).isTrue();
        assertThat(INTERSECT.matches(right, left)).isTrue();
        assertThat(INTERSECT.matches(left, Collections.emptySet())).isFalse();
    }

    @Test
    void disjoint() {
        Collection<Integer> first = Arrays.asList(1, 2, 3);
        Collection<Integer> second = Arrays.asList(3, 4, 5);
        assertThat(DISJOINT.matches(first, second)).isFalse();
        assertThat(DISJOINT.matches(second, first)).isFalse();
        Collection<Integer> third = Arrays.asList(4, 5);
        assertThat(DISJOINT.matches(first, third)).isTrue();
        assertThat(DISJOINT.matches(third, first)).isTrue();
        // empty sets are disjoint
        assertThat(DISJOINT.matches(Collections.emptyList(), Collections.emptySet())).isTrue();
        assertThat(DISJOINT.matches(first, Collections.emptySet())).isTrue();
    }

}
