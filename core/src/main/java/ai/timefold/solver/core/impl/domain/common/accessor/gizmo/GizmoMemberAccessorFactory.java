package ai.timefold.solver.core.impl.domain.common.accessor.gizmo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.Objects;

import ai.timefold.solver.core.impl.domain.common.ReflectionHelper;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;

public class GizmoMemberAccessorFactory {
    /**
     * Returns the generated class name for a given member.
     * (Here as accessing any method of GizmoMemberAccessorImplementor
     * will try to load Gizmo code)
     *
     * @param member The member to get the generated class name for
     * @return The generated class name for member
     */
    public static String getGeneratedClassName(Member member) {
        String memberName = Objects.requireNonNullElse(ReflectionHelper.getGetterPropertyName(member), member.getName());
        String memberType = (member instanceof Field) ? "Field" : "Method";

        return member.getDeclaringClass().getName() + "$Timefold$MemberAccessor$" + memberType + "$" + memberName;
    }

    /**
     *
     * @param member never null
     * @param annotationClass may be null if the member is not annotated
     * @param gizmoClassLoader never null
     * @param accessorInfo additional information of the accessor
     * @return never null
     */
    public static MemberAccessor buildGizmoMemberAccessor(Member member, Class<? extends Annotation> annotationClass,
            AccessorInfo accessorInfo, GizmoClassLoader gizmoClassLoader) {
        return GizmoMemberAccessorImplementor.createAccessorFor(member, annotationClass, accessorInfo, gizmoClassLoader);
    }

    private GizmoMemberAccessorFactory() {
    }
}
