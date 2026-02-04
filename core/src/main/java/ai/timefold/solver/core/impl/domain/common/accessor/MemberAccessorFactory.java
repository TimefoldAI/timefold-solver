package ai.timefold.solver.core.impl.domain.common.accessor;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;
import ai.timefold.solver.core.impl.domain.common.accessor.gizmo.AccessorInfo;
import ai.timefold.solver.core.impl.domain.common.accessor.gizmo.GizmoClassLoader;
import ai.timefold.solver.core.impl.domain.common.accessor.gizmo.GizmoMemberAccessorFactory;

public final class MemberAccessorFactory {

    // exists only so that the various member accessors can share the same text in their exception messages
    static final String CLASSLOADER_NUDGE_MESSAGE =
            "Maybe add getClass().getClassLoader() as a parameter to the %s.create...() method call."
                    .formatted(SolverFactory.class.getSimpleName());

    /**
     * Creates a new member accessor based on the given parameters.
     *
     * @param member never null, method or field to access
     * @param memberAccessorType never null
     * @param domainAccessType never null
     * @param classLoader null or {@link GizmoClassLoader} if domainAccessType is {@link DomainAccessType#GIZMO}.
     * @return never null, new instance of the member accessor
     */
    public static MemberAccessor buildMemberAccessor(Member member, MemberAccessorType memberAccessorType,
            DomainAccessType domainAccessType, ClassLoader classLoader) {
        return buildMemberAccessor(member, memberAccessorType, null, domainAccessType, classLoader);
    }

    /**
     * Creates a new member accessor based on the given parameters.
     *
     * @param member never null, method or field to access
     * @param memberAccessorType never null
     * @param annotationClass the annotation the member was annotated with (used for error reporting)
     * @param domainAccessType never null
     * @param classLoader null or {@link GizmoClassLoader} if domainAccessType is {@link DomainAccessType#GIZMO}.
     * @return never null, new instance of the member accessor
     */
    public static MemberAccessor buildMemberAccessor(Member member, MemberAccessorType memberAccessorType,
            Class<? extends Annotation> annotationClass, DomainAccessType domainAccessType, ClassLoader classLoader) {
        return switch (domainAccessType) {
            case GIZMO -> GizmoMemberAccessorFactory.buildGizmoMemberAccessor(member, annotationClass,
                    AccessorInfo.of(memberAccessorType != MemberAccessorType.VOID_METHOD,
                            memberAccessorType == MemberAccessorType.FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER),
                    (GizmoClassLoader) Objects.requireNonNull(classLoader));
            case REFLECTION -> buildReflectiveMemberAccessor(member, memberAccessorType, annotationClass);
        };
    }

    private static MemberAccessor buildReflectiveMemberAccessor(Member member, MemberAccessorType memberAccessorType,
            Class<? extends Annotation> annotationClass) {
        return buildReflectiveMemberAccessor(member, memberAccessorType, annotationClass,
                (AnnotatedElement) member, memberAccessorType == MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER);
    }

    private static MemberAccessor buildReflectiveMemberAccessor(Member member, MemberAccessorType memberAccessorType,
            Class<? extends Annotation> annotationClass, AnnotatedElement annotatedElement, boolean requireSetter) {
        var messagePrefix = (annotationClass == null) ? "The" : "The @%s annotated".formatted(annotationClass.getSimpleName());
        if (member instanceof Field field) {
            var getter = ReflectionHelper.getGetterMethod(field.getDeclaringClass(), field.getName());
            if (getter == null) {
                var setter = ReflectionHelper.getSetterMethod(field.getDeclaringClass(), field.getName());
                if (setter != null) {
                    throw new IllegalArgumentException("%s field (%s) on class (%s) has a setter (%s) but no getter."
                            .formatted(messagePrefix, field.getName(), field.getDeclaringClass().getCanonicalName(), setter));
                }

                if (Modifier.isFinal(field.getModifiers()) && requireSetter) {
                    throw new IllegalArgumentException("%s field (%s) on class (%s) is final but requires a setter."
                            .formatted(messagePrefix, field.getName(), field.getDeclaringClass().getCanonicalName()));
                }

                if (Modifier.isPublic(field.getModifiers())) {
                    return new ReflectionFieldMemberAccessor(field);
                } else {
                    throw new IllegalArgumentException(
                            "%s field (%s) on class (%s) is not public and does not have a public getter method."
                                    .formatted(messagePrefix, field.getName(), field.getDeclaringClass().getName()));
                }
            }
            // Final fields may only have a getter
            // Non-final fields MUST have both a getter and setter
            return buildReflectiveMemberAccessor(getter, memberAccessorType, annotationClass,
                    field, requireSetter || !Modifier.isFinal(field.getModifiers()));
        } else if (member instanceof Method method) {
            MemberAccessor memberAccessor;
            if (!Modifier.isPublic(method.getModifiers())) {
                throw new IllegalStateException("%s method (%s) on class (%s) is not public."
                        .formatted(messagePrefix, method.getName(), method.getDeclaringClass().getName()));
            }
            switch (memberAccessorType) {
                case FIELD_OR_READ_METHOD, FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER:
                    if (!ReflectionHelper.isGetterMethod(method)) {
                        boolean methodWithParameter =
                                memberAccessorType == MemberAccessorType.FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER
                                        && method.getParameterCount() > 0;
                        if (annotationClass == null) {
                            ReflectionHelper.assertReadMethod(method, methodWithParameter);
                        } else {
                            ReflectionHelper.assertReadMethod(method, methodWithParameter, annotationClass);
                        }
                        memberAccessor = methodWithParameter ? new ReflectionMethodExtendedMemberAccessor(method)
                                : new ReflectionMethodMemberAccessor(method);
                        break;
                    }
                    // Intentionally fall through (no break)
                case FIELD_OR_GETTER_METHOD, FIELD_OR_GETTER_METHOD_WITH_SETTER:
                    boolean getterOnly = !requireSetter;
                    if (annotationClass == null) {
                        ReflectionHelper.assertGetterMethod(method);
                    } else {
                        ReflectionHelper.assertGetterMethod(method, annotationClass);
                    }
                    memberAccessor = new ReflectionBeanPropertyMemberAccessor(method, annotatedElement, getterOnly);
                    break;
                case VOID_METHOD:
                    memberAccessor = new ReflectionMethodMemberAccessor(method, false, false);
                    break;
                default:
                    throw new IllegalStateException("The memberAccessorType (%s) is not implemented."
                            .formatted(memberAccessorType));
            }
            if (memberAccessorType == MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER
                    && !memberAccessor.supportSetter()) {
                if (annotationClass == null) {
                    throw new IllegalStateException(
                            "The class (%s) has a getter method (%s), but lacks a setter for that property (%s)."
                                    .formatted(method.getDeclaringClass(), method, memberAccessor.getName()));
                } else {
                    throw new IllegalStateException(
                            "The class (%s) has a @%s-annotated getter method (%s), but lacks a setter for that property (%s)."
                                    .formatted(method.getDeclaringClass(), annotationClass.getSimpleName(), method,
                                            memberAccessor.getName()));
                }
            }
            return memberAccessor;
        } else {
            throw new IllegalStateException("Impossible state: the member (%s)'s type is not a %s or a %s."
                    .formatted(member, Field.class.getSimpleName(), Method.class.getSimpleName()));
        }
    }

    private final Map<String, MemberAccessor> memberAccessorCache;
    private final GizmoClassLoader gizmoClassLoader = new GizmoClassLoader();

    public MemberAccessorFactory() {
        this(null);
    }

    /**
     * Prefills the member accessor cache.
     *
     * @param memberAccessorMap key is the fully qualified member name
     */
    public MemberAccessorFactory(Map<String, MemberAccessor> memberAccessorMap) {
        // The MemberAccessorFactory may be accessed, and this cache both read and updated, by multiple threads.
        this.memberAccessorCache =
                memberAccessorMap == null ? new ConcurrentHashMap<>() : new ConcurrentHashMap<>(memberAccessorMap);
    }

    /**
     * Creates a new member accessor based on the given parameters. Caches the result.
     *
     * @param member never null, method or field to access
     * @param memberAccessorType never null
     * @param annotationClass the annotation the member was annotated with (used for error reporting)
     * @param domainAccessType never null
     * @return never null, new {@link MemberAccessor} instance unless already found in memberAccessorMap
     */
    public MemberAccessor buildAndCacheMemberAccessor(Member member, MemberAccessorType memberAccessorType,
            Class<? extends Annotation> annotationClass, DomainAccessType domainAccessType) {
        String generatedClassName = GizmoMemberAccessorFactory.getGeneratedClassName(member);
        return memberAccessorCache.computeIfAbsent(generatedClassName,
                k -> MemberAccessorFactory.buildMemberAccessor(member, memberAccessorType, annotationClass, domainAccessType,
                        gizmoClassLoader));
    }

    /**
     * Creates a new member accessor based on the given parameters. Caches the result.
     *
     * @param member never null, method or field to access
     * @param memberAccessorType never null
     * @param domainAccessType never null
     * @return never null, new {@link MemberAccessor} instance unless already found in memberAccessorMap
     */
    public MemberAccessor buildAndCacheMemberAccessor(Member member, MemberAccessorType memberAccessorType,
            DomainAccessType domainAccessType) {
        String generatedClassName = GizmoMemberAccessorFactory.getGeneratedClassName(member);
        return memberAccessorCache.computeIfAbsent(generatedClassName,
                k -> MemberAccessorFactory.buildMemberAccessor(member, memberAccessorType, domainAccessType, gizmoClassLoader));
    }

    public GizmoClassLoader getGizmoClassLoader() {
        return gizmoClassLoader;
    }

    public enum MemberAccessorType {
        FIELD_OR_READ_METHOD,
        FIELD_OR_READ_METHOD_WITH_OPTIONAL_PARAMETER,
        FIELD_OR_GETTER_METHOD,
        FIELD_OR_GETTER_METHOD_WITH_SETTER,
        VOID_METHOD
    }
}
