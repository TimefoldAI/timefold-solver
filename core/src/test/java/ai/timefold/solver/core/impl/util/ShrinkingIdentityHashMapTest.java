package ai.timefold.solver.core.impl.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.junit.jupiter.api.Test;

class ShrinkingIdentityHashMapTest {

    @Test
    void clearSmallMapInPlace() {
        var map = new ShrinkingIdentityHashMap<Integer, Integer>();
        map.put(1, 1);
        var delegate = map.delegate();
        map.clear();
        assertThat(map.delegate()).isSameAs(delegate);
        assertThat(map.isEmpty()).isTrue();
    }

    @Test
    void clearLargeMapInPlaceWhenNearMax() {
        var map = new ShrinkingIdentityHashMap<Integer, Integer>();
        putAll(map, 100);
        var delegate = map.delegate();
        map.clear();
        assertThat(map.delegate()).isSameAs(delegate);
        assertThat(map.isEmpty()).isTrue();
    }

    @Test
    void replaceLargeMapWhenFarBelowMax() {
        var map = new ShrinkingIdentityHashMap<Integer, Integer>();
        putAll(map, 1000);
        var delegate = map.delegate();
        map.clear();
        assertThat(map.delegate()).isSameAs(delegate);
        Integer key = 1;
        for (var cycle = 0; cycle < 10; cycle++) {
            map.put(key, key);
            map.clear();
            if (map.delegate() != delegate) {
                assertThat(map.isEmpty()).isTrue();
                map.put(key, key);
                assertThat(map.get(key)).isSameAs(key);
                return;
            }
        }
        fail("The map was not replaced within 10 clears.");
    }

    @Test
    void keepEmptyMapEvenWhenFarBelowMax() {
        var map = new ShrinkingIdentityHashMap<Integer, Integer>();
        var delegate = map.delegate();
        map.clear();
        assertThat(map.delegate()).isSameAs(delegate);
    }

    @Test
    void peakSurvivesRemove() {
        var map = new ShrinkingIdentityHashMap<Object, Object>();
        var keys = new Object[1000];
        for (var i = 0; i < keys.length; i++) {
            keys[i] = new Object();
            map.put(keys[i], keys[i]);
        }
        for (var i = 1; i < keys.length; i++) {
            map.remove(keys[i]);
        }
        var delegate = map.delegate();
        map.clear();
        assertThat(map.delegate()).isSameAs(delegate);
    }

    private static void putAll(ShrinkingIdentityHashMap<Integer, Integer> map, int count) {
        for (var i = 0; i < count; i++) {
            Integer key = i;
            map.put(key, key);
        }
    }

}
