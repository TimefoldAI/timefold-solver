package ai.timefold.solver.core.impl.domain.common;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import ai.timefold.solver.core.api.domain.entity.PlanningPin;

import org.jspecify.annotations.Nullable;

/**
 * Locates the optional {@link PlanningPin} setter that accepts the planning solution.
 */
public final class PlanningPinSupport {

    private PlanningPinSupport() {
    }

    /**
     * Returns the public void setter for {@code propertyName} whose single parameter is the planning solution
     * (or a supertype of it). Returns null when the only setter is the boolean value setter.
     */
    public static @Nullable Method findSolutionArgumentSetter(Class<?> entityClass, String propertyName,
            Class<?> propertyType, Class<?> solutionClass) {
        var setterName = ReflectionHelper.PROPERTY_MUTATOR_PREFIX + ReflectionHelper.capitalizePropertyName(propertyName);
        Method match = null;
        for (var method : entityClass.getMethods()) {
            if (!isCandidate(method, setterName)) {
                continue;
            }
            var parameterType = method.getParameterTypes()[0];
            if (isBooleanPropertyParameter(propertyType, parameterType)) {
                continue;
            }
            assertValidSolutionParameter(entityClass, solutionClass, method, parameterType);
            match = preferMoreSpecific(entityClass, match, method);
        }
        assertNonPublicSolutionSetterIsRejected(entityClass, setterName, propertyType, solutionClass);
        return match;
    }

    private static boolean isCandidate(Method method, String setterName) {
        return method.getName().equals(setterName)
                && method.getParameterCount() == 1
                && !Modifier.isStatic(method.getModifiers())
                && !method.isSynthetic()
                && !method.isBridge();
    }

    private static boolean isBooleanPropertyParameter(Class<?> propertyType, Class<?> parameterType) {
        var propertyIsBoolean = propertyType == boolean.class || propertyType == Boolean.class;
        var parameterIsBoolean = parameterType == boolean.class || parameterType == Boolean.class;
        return propertyIsBoolean && parameterIsBoolean;
    }

    private static void assertValidSolutionParameter(Class<?> entityClass, Class<?> solutionClass, Method method,
            Class<?> parameterType) {
        if (method.getReturnType() != void.class) {
            throw new IllegalStateException("""
                    The entityClass (%s) has a @%s setter (%s) that does not return void.
                    Maybe change it to void?"""
                    .formatted(entityClass.getCanonicalName(), PlanningPin.class.getSimpleName(), method));
        }
        if (!parameterType.isAssignableFrom(solutionClass)) {
            throw new IllegalStateException("""
                    The entityClass (%s) has a @%s setter (%s) whose parameter type (%s) is not the planning solution (%s).
                    Maybe change the parameter to (%s), or remove the overload?"""
                    .formatted(entityClass.getCanonicalName(), PlanningPin.class.getSimpleName(), method,
                            parameterType.getCanonicalName(), solutionClass.getCanonicalName(),
                            solutionClass.getCanonicalName()));
        }
    }

    private static Method preferMoreSpecific(Class<?> entityClass, @Nullable Method current, Method candidate) {
        if (current == null) {
            return candidate;
        }
        var currentParameterType = current.getParameterTypes()[0];
        var candidateParameterType = candidate.getParameterTypes()[0];
        if (currentParameterType.equals(candidateParameterType)) {
            return current;
        }
        if (currentParameterType.isAssignableFrom(candidateParameterType)) {
            return candidate;
        }
        if (candidateParameterType.isAssignableFrom(currentParameterType)) {
            return current;
        }
        throw new IllegalStateException("""
                The entityClass (%s) has multiple @%s setters that accept the planning solution: (%s) and (%s).
                Maybe keep only one?"""
                .formatted(entityClass.getCanonicalName(), PlanningPin.class.getSimpleName(), current, candidate));
    }

    private static void assertNonPublicSolutionSetterIsRejected(Class<?> entityClass, String setterName,
            Class<?> propertyType, Class<?> solutionClass) {
        var current = entityClass;
        while (current != null && current != Object.class) {
            for (var method : current.getDeclaredMethods()) {
                if (!isCandidate(method, setterName) || Modifier.isPublic(method.getModifiers())) {
                    continue;
                }
                var parameterType = method.getParameterTypes()[0];
                if (isBooleanPropertyParameter(propertyType, parameterType)
                        || !parameterType.isAssignableFrom(solutionClass)) {
                    continue;
                }
                throw new IllegalStateException("""
                        The entityClass (%s) has a @%s setter (%s) that accepts the planning solution, but it is not public.
                        Maybe make the method public?"""
                        .formatted(entityClass.getCanonicalName(), PlanningPin.class.getSimpleName(), method));
            }
            current = current.getSuperclass();
        }
    }

}
