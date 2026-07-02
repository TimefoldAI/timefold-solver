package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ScalingNavigableMapTest {

    @Test
    void getOrCreateGetRemove() {
        var map = new ScalingNavigableMap<Integer, String>();
        assertThat(map.isEmpty()).isTrue();
        assertThat(map.get(1)).isNull();

        assertThat(map.getOrCreate(1, () -> "one")).isEqualTo("one");
        assertThat(map.getOrCreate(1, () -> "should not be used")).isEqualTo("one");
        assertThat(map.get(1)).isEqualTo("one");
        assertThat(map.size()).isEqualTo(1);
        assertThat(map.isEmpty()).isFalse();

        map.remove(1);
        assertThat(map.get(1)).isNull();
        assertThat(map.isEmpty()).isTrue();
    }

    @Test
    void removeIsNoOpForMissingKey() {
        var map = new ScalingNavigableMap<Integer, String>();
        map.getOrCreate(1, () -> "one");
        map.getOrCreate(2, () -> "two");

        map.remove(99); // Not present; must not throw.

        assertThat(map.size()).isEqualTo(2);
        assertThat(map.get(1)).isEqualTo("one");
        assertThat(map.get(2)).isEqualTo("two");
    }

    @Test
    void arrayModeIsAlwaysAscendingRegardlessOfReversed() {
        var map = new ScalingNavigableMap<Integer, String>();
        map.getOrCreate(3, () -> "three");
        map.getOrCreate(1, () -> "one");
        map.getOrCreate(2, () -> "two");
        assertThat(map.arrayBased).isTrue();
        assertThat(ascendingKeys(map)).containsExactly(1, 2, 3);
        assertThat(descendingKeys(map)).containsExactly(1, 2, 3);
    }

    @Test
    void iteratorHonorsReversedPastArrayThreshold() {
        var map = new ScalingNavigableMap<Integer, String>();
        for (var key = 0; key <= ScalingNavigableMap.ARRAY_THRESHOLD; key++) { // crosses the threshold on the last put
            var value = "value" + key;
            map.getOrCreate(key, () -> value);
        }
        assertThat(map.arrayBased).isFalse();
        assertThat(ascendingKeys(map)).isSorted();
        assertThat(descendingKeys(map)).isSortedAccordingTo((a, b) -> b - a);
    }

    @Test
    void treeifiesPastArrayThresholdAndStaysTreeified() {
        var map = new ScalingNavigableMap<Integer, String>();
        for (var key = 0; key <= ScalingNavigableMap.ARRAY_THRESHOLD; key++) { // crosses the threshold on the last put
            var value = "value" + key;
            map.getOrCreate(key, () -> value);
        }
        assertThat(map.arrayBased).isFalse();
        assertThat(map.size()).isEqualTo(ScalingNavigableMap.ARRAY_THRESHOLD + 1);
        assertThat(ascendingKeys(map)).hasSize(ScalingNavigableMap.ARRAY_THRESHOLD + 1).isSorted();

        // Remove all but one key, well below the threshold: must not revert to array mode.
        for (var key = 1; key <= ScalingNavigableMap.ARRAY_THRESHOLD; key++) {
            map.remove(key);
        }
        assertThat(map.arrayBased).isFalse();
        assertThat(map.size()).isEqualTo(1);
        assertThat(map.get(0)).isEqualTo("value0");
    }

    private static List<Integer> ascendingKeys(ScalingNavigableMap<Integer, String> map) {
        return keys(map, false);
    }

    private static List<Integer> keys(ScalingNavigableMap<Integer, String> map, boolean reversed) {
        var keys = new ArrayList<Integer>();
        if (map.arrayBased) {
            for (var i = 0; i < map.size(); i++) {
                keys.add(map.keyAt(i));
            }
        } else {
            var entryIterator = map.iterator(reversed);
            while (entryIterator.hasNext()) {
                keys.add(entryIterator.next().getKey());
            }
        }
        return keys;
    }

    private static List<Integer> descendingKeys(ScalingNavigableMap<Integer, String> map) {
        return keys(map, true);
    }

}
