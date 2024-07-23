package ai.timefold.solver.core.impl.domain.common.accessor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;
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
                    memberAccessorType != MemberAccessorType.REGULAR_METHOD,
                    (GizmoClassLoader) Objects.requireNonNull(classLoader));
            case REFLECTION -> buildReflectiveMemberAccessor(member, memberAccessorType, annotationClass);
        };
    }

    private static MemberAccessor buildReflectiveMemberAccessor(Member member, MemberAccessorType memberAccessorType,
            Class<? extends Annotation> annotationClass) {
        if (member instanceof Field field) {
            return new ReflectionFieldMemberAccessor(field);
        } else if (member instanceof Method method) {
            MemberAccessor memberAccessor;
            switch (memberAccessorType) {
                case FIELD_OR_READ_METHOD:
                    if (!ReflectionHelper.isGetterMethod(method)) {
                        if (annotationClass == null) {
                            ReflectionHelper.assertReadMethod(method);
                        } else {
                            ReflectionHelper.assertReadMethod(method, annotationClass);
                        }
                        memberAccessor = new ReflectionMethodMemberAccessor(method);
                        break;
                    }
                    // Intentionally fall through (no break)
                case FIELD_OR_GETTER_METHOD:
                case FIELD_OR_GETTER_METHOD_WITH_SETTER:
                    boolean getterOnly = memberAccessorType != MemberAccessorType.FIELD_OR_GETTER_METHOD_WITH_SETTER;
                    if (annotationClass == null) {
                        ReflectionHelper.assertGetterMethod(method);
                    } else {
                        ReflectionHelper.assertGetterMethod(method, annotationClass);
                    }
                    memberAccessor = new ReflectionBeanPropertyMemberAccessor(method, getterOnly);
                    break;
                case REGULAR_METHOD:
                    memberAccessor = new ReflectionMethodMemberAccessor(method, false);
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
        FIELD_OR_GETTER_METHOD,
        FIELD_OR_GETTER_METHOD_WITH_SETTER,
        REGULAR_METHOD
    }
}
