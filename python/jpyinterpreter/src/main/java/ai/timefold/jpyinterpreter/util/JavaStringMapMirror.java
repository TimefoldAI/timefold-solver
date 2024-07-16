package ai.timefold.jpyinterpreter.util;

import java.util.Collection;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonString;

import org.apache.commons.collections4.OrderedMap;
import org.apache.commons.collections4.OrderedMapIterator;

public class JavaStringMapMirror implements OrderedMap<PythonLikeObject, PythonLikeObject> {
    final Map<String, PythonLikeObject> delegate;

    public JavaStringMapMirror(Map<String, PythonLikeObject> delegate) {
        this.delegate = delegate;
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsKey(Object o) {
        return o instanceof PythonString && delegate.containsKey(((PythonString) o).value);
    }

    @Override
    public boolean containsValue(Object o) {
        return delegate.containsValue(o);
    }

    @Override
    public PythonLikeObject get(Object o) {
        if (o instanceof PythonString) {
            return delegate.get(((PythonString) o).value);
        }
        return null;
    }

    @Override
    public PythonLikeObject put(PythonLikeObject key, PythonLikeObject value) {
        if (key instanceof PythonString) {
            return delegate.put(((PythonString) key).value, value);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public PythonLikeObject remove(Object o) {
        if (o instanceof PythonString) {
            return delegate.remove(((PythonString) o).value);
        }
        return delegate.remove(o);
    }

    @Override
    public void putAll(Map<? extends PythonLikeObject, ? extends PythonLikeObject> map) {
        map.forEach(this::put);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public Set<PythonLikeObject> keySet() {
        return delegate.keySet().stream().map(PythonString::valueOf).collect(Collectors.toSet());
    }

    @Override
    public Collection<PythonLikeObject> values() {
        return delegate.values();
    }

    @Override
    public Set<Entry<PythonLikeObject, PythonLikeObject>> entrySet() {
        return delegate.entrySet().stream().map(entry -> new Entry<PythonLikeObject, PythonLikeObject>() {
            @Override
            public PythonLikeObject getKey() {
                return PythonString.valueOf(entry.getKey());
            }

            @Override
            public PythonLikeObject getValue() {
                return entry.getValue();
            }

            @Override
            public PythonLikeObject setValue(PythonLikeObject o) {
                return entry.setValue(o);
            }
        }).collect(Collectors.toSet());
    }

    @Override
    public OrderedMapIterator<PythonLikeObject, PythonLikeObject> mapIterator() {
        throw new UnsupportedOperationException("mapIterator is not supported");
    }

    @Override
    public PythonLikeObject firstKey() {
        if (delegate.isEmpty()) {
            throw new NoSuchElementException("Map is empty");
        }
        return PythonString.valueOf(delegate.keySet().iterator().next());
    }

    @Override
    public PythonLikeObject lastKey() {
        if (delegate.isEmpty()) {
            throw new NoSuchElementException("Map is empty");
        }

        String lastKey = null;
        for (String key : delegate.keySet()) {
            lastKey = key;
        }
        return PythonString.valueOf(lastKey);
    }

    @Override
    public PythonLikeObject nextKey(PythonLikeObject object) {
        boolean returnNextKey = false;
        for (String key : delegate.keySet()) {
            if (key.equals(object.toString())) {
                returnNextKey = true;
            }
            if (returnNextKey) {
                return PythonString.valueOf(key);
            }
        }
        return null;
    }

    @Override
    public PythonLikeObject previousKey(PythonLikeObject object) {
        String previousKey = null;
        for (String key : delegate.keySet()) {
            if (key.equals(object.toString())) {
                return PythonString.valueOf(previousKey);
            }
            previousKey = key;
        }
        return null;
    }
}
