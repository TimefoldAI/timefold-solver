package ai.timefold.solver.core.impl.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ImmutableIdentityPerfectHashMapTest {

    @Test
    void emptyMap() {
        var map = new ImmutableIdentityPerfectHashMap<>(Map.<String, Integer> of());
        assertThat(map).hasSize(0);
        assertThat(map.isEmpty()).isTrue();
        assertThat(map.get("anything")).isNull();
        assertThat(map.containsKey("anything")).isFalse();
        assertThat(map.entrySet()).isEmpty();
    }

    @Test
    void singleEntryMap() {
        var map = new ImmutableIdentityPerfectHashMap<>(Map.of("a", 1));
        assertThat(map).hasSize(1);
        assertThat(map.get("a")).isEqualTo(1);
        assertThat(map.get("b")).isNull();
        assertThat(map.containsKey("a")).isTrue();
        assertThat(map.containsKey("b")).isFalse();
    }

    @Test
    void multiEntryLookup() {
        var source = new LinkedHashMap<String, Integer>();
        source.put("alpha", 1);
        source.put("beta", 2);
        source.put("gamma", 3);
        source.put("delta", 4);
        source.put("epsilon", 5);
        var map = new ImmutableIdentityPerfectHashMap<>(source);
        assertThat(map).hasSize(5);
        assertThat(map.get("alpha")).isEqualTo(1);
        assertThat(map.get("beta")).isEqualTo(2);
        assertThat(map.get("gamma")).isEqualTo(3);
        assertThat(map.get("delta")).isEqualTo(4);
        assertThat(map.get("epsilon")).isEqualTo(5);
    }

    @Test
    void getAbsentKey() {
        var map = new ImmutableIdentityPerfectHashMap<>(Map.of("x", 10, "y", 20, "z", 30));
        assertThat(map.get("missing")).isNull();
    }

    @Test
    void containsKey() {
        var map = new ImmutableIdentityPerfectHashMap<>(Map.of("present", 42));
        assertThat(map.containsKey("present")).isTrue();
        assertThat(map.containsKey("absent")).isFalse();
    }

    @Test
    void entrySetIteration() {
        var map = new ImmutableIdentityPerfectHashMap<>(Map.of("a", 1, "b", 2, "c", 3));
        assertThat(map.entrySet()).containsExactlyInAnyOrder(
                Map.entry("a", 1),
                Map.entry("b", 2),
                Map.entry("c", 3));
    }

    @Test
    void entrySetSize() {
        var map = new ImmutableIdentityPerfectHashMap<>(Map.of("x", 1, "y", 2));
        assertThat(map.entrySet()).hasSize(map.size());
    }

    @Test
    void nullValueAllowed() {
        var source = new HashMap<String, Integer>();
        source.put("key", null);
        var map = new ImmutableIdentityPerfectHashMap<>(source);
        assertThat(map.containsKey("key")).isTrue();
        assertThat(map.get("key")).isNull();
    }

    @Test
    void nullKeySupported() {
        var source = new HashMap<String, Integer>();
        source.put(null, 99);
        source.put("a", 1);
        var map = new ImmutableIdentityPerfectHashMap<>(source);
        assertThat(map).hasSize(2);
        assertThat(map.get(null)).isEqualTo(99);
        assertThat(map.containsKey(null)).isTrue();
        assertThat(map.containsKey("absent")).isFalse();
        assertThat(map.entrySet()).containsExactlyInAnyOrder(
                new AbstractMap.SimpleImmutableEntry<>(null, 99),
                Map.entry("a", 1));
    }

    @Test
    void nullKeyNullValueSupported() {
        var source = new HashMap<String, Integer>();
        source.put(null, null);
        var map = new ImmutableIdentityPerfectHashMap<>(source);
        assertThat(map.containsKey(null)).isTrue();
        assertThat(map.get(null)).isNull();
    }

    @Test
    void identitySemanticsGetMisses() {
        // Two String objects that are .equals() but different references must NOT resolve to the same key.
        var key = new String("hello");
        var map = new ImmutableIdentityPerfectHashMap<>(Map.of(key, 42));
        assertThat(map.get(key)).isEqualTo(42); // same reference → hit
        assertThat(map.get(new String("hello"))).isNull(); // different reference → miss
        assertThat(map.containsKey(key)).isTrue();
        assertThat(map.containsKey(new String("hello"))).isFalse();
    }
}
