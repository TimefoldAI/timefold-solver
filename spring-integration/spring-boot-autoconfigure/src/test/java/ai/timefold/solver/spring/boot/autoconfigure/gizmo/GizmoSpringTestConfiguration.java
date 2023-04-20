package ai.timefold.solver.spring.boot.autoconfigure.gizmo;

import ai.timefold.solver.spring.boot.autoconfigure.gizmo.constraints.TestdataGizmoConstraintProvider;
import ai.timefold.solver.spring.boot.autoconfigure.gizmo.domain.TestdataGizmoSpringEntity;
import ai.timefold.solver.spring.boot.autoconfigure.gizmo.domain.TestdataGizmoSpringSolution;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@EntityScan(basePackageClasses = { TestdataGizmoSpringEntity.class, TestdataGizmoSpringSolution.class,
        TestdataGizmoConstraintProvider.class })
public class GizmoSpringTestConfiguration {
}
