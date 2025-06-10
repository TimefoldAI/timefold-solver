package ai.timefold.solver.core.impl.domain.common.accessor;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.function.IntPredicate;

import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;

/**
 * A {@link MemberAccessor} based on a getter and optionally a setter.
 */
public final class ReflectionBeanPropertyMemberAccessor extends AbstractMemberAccessor {

    private final Class<?> propertyType;
    private final String propertyName;
    private final Method getterMethod;
    private final MethodHandle getherMethodHandle;
    private final Method setterMethod;
    private final MethodHandle setterMethodHandle;

    public ReflectionBeanPropertyMemberAccessor(Method getterMethod) {
        this(getterMethod, false);
    }

    public ReflectionBeanPropertyMemberAccessor(Method getterMethod, boolean getterOnly) {
        this.getterMethod = getterMethod;
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            getterMethod.setAccessible(true); // Performance hack by avoiding security checks
            this.getherMethodHandle = lookup.unreflect(getterMethod)
                    .asFixedArity();
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("""
                    Impossible state: method (%s) not accessible.
                    %s"""
                    .formatted(getterMethod, MemberAccessorFactory.CLASSLOADER_NUDGE_MESSAGE), e);
        }
        Class<?> declaringClass = getterMethod.getDeclaringClass();
        if (!ReflectionHelper.isGetterMethod(getterMethod)) {
            throw new IllegalArgumentException("The getterMethod (%s) is not a valid getter."
                    .formatted(getterMethod));
        }
        propertyType = getterMethod.getReturnType();
        propertyName = ReflectionHelper.getGetterPropertyName(getterMethod);
        if (getterOnly) {
            setterMethod = null;
            setterMethodHandle = null;
        } else {
            setterMethod = ReflectionHelper.getDeclaredSetterMethod(declaringClass, getterMethod.getReturnType(), propertyName);
            if (setterMethod == null) {
                throw new IllegalArgumentException("The getterMethod (%s) does not have a matching setterMethod on class (%s)."
                        .formatted(getterMethod.getName(), declaringClass.getCanonicalName()));
            }
            var getterAccess = AccessModifier.forMethod(getterMethod);
            var setterAccess = AccessModifier.forMethod(setterMethod);
            if (getterAccess != setterAccess) {
                throw new IllegalArgumentException(
                        "The getterMethod (%s) has access modifier (%s) which does not match the setterMethod (%s) access modifier (%s) on class (%s)."
                                .formatted(getterMethod.getName(), getterAccess, setterMethod.getName(), setterAccess,
                                        declaringClass.getCanonicalName()));
            }
            try {
                setterMethod.setAccessible(true); // Performance hack by avoiding security checks
                this.setterMethodHandle = lookup.unreflect(setterMethod)
                        .asFixedArity();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("""
                        Impossible state: method (%s) not accessible.
                        %s"""
                        .formatted(setterMethod, MemberAccessorFactory.CLASSLOADER_NUDGE_MESSAGE), e);
            }
        }
    }

    private enum AccessModifier {
        PUBLIC("public", Modifier::isPublic),
        PROTECTED("protected", Modifier::isProtected),
        PACKAGE_PRIVATE("package-private", modifier -> false),
        PRIVATE("private", Modifier::isPrivate);

        final String name;
        final IntPredicate predicate;

        AccessModifier(String name, IntPredicate predicate) {
            this.name = name;
            this.predicate = predicate;
        }

        public static AccessModifier forMethod(Method method) {
            var modifiers = method.getModifiers();
            for (var accessModifier : AccessModifier.values()) {
                if (accessModifier.predicate.test(modifiers)) {
                    return accessModifier;
                }
            }
            return PACKAGE_PRIVATE;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Override
    public Class<?> getDeclaringClass() {
        return getterMethod.getDeclaringClass();
    }

    @Override
    public String getName() {
        return propertyName;
    }

    @Override
    public Class<?> getType() {
        return propertyType;
    }

    @Override
    public Type getGenericType() {
        return getterMethod.getGenericReturnType();
    }

    @Override
    public Object executeGetter(Object bean) {
        if (bean == null) {
            throw new IllegalArgumentException("Requested property (%s) getterMethod (%s) on a null bean."
                    .formatted(propertyName, getterMethod));
        }
        try {
            return getherMethodHandle.invoke(bean);
        } catch (Throwable e) {
            throw new IllegalStateException("The property (%s) getterMethod (%s) on bean of class (%s) throws an exception."
                    .formatted(propertyName, getterMethod, bean.getClass()), e);
        }
    }

    @Override
    public boolean supportSetter() {
        return setterMethod != null;
    }

    @Override
    public void executeSetter(Object bean, Object value) {
        if (bean == null) {
            throw new IllegalArgumentException("Requested property (%s) setterMethod (%s) on a null bean."
                    .formatted(propertyName, setterMethod));
        }
        try {
            setterMethodHandle.invoke(bean, value);
        } catch (Throwable e) {
            throw new IllegalStateException("The property (%s) setterMethod (%s) on bean of class (%s) throws an exception."
                    .formatted(propertyName, setterMethod, bean.getClass()), e);
        }
    }

    @Override
    public String getSpeedNote() {
        return "MethodHandle";
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        return getterMethod.getAnnotation(annotationClass);
    }

    @Override
    public <T extends Annotation> T[] getDeclaredAnnotationsByType(Class<T> annotationClass) {
        return getterMethod.getDeclaredAnnotationsByType(annotationClass);
    }

    @Override
    public String toString() {
        return "bean property " + propertyName + " on " + getterMethod.getDeclaringClass();
    }

}
