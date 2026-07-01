package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class ScalingNavigableMapTest {

    @Test
    void getOrCreateGetRemove() {
        var map = new ScalingNavigableMap<Integer, String>(false);
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
    void iteratesForwardOrReversed() {
        var ascending = new ScalingNavigableMap<Integer, String>(false);
        ascending.getOrCreate(3, () -> "three");
        ascending.getOrCreate(1, () -> "one");
        ascending.getOrCreate(2, () -> "two");
        assertThat(keysInCursorOrder(ascending)).containsExactly(1, 2, 3);

        var descending = new ScalingNavigableMap<Integer, String>(true);
        descending.getOrCreate(3, () -> "three");
        descending.getOrCreate(1, () -> "one");
        descending.getOrCreate(2, () -> "two");
        assertThat(keysInCursorOrder(descending)).containsExactly(3, 2, 1);
    }

    @Test
    void iteratesForwardOrReversedPastArrayThreshold() {
        var ascending = new ScalingNavigableMap<Integer, String>(false);
        var descending = new ScalingNavigableMap<Integer, String>(true);
        for (var key = 0; key <= ScalingNavigableMap.ARRAY_THRESHOLD; key++) { // crosses the threshold on the last put
            var value = "value" + key;
            ascending.getOrCreate(key, () -> value);
            descending.getOrCreate(key, () -> value);
        }
        assertThat(ascending.belowThreshold).isFalse();
        assertThat(descending.belowThreshold).isFalse();
        assertThat(keysInCursorOrder(ascending)).isSorted();
        assertThat(keysInCursorOrder(descending)).isSortedAccordingTo((a, b) -> b - a);
    }

    @Test
    void treeifiesPastArrayThresholdAndStaysTreeified() {
        var map = new ScalingNavigableMap<Integer, String>(false);
        for (var key = 0; key <= ScalingNavigableMap.ARRAY_THRESHOLD; key++) { // crosses the threshold on the last put
            var value = "value" + key;
            map.getOrCreate(key, () -> value);
        }
        assertThat(map.belowThreshold).isFalse();
        assertThat(map.size()).isEqualTo(ScalingNavigableMap.ARRAY_THRESHOLD + 1);
        assertThat(keysInCursorOrder(map)).hasSize(ScalingNavigableMap.ARRAY_THRESHOLD + 1).isSorted();

        // Remove all but one key, well below the threshold: must not revert to array mode.
        for (var key = 1; key <= ScalingNavigableMap.ARRAY_THRESHOLD; key++) {
            map.remove(key);
        }
        assertThat(map.belowThreshold).isFalse();
        assertThat(map.size()).isEqualTo(1);
        assertThat(map.get(0)).isEqualTo("value0");
    }

    private static List<Integer> keysInCursorOrder(ScalingNavigableMap<Integer, String> map) {
        var keys = new ArrayList<Integer>();
        var cursor = map.cursorFromStart();
        while (cursor.advance()) {
            keys.add(cursor.key());
        }
        return keys;
    }

}
