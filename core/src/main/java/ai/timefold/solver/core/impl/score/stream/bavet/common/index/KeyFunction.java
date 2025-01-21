package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.impl.util.Quadruple;
import ai.timefold.solver.core.impl.util.Triple;

interface KeyFunction {

    @SuppressWarnings("unchecked")
    static Object buildPair(int keyId, Object oldKey, Object subkey1, Object subkey2) {
        if (oldKey == null) {
            return new Pair<>(subkey1, subkey2);
        }
        return ((Pair<Object, Object>) extractSubkey(keyId, oldKey))
                .newIfDifferent(subkey1, subkey2);
    }

    @SuppressWarnings("unchecked")
    static Object buildTriple(int keyId, Object oldKey, Object subkey1, Object subkey2, Object subkey3) {
        if (oldKey == null) {
            return new Triple<>(subkey1, subkey2, subkey3);
        }
        return ((Triple<Object, Object, Object>) extractSubkey(keyId, oldKey))
                .newIfDifferent(subkey1, subkey2, subkey3);
    }

    @SuppressWarnings("unchecked")
    static Object buildQuadruple(int keyId, Object oldKey, Object subkey1, Object subkey2,
            Object subkey3, Object subkey4) {
        if (oldKey == null) {
            return new Quadruple<>(subkey1, subkey2, subkey3, subkey4);
        }
        return ((Quadruple<Object, Object, Object, Object>) extractSubkey(keyId, oldKey))
                .newIfDifferent(subkey1, subkey2, subkey3, subkey4);
    }

    static Object extractSubkey(int keyId, Object key) {
        if (key instanceof IndexKeys indexKeys) {
            return indexKeys.get(keyId);
        }
        // This is a single key, not an IndexKeys.
        // See IndexKeys.of(Object o) for details.
        return key;
    }

}
