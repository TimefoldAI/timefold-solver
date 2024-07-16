package ai.timefold.solver.core.impl.domain.common.accessor.gizmo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.Objects;

import ai.timefold.solver.core.api.domain.common.DomainAccessType;
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
     * @return never null
     */
    public static MemberAccessor buildGizmoMemberAccessor(Member member, Class<? extends Annotation> annotationClass,
            boolean returnTypeRequired, GizmoClassLoader gizmoClassLoader) {
        try {
            // Check if Gizmo on the classpath by verifying we can access one of its classes
            Class.forName("io.quarkus.gizmo.ClassCreator", false,
                    Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("When using the domainAccessType (" +
                    DomainAccessType.GIZMO +
                    ") the classpath or modulepath must contain io.quarkus.gizmo:gizmo.\n" +
                    "Maybe add a dependency to io.quarkus.gizmo:gizmo.");
        }
        return GizmoMemberAccessorImplementor.createAccessorFor(member, annotationClass, returnTypeRequired, gizmoClassLoader);
    }

    private GizmoMemberAccessorFactory() {
    }
}
