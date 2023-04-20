package ai.timefold.solver.test.impl.score.buildin.simplebigdecimal;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.core.api.score.stream.ConstraintStream;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.test.api.score.stream.ConstraintVerifier;
import ai.timefold.solver.test.impl.score.AbstractScoreVerifier;

/**
 * To assert the constraints of a {@link SolverFactory}
 * that uses a {@link SimpleBigDecimalScore}.
 * If you're using {@link ConstraintStream}s, use {@link ConstraintVerifier} instead.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 * @deprecated Score DRL is deprecated and will be removed in a future major version of Timefold.
 *             See <a href="https://timefold.ai/docs/">DRL to
 *             Constraint Streams migration recipe</a>.
 */
@Deprecated(forRemoval = true)
public class SimpleBigDecimalScoreVerifier<Solution_> extends AbstractScoreVerifier<Solution_> {

    /**
     * @param solverFactory never null, the {@link SolverFactory} of which you want to test the constraints.
     */
    public SimpleBigDecimalScoreVerifier(SolverFactory<Solution_> solverFactory) {
        super(solverFactory, SimpleBigDecimalScore.class);
    }

    /**
     * Assert that the constraint of {@link PlanningSolution}
     * has the expected weight for that score level.
     *
     * @param constraintName never null, the name of the constraint
     * @param expectedWeight never null, the total weight for all matches of that 1 constraint
     * @param solution never null, the actual {@link PlanningSolution}
     */
    public void assertWeight(String constraintName, BigDecimal expectedWeight, Solution_ solution) {
        assertWeight(null, constraintName, expectedWeight, solution);
    }

    /**
     * Assert that the constraint of {@link PlanningSolution}
     * has the expected weight for that score level.
     *
     * @param constraintPackage sometimes null.
     *        When null, {@code constraintName} for the {@code scoreLevel} must be unique.
     * @param constraintName never null, the name of the constraint
     * @param expectedWeight never null, the total weight for all matches of that 1 constraint
     * @param solution never null, the actual {@link PlanningSolution}
     */
    public void assertWeight(String constraintPackage, String constraintName, BigDecimal expectedWeight, Solution_ solution) {
        assertWeight(constraintPackage, constraintName, 0, expectedWeight, solution);
    }

}
