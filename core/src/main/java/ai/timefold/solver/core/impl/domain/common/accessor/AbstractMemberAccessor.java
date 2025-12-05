package ai.timefold.solver.core.impl.domain.common.accessor;

import java.lang.reflect.Type;
import java.util.function.Function;

public abstract class AbstractMemberAccessor implements MemberAccessor {

    // We cache this so that the same reference is always returned; useful for CS node sharing.
    private final Function getterFunction = this::executeGetter;

    @Override
    public <Fact_, Result_> Function<Fact_, Result_> getGetterFunction() {
        return getterFunction;
    }

    @Override
    public Type getGetterMethodParameterType() {
        // Return null because we do not support methods that have parameters by default
        return null;
    }

    @Override
    public Object executeGetter(Object bean, Object value) {
        // We do not support methods that have parameters by default
        throw new UnsupportedOperationException(
                "The method executeGetter(Object, Object) is not supported. Maybe call executeGetter(Object) instead.");
    }
}
