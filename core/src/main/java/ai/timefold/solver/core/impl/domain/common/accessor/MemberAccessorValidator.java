package ai.timefold.solver.core.impl.domain.common.accessor;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
final class MemberAccessorValidator {
    private MemberAccessorValidator() {
    }

    public static void verifyIsValidMember(@Nullable Class<?> annotationClass, Member member,
            MemberAccessorType memberAccessorType) {
        var memberType = (member instanceof Field) ? "field" : "method";
        var messagePrefix = (annotationClass == null)
                ? "The %s (%s) in class (%s)".formatted(memberType, member.getName(),
                        member.getDeclaringClass().getCanonicalName())
                : "The @%s annotated %s (%s) in class (%s)".formatted(annotationClass.getSimpleName(), memberType,
                        member.getName(),
                        member.getDeclaringClass().getCanonicalName());

        verifyDeclaringClassIsAccessible(member, messagePrefix);
        switch (memberAccessorType) {
            case FIELD_OR_READ_METHOD -> verifyIsPublicFieldOrHasReadMethod(member, messagePrefix, false);
            case FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER ->
                verifyIsPublicFieldOrHasReadMethod(member, messagePrefix, true);
            case FIELD_OR_GETTER_METHOD -> verifyFieldOrGetter(member, messagePrefix);
            case VOID_METHOD -> verifyIsVoidMethod(member, messagePrefix);
            case FIELD_OR_GETTER_METHOD_WITH_SETTER -> {
                verifyFieldOrGetter(member, messagePrefix);
                verifyIsPublicFieldOrHasPublicSetter(member, messagePrefix);
            }
        }
    }

    private static void verifyDeclaringClassIsAccessible(Member member, String messagePrefix) {
        var declaringClass = member.getDeclaringClass();
        if (!Modifier.isPublic(declaringClass.getModifiers())) {
            throw new IllegalArgumentException(
                    "%s is not accessible because its declaring class (%s) is not public. Maybe make the class (%s) public?"
                            .formatted(messagePrefix, declaringClass.getCanonicalName(), declaringClass.getSimpleName()));
        }
    }

    private static void verifyIsVoidMethod(Member member, String messagePrefix) {
        if (!(member instanceof Method method)) {
            throw new IllegalArgumentException(
                    "%s must be a void method, but is a field instead.".formatted(messagePrefix));
        }
        if (!method.getReturnType().equals(void.class)) {
            throw new IllegalArgumentException("%s must be a void method, but it returns (%s).".formatted(messagePrefix,
                    method.getReturnType().getCanonicalName()));
        }
        if (!Modifier.isPublic(method.getModifiers())) {
            throw new IllegalArgumentException(
                    "%s is a void method, but it is not public. Maybe make the method (%s) public?"
                            .formatted(messagePrefix, method.getName()));
        }
    }

    private static void verifyGetterName(Method method, String messagePrefix) {
        if (!method.getName().startsWith("get") && !(method.getReturnType().equals(boolean.class) && method.getName()
                .startsWith("is"))) {
            throw new IllegalArgumentException("""
                    %s is suppose to be a public getter method, but its name (%s) does not start with "get"%s.
                    Maybe add a "get" prefix to the method?"""
                    .formatted(messagePrefix, method.getName(),
                            method.getReturnType().equals(boolean.class) ? " or \"is\"" : ""));
        }
    }

    private static void verifyFieldOrGetter(Member member, String messagePrefix) {
        switch (member) {
            case Field field -> verifyIsPublicFieldOrHasPublicGetter(field, messagePrefix);
            case Method method -> {
                verifyGetterName(method, messagePrefix);
                if (!Modifier.isPublic(method.getModifiers())) {
                    throw new IllegalArgumentException(
                            "%s is a getter method, but it is not public. Maybe make the method (%s) public?"
                                    .formatted(messagePrefix, method.getName()));
                }
                if (method.getReturnType().equals(void.class)) {
                    throw new IllegalArgumentException(
                            "%s has a public getter method that returns void. Maybe make the method (%s) return a value instead?"
                                    .formatted(messagePrefix, method.getName()));
                }
            }
            default -> throw new IllegalArgumentException("Unhandled member type (%s)."
                    .formatted(member.getClass().getCanonicalName()));
        }
    }

    private static void verifyIsPublicFieldOrHasPublicGetter(Field field, String messagePrefix) {
        var getterMethod = ReflectionHelper.getGetterMethod(field.getDeclaringClass(), field.getName());
        if (getterMethod == null) {
            if (!Modifier.isPublic(field.getModifiers())) {
                throw new IllegalArgumentException(
                        "%s is not public and does not have a public getter method. Maybe add a public getter method?"
                                .formatted(messagePrefix));
            }
        } else {
            if (!Modifier.isPublic(getterMethod.getModifiers())) {
                throw new IllegalArgumentException(
                        "%s has a non-public getter method. Maybe make the method (%s) public?"
                                .formatted(messagePrefix, getterMethod.getName()));
            }
            if (getterMethod.getReturnType().equals(void.class)) {
                throw new IllegalArgumentException(
                        "%s has a public getter method that returns void. Maybe make the method (%s) return (%s) instead?"
                                .formatted(messagePrefix, getterMethod.getName(), field.getType().getCanonicalName()));
            }
        }
    }

    private static void verifyIsPublicFieldOrHasReadMethod(Member member, String messagePrefix,
            boolean acceptOptionalParameter) {
        switch (member) {
            case Field field -> verifyIsPublicFieldOrHasPublicGetter(field, messagePrefix);
            case Method method -> {
                if (!Modifier.isPublic(method.getModifiers())) {
                    throw new IllegalArgumentException(
                            "%s is a read method, but it is not public. Maybe make the method (%s) public?"
                                    .formatted(messagePrefix, method.getName()));
                }
                if (method.getReturnType().equals(void.class)) {
                    throw new IllegalArgumentException(
                            "%s is a public read method, but it returns void. Maybe make the method (%s) return a value instead?"
                                    .formatted(messagePrefix, method.getName()));
                }

                if (acceptOptionalParameter) {
                    if (method.getParameterCount() > 1) {
                        throw new IllegalArgumentException("""
                                %s is a public read method, but takes (%d) parameters instead of zero or one.
                                Maybe make the method (%s) take zero or one parameter(s)?"""
                                .formatted(messagePrefix, method.getParameterCount(), method.getName()));
                    }
                } else if (method.getParameterCount() != 0) {
                    throw new IllegalArgumentException("""
                            %s is a public read method, but takes (%d) parameters instead of none.
                            Maybe make the method (%s) take no parameters?"""
                            .formatted(messagePrefix, method.getParameterCount(), method.getName()));
                }
            }
            default -> throw new IllegalArgumentException("Unhandled member type (%s)."
                    .formatted(member.getClass().getCanonicalName()));
        }
    }

    private static void verifyIsPublicFieldOrHasPublicSetter(Member member, String messagePrefix) {
        switch (member) {
            case Field field -> {
                var getterMethod = ReflectionHelper.getGetterMethod(field.getDeclaringClass(), field.getName());
                var setterMethod = ReflectionHelper.getSetterMethod(field.getDeclaringClass(), field.getName());
                if (setterMethod == null) {
                    if (!Modifier.isPublic(field.getModifiers())) {
                        throw new IllegalArgumentException(
                                "%s does not have a setter method and is not public. Maybe add a public setter method?"
                                        .formatted(messagePrefix));
                    }
                } else {
                    if (getterMethod == null) {
                        throw new IllegalArgumentException(
                                "%s has a setter method (%s) without a getter method. Maybe add a public getter method?"
                                        .formatted(member, setterMethod.getName()));
                    }
                    verifyGetterSetterProperties(getterMethod, setterMethod, messagePrefix);
                }
            }
            case Method getterMethod -> {
                verifyGetterName(getterMethod, messagePrefix);
                var memberName = ReflectionHelper.getGetterPropertyName(getterMethod);
                var setterMethod = ReflectionHelper.getSetterMethod(getterMethod.getDeclaringClass(),
                        memberName);
                if (setterMethod == null) {
                    throw new IllegalArgumentException("""
                            %s requires both a public getter and a public setter but only have a public getter.
                            Maybe add a public setter for the member (%s)?"""
                            .formatted(messagePrefix, memberName));
                }
                verifyGetterSetterProperties(getterMethod, setterMethod, messagePrefix);
            }
            default -> throw new IllegalArgumentException("Unhandled member type (%s)."
                    .formatted(member.getClass().getCanonicalName()));
        }
    }

    private static void verifyGetterSetterProperties(Method getterMethod, Method setterMethod, String messagePrefix) {
        if (!Modifier.isPublic(getterMethod.getModifiers())) {
            throw new IllegalArgumentException(
                    "%s has a non-public getter method. Maybe make the method (%s) public?"
                            .formatted(messagePrefix, getterMethod.getName()));
        }
        if (!Modifier.isPublic(setterMethod.getModifiers())) {
            throw new IllegalArgumentException(
                    "%s has a non-public setter method. Maybe make the method (%s) public?"
                            .formatted(messagePrefix, setterMethod.getName()));
        }
        if (setterMethod.getParameterCount() != 1) {
            throw new IllegalArgumentException("""
                    %s has a public setter method that takes (%d) parameters instead of one.
                    Maybe make the method (%s) take exactly one parameter?"""
                    .formatted(messagePrefix, setterMethod.getParameterCount(),
                            setterMethod.getName()));
        }
        if (!setterMethod.getParameterTypes()[0].isAssignableFrom(getterMethod.getReturnType())) {
            throw new IllegalArgumentException(
                    """
                            %s has a public setter method but its parameter type (%s) is not assignable from the getter return type (%s).
                            Maybe make the public setter (%s) accept (%s)?"""
                            .formatted(messagePrefix, setterMethod.getParameterTypes()[0].getCanonicalName(),
                                    getterMethod.getReturnType().getCanonicalName(), setterMethod.getName(),
                                    getterMethod.getReturnType().getCanonicalName()));
        }
        if (!setterMethod.getReturnType().equals(void.class)) {
            throw new IllegalArgumentException(
                    "%s has a public getter method that returns void. Maybe make the method (%s) return (%s) instead?"
                            .formatted(messagePrefix, setterMethod.getName(), getterMethod.getReturnType().getCanonicalName()));
        }
    }
}
