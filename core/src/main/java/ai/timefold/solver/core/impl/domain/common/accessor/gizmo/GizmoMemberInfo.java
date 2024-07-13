package ai.timefold.solver.core.impl.domain.common.accessor.gizmo;

import java.lang.annotation.Annotation;

/**
 * @param descriptor never null
 * @param annotationClass null if not annotated
 */
public record GizmoMemberInfo(GizmoMemberDescriptor descriptor, Class<? extends Annotation> annotationClass) {

}
