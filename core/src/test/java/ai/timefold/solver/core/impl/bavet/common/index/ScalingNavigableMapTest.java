package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

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
    void iteratorHonorsReversedInTreeMode() {
        var map = treeifiedMap();
        assertThat(map.arrayBased).isFalse();
        assertThat(ascendingKeys(map)).isSorted();
        assertThat(descendingKeys(map)).isSortedAccordingTo((a, b) -> b - a);
    }

    @Test
    void treeifiesOnChurnAtThresholdAndStaysTreeified() {
        var map = treeifiedMap();
        assertThat(map.arrayBased).isFalse();
        // treeifiedMap() fills 0..ARRAY_THRESHOLD (65 keys), then churns key 0 back in.
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

    @Test
    void growthWithoutChurnStaysArrayBased() {
        var map = new ScalingNavigableMap<Integer, String>();
        for (var key = 0; key < ScalingNavigableMap.MAXIMUM_ARRAY_SIZE; key++) {
            var value = "value" + key;
            map.getOrCreate(key, () -> value);
        }
        assertThat(map.arrayBased).isTrue();
        assertThat(map.size()).isEqualTo(ScalingNavigableMap.MAXIMUM_ARRAY_SIZE);
        assertThat(ascendingKeys(map)).isSorted();
        assertThat(map.get(ScalingNavigableMap.MAXIMUM_ARRAY_SIZE / 2))
                .isEqualTo("value" + ScalingNavigableMap.MAXIMUM_ARRAY_SIZE / 2);
    }

    @Test
    void churnBelowThresholdStaysArrayBased() {
        var map = new ScalingNavigableMap<Integer, String>();
        var upperBound = ScalingNavigableMap.ARRAY_THRESHOLD / 2;
        for (var key = 0; key < upperBound; key++) {
            var value = "value" + key;
            map.getOrCreate(key, () -> value);
        }
        var churnKey = 0;
        for (var i = 0; i < ScalingNavigableMap.CHURN_TOLERANCE * 4; i++) {
            map.remove(churnKey);
            map.getOrCreate(churnKey, () -> "churned");
            assertThat(map.arrayBased).isTrue();
        }
        assertThat(map.arrayBased).isTrue();
        assertThat(map.size()).isEqualTo(upperBound);
    }

    @Test
    void churnAtThresholdTreeifiesOnlyAfterTolerance() {
        var map = new ScalingNavigableMap<Integer, String>();
        var keyCount = ScalingNavigableMap.ARRAY_THRESHOLD + 1;
        for (var key = 0; key < keyCount; key++) {
            var value = "value" + key;
            map.getOrCreate(key, () -> value);
        }
        assertThat(map.arrayBased).isTrue();

        var churnKey = 0;
        for (var i = 0; i < ScalingNavigableMap.CHURN_TOLERANCE - 1; i++) {
            map.remove(churnKey);
            map.getOrCreate(churnKey, () -> "rebuilt" + churnKey);
        }
        assertThat(map.arrayBased).isTrue();

        map.remove(churnKey);
        map.getOrCreate(churnKey, () -> "rebuilt" + churnKey);
        assertThat(map.arrayBased).isFalse();

        assertThat(map.size()).isEqualTo(keyCount);
        assertThat(map.get(churnKey)).isEqualTo("rebuilt" + churnKey);
        var expectedKeys = new ArrayList<Integer>();
        for (var key = 0; key < keyCount; key++) {
            expectedKeys.add(key);
        }
        assertThat(ascendingKeys(map)).containsExactlyElementsOf(expectedKeys);
    }

    @Test
    void safetyCeilingTreeifiesWithoutChurn() {
        var map = new ScalingNavigableMap<Integer, String>();
        var keyCount = ScalingNavigableMap.MAXIMUM_ARRAY_SIZE + 1;
        for (var key = 0; key < keyCount; key++) {
            var value = "value" + key;
            map.getOrCreate(key, () -> value);
        }
        assertThat(map.arrayBased).isFalse();
        assertThat(map.size()).isEqualTo(keyCount);
        assertThat(ascendingKeys(map)).isSorted();
        assertThat(descendingKeys(map)).isSortedAccordingTo((a, b) -> b - a);
    }

    @Test
    void randomChurnMatchesTreeMapModel() {
        var map = new ScalingNavigableMap<Integer, String>();
        var oracle = new TreeMap<Integer, String>();
        var random = new Random(42L);
        var keySpace = 2 * ScalingNavigableMap.MAXIMUM_ARRAY_SIZE;
        var operationCount = 100_000;
        for (var i = 0; i < operationCount; i++) {
            var key = random.nextInt(keySpace);
            var value = "value" + key;
            var operation = random.nextInt(10);
            if (operation < 6) {
                map.getOrCreate(key, () -> value);
                oracle.putIfAbsent(key, value);
            } else if (operation < 9) {
                map.remove(key);
                oracle.remove(key);
            } else {
                assertThat(map.get(key)).isEqualTo(oracle.get(key));
            }
            if (i % 1000 == 0) {
                assertThat(map.size()).isEqualTo(oracle.size());
                assertThat(ascendingKeys(map)).containsExactlyElementsOf(oracle.keySet());
            }
        }
        assertThat(map.size()).isEqualTo(oracle.size());
        assertThat(ascendingKeys(map)).containsExactlyElementsOf(oracle.keySet());
    }

    /**
     * Builds a map that has treeified via churn (not via the size ceiling):
     * fills 0..ARRAY_THRESHOLD (65 keys, still array-mode), then removes and immediately
     * re-adds key 0, CHURN_TOLERANCE times. Size drops to ARRAY_THRESHOLD (still >= ARRAY_THRESHOLD)
     * on the first removal, so every cycle counts as churn at scale; after CHURN_TOLERANCE cycles
     * the map has treeified.
     */
    private static ScalingNavigableMap<Integer, String> treeifiedMap() {
        var map = new ScalingNavigableMap<Integer, String>();
        for (var key = 0; key <= ScalingNavigableMap.ARRAY_THRESHOLD; key++) {
            var value = "value" + key;
            map.getOrCreate(key, () -> value);
        }
        for (var i = 0; i < ScalingNavigableMap.CHURN_TOLERANCE; i++) {
            map.remove(0);
            map.getOrCreate(0, () -> "value0");
        }
        assertThat(map.arrayBased).isFalse();
        return map;
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
