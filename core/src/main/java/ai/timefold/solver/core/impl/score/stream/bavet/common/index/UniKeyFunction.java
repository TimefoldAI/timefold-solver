package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

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

    @Override
    public Object apply(A a, Object oldKey) {
        return switch (mappingFunctionCount) {
            case 1 -> apply1(a);
            case 2 -> apply2(a, oldKey);
            case 3 -> apply3(a, oldKey);
            case 4 -> apply4(a, oldKey);
            default -> oldKey == null ? applyManyFresh(a) : applyMany(a, oldKey);
        };
    }

    private Object apply1(A a) {
        return mappingFunction0.apply(a);
    }

    private Object apply2(A a, Object oldKey) {
        var subkey1 = mappingFunction0.apply(a);
        var subkey2 = mappingFunction1.apply(a);
        return KeyFunction.buildPair(keyId, oldKey, subkey1, subkey2);
    }

    private Object apply3(A a, Object oldKey) {
        var subkey1 = mappingFunction0.apply(a);
        var subkey2 = mappingFunction1.apply(a);
        var subkey3 = mappingFunction2.apply(a);
        return KeyFunction.buildTriple(keyId, oldKey, subkey1, subkey2, subkey3);
    }

    private Object apply4(A a, Object oldKey) {
        var subkey1 = mappingFunction0.apply(a);
        var subkey2 = mappingFunction1.apply(a);
        var subkey3 = mappingFunction2.apply(a);
        var subkey4 = mappingFunction3.apply(a);
        return KeyFunction.buildQuadruple(keyId, oldKey, subkey1, subkey2, subkey3, subkey4);
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
        var oldArray = ((IndexerKey) KeyFunction.extractSubkey(keyId, oldKey)).properties();
        var subKeysEqual = true;
        for (var i = 0; i < mappingFunctionCount; i++) {
            var subkey = mappingFunctions[i].apply(a);
            subKeysEqual = subKeysEqual && Objects.equals(subkey, oldArray[i]);
            result[i] = subkey;
        }
        return subKeysEqual ? oldKey : new IndexerKey(result);
    }

}
