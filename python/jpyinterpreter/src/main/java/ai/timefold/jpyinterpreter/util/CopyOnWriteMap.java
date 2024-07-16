package ai.timefold.jpyinterpreter.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class CopyOnWriteMap<Key_, Value_> implements Map<Key_, Value_> {
    private final Map<Key_, Value_> immutableMap;
    private Optional<Map<Key_, Value_>> modifiedMap;

    public CopyOnWriteMap(Map<Key_, Value_> immutableMap) {
        this.immutableMap = immutableMap;
        this.modifiedMap = Optional.empty();
    }

    // Read Operations
    @Override
    public int size() {
        return modifiedMap.map(Map::size).orElseGet(immutableMap::size);
    }

    @Override
    public boolean isEmpty() {
        return modifiedMap.map(Map::isEmpty).orElseGet(immutableMap::isEmpty);
    }

    @Override
    public boolean containsKey(Object o) {
        return modifiedMap.map(map -> map.containsKey(o)).orElseGet(() -> immutableMap.containsKey(o));
    }

    @Override
    public boolean containsValue(Object o) {
        return modifiedMap.map(map -> map.containsValue(o)).orElseGet(() -> immutableMap.containsValue(o));
    }

    @Override
    public Value_ get(Object o) {
        return modifiedMap.map(map -> map.get(o)).orElseGet(() -> immutableMap.get(o));
    }

    @Override
    public Set<Key_> keySet() {
        return modifiedMap.map(Map::keySet).orElseGet(immutableMap::keySet);
    }

    @Override
    public Collection<Value_> values() {
        return modifiedMap.map(Map::values).orElseGet(immutableMap::values);
    }

    @Override
    public Set<Entry<Key_, Value_>> entrySet() {
        return modifiedMap.map(Map::entrySet).orElseGet(immutableMap::entrySet);
    }

    // Write Operations
    @Override
    public Value_ put(Key_ key, Value_ value) {
        if (modifiedMap.isEmpty()) {
            modifiedMap = Optional.of(new HashMap<>(immutableMap));
        }
        return modifiedMap.get().put(key, value);
    }

    @Override
    public Value_ remove(Object o) {
        if (modifiedMap.isEmpty()) {
            modifiedMap = Optional.of(new HashMap<>(immutableMap));
        }
        return modifiedMap.get().remove(o);
    }

    @Override
    public void putAll(Map<? extends Key_, ? extends Value_> map) {
        if (modifiedMap.isEmpty()) {
            modifiedMap = Optional.of(new HashMap<>(immutableMap));
        }
        modifiedMap.get().putAll(map);
    }

    @Override
    public void clear() {
        if (modifiedMap.isEmpty()) {
            modifiedMap = Optional.of(new HashMap<>(immutableMap));
        }
        modifiedMap.get().clear();
    }
}
