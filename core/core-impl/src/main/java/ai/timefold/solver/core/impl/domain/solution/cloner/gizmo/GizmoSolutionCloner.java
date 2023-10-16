package ai.timefold.solver.core.impl.domain.solution.cloner.gizmo;

import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

public interface GizmoSolutionCloner<Solution_> extends SolutionCloner<Solution_> {
    void setSolutionDescriptor(SolutionDescriptor<Solution_> descriptor);
}
