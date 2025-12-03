package ai.timefold.solver.core.impl.domain.common.accessor;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.Function;

public final class ReflectionMethodExtendedMemberAccessor extends ReflectionMethodMemberAccessor {

    private final Type getterMethodParameterType;

    public ReflectionMethodExtendedMemberAccessor(Method readMethod) {
        this(readMethod, true);
    }

    public ReflectionMethodExtendedMemberAccessor(Method readMethod, boolean returnTypeRequired) {
        super(readMethod, returnTypeRequired, true);
        this.getterMethodParameterType = readMethod.getGenericParameterTypes()[0];
    }

    @Override
    public Object executeGetter(Object bean) {
        throw new UnsupportedOperationException(
                "The method executeGetter(Object) without parameter is not supported. Maybe call executeGetter(Object, Object) from ExtendedMemberAccessor instead.");
    }

    @Override
    public <Fact_, Result_> Function<Fact_, Result_> getGetterFunction() {
        throw new UnsupportedOperationException("The method getGetterFunction() is not supported.");
    }

    @Override
    public Type getGetterMethodParameterType() {
        return getterMethodParameterType;
    }

    @Override
    public Object executeGetter(Object bean, Object value) {
        if (bean == null) {
            throw new IllegalArgumentException("Requested getterMethod (%s) on a null bean.".formatted(getReadMethod()));
        }
        try {
            return getMethodHandle().invoke(bean, value);
        } catch (Throwable e) {
            throw new IllegalStateException("The property (%s) getterMethod (%s) on bean of class (%s) throws an exception."
                    .formatted(getName(), getReadMethod(), bean.getClass()), e);
        }
    }
}
