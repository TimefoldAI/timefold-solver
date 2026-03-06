package ai.timefold.solver.core.impl.domain.solution.cloner;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

/**
 * Tests the {@link LambdaBasedSolutionCloner} both via the annotation path
 * (where it should now be the default) and via programmatic specification.
 * <p>
 * Inherits all tests from {@link AbstractSolutionClonerTest} using the lambda-based cloner
 * retrieved from the SolutionDescriptor (annotation path).
 */
class LambdaBasedSolutionClonerTest extends AbstractSolutionClonerTest {

    @Override
    protected <Solution_> SolutionCloner<Solution_> createSolutionCloner(
            SolutionDescriptor<Solution_> solutionDescriptor) {
        // The annotation path should now produce a LambdaBasedSolutionCloner
        return solutionDescriptor.getSolutionCloner();
    }

    @Test
    void annotationPathUsesLambdaBasedCloner() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var cloner = solutionDescriptor.getSolutionCloner();
        assertThat(cloner).isInstanceOf(LambdaBasedSolutionCloner.class);
    }
}
