package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.function.PentaFunction;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.core.impl.util.Quadruple;
import ai.timefold.solver.core.impl.util.Triple;

final class QuadKeyFunction<A, B, C, D>
        implements PentaFunction<A, B, C, D, Object, Object>, KeyFunction {

    private final int keyId;
    private final int mappingFunctionCount;
    private final QuadMappingFunction<A, B, C, D>[] mappingFunctions;
    private final QuadMappingFunction<A, B, C, D> mappingFunction0;
    private final QuadMappingFunction<A, B, C, D> mappingFunction1;
    private final QuadMappingFunction<A, B, C, D> mappingFunction2;
    private final QuadMappingFunction<A, B, C, D> mappingFunction3;

    public QuadKeyFunction(QuadMappingFunction<A, B, C, D> mappingFunction) {
        this(-1, Collections.singletonList(mappingFunction));
    }

    @SuppressWarnings("unchecked")
    public QuadKeyFunction(int keyId, List<QuadMappingFunction<A, B, C, D>> mappingFunctionList) {
        this.keyId = keyId;
        this.mappingFunctionCount = mappingFunctionList.size();
        this.mappingFunctions = mappingFunctionList.toArray(new QuadMappingFunction[0]);
        this.mappingFunction0 = mappingFunctions[0];
        this.mappingFunction1 = mappingFunctionCount > 1 ? mappingFunctions[1] : null;
        this.mappingFunction2 = mappingFunctionCount > 2 ? mappingFunctions[2] : null;
        this.mappingFunction3 = mappingFunctionCount > 3 ? mappingFunctions[3] : null;
    }

    @Override
    public Object apply(A a, B b, C c, D d, Object oldKey) {
        return switch (mappingFunctionCount) {
            case 1 -> apply1(a, b, c, d);
            case 2 -> apply2(a, b, c, d, oldKey);
            case 3 -> apply3(a, b, c, d, oldKey);
            case 4 -> apply4(a, b, c, d, oldKey);
            default -> applyMany(a, b, c, d, oldKey);
        };
    }

    private Object apply1(A a, B b, C c, D d) {
        return mappingFunction0.apply(a, b, c, d);
    }

    @SuppressWarnings("unchecked")
    private Object apply2(A a, B b, C c, D d, Object oldKey) {
        var subkey1 = mappingFunction0.apply(a, b, c, d);
        var subkey2 = mappingFunction1.apply(a, b, c, d);
        if (oldKey == null) {
            return new Pair<>(subkey1, subkey2);
        }
        return ((Pair<Object, Object>) UniKeyFunction.extractSubkey(keyId, oldKey))
                .newIfDifferent(subkey1, subkey2);
    }

    @SuppressWarnings("unchecked")
    private Object apply3(A a, B b, C c, D d, Object oldKey) {
        var subkey1 = mappingFunction0.apply(a, b, c, d);
        var subkey2 = mappingFunction1.apply(a, b, c, d);
        var subkey3 = mappingFunction2.apply(a, b, c, d);
        if (oldKey == null) {
            return new Triple<>(subkey1, subkey2, subkey3);
        }
        return ((Triple<Object, Object, Object>) UniKeyFunction.extractSubkey(keyId, oldKey))
                .newIfDifferent(subkey1, subkey2, subkey3);
    }

    @SuppressWarnings("unchecked")
    private Object apply4(A a, B b, C c, D d, Object oldKey) {
        var subkey1 = mappingFunction0.apply(a, b, c, d);
        var subkey2 = mappingFunction1.apply(a, b, c, d);
        var subkey3 = mappingFunction2.apply(a, b, c, d);
        var subkey4 = mappingFunction3.apply(a, b, c, d);
        if (oldKey == null) {
            return new Quadruple<>(subkey1, subkey2, subkey3, subkey4);
        }
        return ((Quadruple<Object, Object, Object, Object>) UniKeyFunction.extractSubkey(keyId, oldKey))
                .newIfDifferent(subkey1, subkey2, subkey3, subkey4);
    }

    private Object applyMany(A a, B b, C c, D d, Object oldKey) {
        var result = new Object[mappingFunctionCount];
        if (oldKey == null) {
            for (var i = 0; i < mappingFunctionCount; i++) {
                result[i] = mappingFunctions[i].apply(a, b, c, d);
            }
        } else {
            var oldArray = ((IndexerKey) UniKeyFunction.extractSubkey(keyId, oldKey)).properties();
            var subKeysEqual = true;
            for (var i = 0; i < mappingFunctionCount; i++) {
                var subkey = mappingFunctions[i].apply(a, b, c, d);
                subKeysEqual = subKeysEqual && Objects.equals(subkey, oldArray[i]);
                result[i] = subkey;
            }
            if (subKeysEqual) {
                return oldKey;
            }
        }
        return new IndexerKey(result);
    }

}
