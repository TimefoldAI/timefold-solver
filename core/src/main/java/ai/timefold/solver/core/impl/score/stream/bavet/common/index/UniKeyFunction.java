package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.impl.util.Quadruple;
import ai.timefold.solver.core.impl.util.Triple;

final class UniKeyFunction<A>
        implements BiFunction<A, Object, Object>, KeyFunction {

    private final int keyId;
    private final int mappingFunctionCount;
    private final UniMappingFunction<A>[] mappingFunctions;
    private final UniMappingFunction<A> mappingFunction0;
    private final UniMappingFunction<A> mappingFunction1;
    private final UniMappingFunction<A> mappingFunction2;
    private final UniMappingFunction<A> mappingFunction3;

    public UniKeyFunction(UniMappingFunction<A> mappingFunction) {
        this(-1, Collections.singletonList(mappingFunction));
    }

    @SuppressWarnings("unchecked")
    public UniKeyFunction(int keyId, List<UniMappingFunction<A>> mappingFunctionList) {
        this.keyId = keyId;
        this.mappingFunctionCount = mappingFunctionList.size();
        this.mappingFunctions = mappingFunctionList.toArray(new UniMappingFunction[0]);
        this.mappingFunction0 = mappingFunctions[0];
        this.mappingFunction1 = mappingFunctionCount > 1 ? mappingFunctions[1] : null;
        this.mappingFunction2 = mappingFunctionCount > 2 ? mappingFunctions[2] : null;
        this.mappingFunction3 = mappingFunctionCount > 3 ? mappingFunctions[3] : null;
    }

    public Object apply(A a) {
        return switch (mappingFunctionCount) {
            case 1 -> apply1(a);
            case 2 -> apply2Fresh(a);
            case 3 -> apply3Fresh(a);
            case 4 -> apply4Fresh(a);
            default -> applyManyFresh(a);
        };
    }

    @Override
    public Object apply(A a, Object oldKey) {
        return oldKey == null ? apply(a) : switch (mappingFunctionCount) {
            case 1 -> apply1(a);
            case 2 -> apply2(a, oldKey);
            case 3 -> apply3(a, oldKey);
            case 4 -> apply4(a, oldKey);
            default -> applyMany(a, oldKey);
        };
    }

    private Object apply1(A a) {
        return mappingFunction0.apply(a);
    }

    private Object apply2Fresh(A a) {
        var subkey1 = mappingFunction0.apply(a);
        var subkey2 = mappingFunction1.apply(a);
        return new Pair<>(subkey1, subkey2);
    }

    private Object apply2(A a, Object oldKey) {
        var subkey1 = mappingFunction0.apply(a);
        var subkey2 = mappingFunction1.apply(a);
        return buildPair(keyId, oldKey, subkey1, subkey2);
    }

    @SuppressWarnings("unchecked")
    static Pair<Object, Object> buildPair(int keyId, Object oldKey, Object subkey1, Object subkey2) {
        return ((Pair<Object, Object>) extractSubkey(keyId, oldKey))
                .newIfDifferent(subkey1, subkey2);
    }

    @SuppressWarnings("unchecked")
    static <Key_> Key_ extractSubkey(int keyId, Object key) {
        if (key instanceof IndexKeys indexKeys) {
            return indexKeys.get(keyId);
        }
        // This is a single key, not an IndexKeys.
        // See IndexKeys.of(Object o) for details.
        return (Key_) key;
    }

    private Object apply3Fresh(A a) {
        var subkey1 = mappingFunction0.apply(a);
        var subkey2 = mappingFunction1.apply(a);
        var subkey3 = mappingFunction2.apply(a);
        return new Triple<>(subkey1, subkey2, subkey3);
    }

    private Object apply3(A a, Object oldKey) {
        var subkey1 = mappingFunction0.apply(a);
        var subkey2 = mappingFunction1.apply(a);
        var subkey3 = mappingFunction2.apply(a);
        return buildTriple(keyId, oldKey, subkey1, subkey2, subkey3);
    }

    @SuppressWarnings("unchecked")
    static Triple<Object, Object, Object> buildTriple(int keyId, Object oldKey, Object subkey1, Object subkey2,
            Object subkey3) {
        return ((Triple<Object, Object, Object>) extractSubkey(keyId, oldKey))
                .newIfDifferent(subkey1, subkey2, subkey3);
    }

    private Object apply4Fresh(A a) {
        var subkey1 = mappingFunction0.apply(a);
        var subkey2 = mappingFunction1.apply(a);
        var subkey3 = mappingFunction2.apply(a);
        var subkey4 = mappingFunction3.apply(a);
        return new Quadruple<>(subkey1, subkey2, subkey3, subkey4);
    }

    private Object apply4(A a, Object oldKey) {
        var subkey1 = mappingFunction0.apply(a);
        var subkey2 = mappingFunction1.apply(a);
        var subkey3 = mappingFunction2.apply(a);
        var subkey4 = mappingFunction3.apply(a);
        return buildQuadruple(keyId, oldKey, subkey1, subkey2, subkey3, subkey4);
    }

    @SuppressWarnings("unchecked")
    static Quadruple<Object, Object, Object, Object> buildQuadruple(int keyId, Object oldKey, Object subkey1, Object subkey2,
            Object subkey3, Object subkey4) {
        return ((Quadruple<Object, Object, Object, Object>) extractSubkey(keyId, oldKey))
                .newIfDifferent(subkey1, subkey2, subkey3, subkey4);
    }

    private Object applyManyFresh(A a) {
        var result = new Object[mappingFunctionCount];
        for (var i = 0; i < mappingFunctionCount; i++) {
            result[i] = mappingFunctions[i].apply(a);
        }
        return new IndexerKey(result);
    }

    private Object applyMany(A a, Object oldKey) {
        var result = new Object[mappingFunctionCount];
        var oldArray = ((IndexerKey) extractSubkey(keyId, oldKey)).properties();
        var subKeysEqual = true;
        for (var i = 0; i < mappingFunctionCount; i++) {
            var subkey = mappingFunctions[i].apply(a);
            subKeysEqual = subKeysEqual && Objects.equals(subkey, oldArray[i]);
            result[i] = subkey;
        }
        return subKeysEqual ? oldKey : new IndexerKey(result);
    }

}
