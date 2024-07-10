package ai.timefold.solver.core.impl.domain.common.accessor.gizmo;

import java.lang.annotation.Annotation;

/**
 * @param descriptor never null
 * @param requiredReturnType true if the method return type is required
 * @param annotationClass null if not annotated
 */
public record GizmoMemberInfo(GizmoMemberDescriptor descriptor, boolean requiredReturnType,
        Class<? extends Annotation> annotationClass) {

}
