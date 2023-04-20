package ai.timefold.solver.core.impl.domain.solution.cloner;

import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

class FieldAccessingSolutionClonerTest extends AbstractSolutionClonerTest {

    @Override
    protected <Solution_> SolutionCloner<Solution_> createSolutionCloner(
            SolutionDescriptor<Solution_> solutionDescriptor) {
        return new FieldAccessingSolutionCloner<>(solutionDescriptor);
    }
}
