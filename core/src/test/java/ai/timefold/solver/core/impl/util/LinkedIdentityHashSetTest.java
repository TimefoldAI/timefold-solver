package ai.timefold.solver.core.impl.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;

class LinkedIdentityHashSetTest {

    @Test
    void size() {
        var identityHashSet = new LinkedIdentityHashSet<Integer>();
        assertThat(identityHashSet).isEmpty();
        identityHashSet.add(1);
        identityHashSet.add(2);
        assertThat(identityHashSet).hasSize(2);
    }

    @Test
    void contains() {
        var identityHashSet = new LinkedIdentityHashSet<>();
        var o1 = new Object();
        var o2 = new Object();
        assertThat(identityHashSet.contains(o1)).isFalse();
        assertThat(identityHashSet.contains(o2)).isFalse();
        assertThat(identityHashSet).doesNotContain(o2);
        identityHashSet.add(o1);
        assertThat(identityHashSet.contains(o1)).isTrue();
        assertThat(identityHashSet.contains(o2)).isFalse();
    }

    @Test
    void iterator() {
        var identityHashSet = new LinkedIdentityHashSet<>();
        var o1 = new Object();
        var o2 = new Object();
        var o3 = new Object();
        identityHashSet.add(o1);
        identityHashSet.add(o2);
        identityHashSet.add(o3);
        var iterator = identityHashSet.iterator();
        assertThat(iterator.next()).isSameAs(o1);
        assertThat(iterator.next()).isSameAs(o2);
        assertThat(iterator.next()).isSameAs(o3);
    }

    @Test
    void toArray() {
        var identityHashSet = new LinkedIdentityHashSet<>();
        var o1 = new Object();
        var o2 = new Object();
        var o3 = new Object();
        identityHashSet.add(o1);
        identityHashSet.add(o2);
        identityHashSet.add(o3);
        var array = identityHashSet.toArray();
        assertThat(array).containsExactly(o1, o2, o3);

        var customArray = new Object[3];
        identityHashSet.toArray(customArray);
        assertThat(customArray).containsExactly(o1, o2, o3);

        var smallerArray = new Object[2];
        assertThat(identityHashSet.toArray(smallerArray)).containsExactly(o1, o2, o3);
        assertThat(smallerArray).doesNotContain(o1, o2, o3);

        var largerArray = new Object[4];
        identityHashSet.toArray(largerArray);
        assertThat(largerArray).containsExactly(o1, o2, o3, null);
    }

    @Test
    void addAndRemove() {
        var identityHashSet = new LinkedIdentityHashSet<>();
        var o1 = new Object();
        var o2 = new Object();
        assertThat(identityHashSet.add(o1)).isTrue();
        assertThat(identityHashSet.add(o2)).isTrue();
        assertThat(identityHashSet.add(o2)).isFalse();
        assertThat(identityHashSet.toArray()).containsExactly(o1, o2);
        assertThat(identityHashSet.remove(o1)).isTrue();
        assertThat(identityHashSet.toArray()).containsExactly(o2);
    }

    @Test
    void containsAll() {
        var identityHashSet = new LinkedIdentityHashSet<>();
        var o1 = new Object();
        var o2 = new Object();
        var o3 = new Object();
        identityHashSet.add(o1);
        identityHashSet.add(o2);
        assertThat(identityHashSet.containsAll(List.of(o1, o2))).isTrue();
        assertThat(identityHashSet.containsAll(List.of(o1, o3))).isFalse();
    }

    @Test
    void addAll() {
        var identityHashSet = new LinkedIdentityHashSet<>();
        var o1 = new Object();
        var o2 = new Object();
        var o3 = new Object();
        identityHashSet.add(o1);
        assertThat(identityHashSet.addAll(List.of(o1, o2, o3))).isTrue();
        assertThat(identityHashSet.addAll(List.of(o1, o2, o3))).isFalse();
        assertThat(identityHashSet).containsExactly(o1, o2, o3);
    }

    @Test
    void retainAll() {
        var identityHashSet = new LinkedIdentityHashSet<>();
        var o1 = new Object();
        var o2 = new Object();
        var o3 = new Object();
        identityHashSet.addAll(List.of(o1, o2, o3));
        AssertionsForClassTypes.assertThatThrownBy(() -> identityHashSet.retainAll(List.of(o1, o2)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void removeAll() {
        var identityHashSet = new LinkedIdentityHashSet<>();
        var o1 = new Object();
        var o2 = new Object();
        var o3 = new Object();
        identityHashSet.addAll(List.of(o1, o2, o3));
        assertThat(identityHashSet.removeAll(List.of(o2, o1))).isTrue();
        assertThat(identityHashSet.removeAll(List.of(o2, o1))).isFalse();
        assertThat(identityHashSet).containsExactly(o3);
    }

    @Test
    void clear() {
        var identityHashSet = new LinkedIdentityHashSet<>();
        var o1 = new Object();
        var o2 = new Object();
        var o3 = new Object();
        identityHashSet.addAll(List.of(o1, o2, o3));
        identityHashSet.clear();
        assertThat(identityHashSet).isEmpty();
    }
}
