package ai.timefold.solver.core.impl.domain.common.accessor;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.Function;

public final class ReflectionExtendedBeanPropertyMemberAccessor extends ReflectionBeanPropertyMemberAccessor
        implements ExtendedMemberAccessor {

    private final Type getterMethodParameterType;

    public ReflectionExtendedBeanPropertyMemberAccessor(Method getterMethod) {
        this(getterMethod, false);
    }

    public ReflectionExtendedBeanPropertyMemberAccessor(Method getterMethod, boolean getterOnly) {
        super(getterMethod, getterOnly, true);
        this.getterMethodParameterType = getGetterMethod().getGenericParameterTypes()[0];
    }

    @Override
    public Type getGetterMethodParameterType() {
        return getterMethodParameterType;
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
    public Object executeGetter(Object bean, Object value) {
        if (bean == null) {
            throw new IllegalArgumentException("Requested property (%s) getterMethod (%s) on a null bean."
                    .formatted(getPropertyName(), getGetterMethod()));
        }
        try {
            return getGetherMethodHandle().invoke(bean, value);
        } catch (Throwable e) {
            throw new IllegalStateException("The property (%s) getterMethod (%s) on bean of class (%s) throws an exception."
                    .formatted(getPropertyName(), getGetterMethod(), bean.getClass()), e);
        }
    }

    @Override
    public String toString() {
        return "extended bean property " + getPropertyName() + " on " + getGetterMethod().getDeclaringClass();
    }

}
