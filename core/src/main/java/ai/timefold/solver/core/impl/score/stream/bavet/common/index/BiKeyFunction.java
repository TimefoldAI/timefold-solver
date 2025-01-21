package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.function.TriFunction;

final class BiKeyFunction<A, B>
        implements TriFunction<A, B, Object, Object>, KeyFunction {

    private final int keyId;
    private final int mappingFunctionCount;
    private final BiMappingFunction<A, B>[] mappingFunctions;
    private final BiMappingFunction<A, B> mappingFunction0;
    private final BiMappingFunction<A, B> mappingFunction1;
    private final BiMappingFunction<A, B> mappingFunction2;
    private final BiMappingFunction<A, B> mappingFunction3;

    public BiKeyFunction(BiMappingFunction<A, B> mappingFunction) {
        this(-1, Collections.singletonList(mappingFunction));
    }

    @SuppressWarnings("unchecked")
    public BiKeyFunction(int keyId, List<BiMappingFunction<A, B>> mappingFunctionList) {
        this.keyId = keyId;
        this.mappingFunctionCount = mappingFunctionList.size();
        this.mappingFunctions = mappingFunctionList.toArray(new BiMappingFunction[0]);
        this.mappingFunction0 = mappingFunctions[0];
        this.mappingFunction1 = mappingFunctionCount > 1 ? mappingFunctions[1] : null;
        this.mappingFunction2 = mappingFunctionCount > 2 ? mappingFunctions[2] : null;
        this.mappingFunction3 = mappingFunctionCount > 3 ? mappingFunctions[3] : null;
    }

    @Override
    public Object apply(A a, B b, Object oldKey) {
        return switch (mappingFunctionCount) {
            case 1 -> apply1(a, b);
            case 2 -> apply2(a, b, oldKey);
            case 3 -> apply3(a, b, oldKey);
            case 4 -> apply4(a, b, oldKey);
            default -> oldKey == null ? applyManyFresh(a, b) : applyMany(a, b, oldKey);
        };
    }

    private Object apply1(A a, B b) {
        return mappingFunction0.apply(a, b);
    }

    private Object apply2(A a, B b, Object oldKey) {
        var subkey1 = mappingFunction0.apply(a, b);
        var subkey2 = mappingFunction1.apply(a, b);
        return KeyFunction.buildPair(keyId, oldKey, subkey1, subkey2);
    }

    private Object apply3(A a, B b, Object oldKey) {
        var subkey1 = mappingFunction0.apply(a, b);
        var subkey2 = mappingFunction1.apply(a, b);
        var subkey3 = mappingFunction2.apply(a, b);
        return KeyFunction.buildTriple(keyId, oldKey, subkey1, subkey2, subkey3);
    }

    private Object apply4(A a, B b, Object oldKey) {
        var subkey1 = mappingFunction0.apply(a, b);
        var subkey2 = mappingFunction1.apply(a, b);
        var subkey3 = mappingFunction2.apply(a, b);
        var subkey4 = mappingFunction3.apply(a, b);
        return KeyFunction.buildQuadruple(keyId, oldKey, subkey1, subkey2, subkey3, subkey4);
    }

    private Object applyManyFresh(A a, B b) {
        var result = new Object[mappingFunctionCount];
        for (var i = 0; i < mappingFunctionCount; i++) {
            result[i] = mappingFunctions[i].apply(a, b);
        }
        return new IndexerKey(result);
    }

    private Object applyMany(A a, B b, Object oldKey) {
        var result = new Object[mappingFunctionCount];
        var oldArray = ((IndexerKey) KeyFunction.extractSubkey(keyId, oldKey)).properties();
        var subKeysEqual = true;
        for (var i = 0; i < mappingFunctionCount; i++) {
            var subkey = mappingFunctions[i].apply(a, b);
            subKeysEqual = subKeysEqual && Objects.equals(subkey, oldArray[i]);
            result[i] = subkey;
        }
        if (subKeysEqual) {
            return oldKey;
        }
        return new IndexerKey(result);
    }

}
