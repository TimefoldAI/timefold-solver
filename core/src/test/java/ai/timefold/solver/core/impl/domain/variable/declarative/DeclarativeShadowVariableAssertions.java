package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;

final class DeclarativeShadowVariableAssertions {

    /**
     * Asserts that the incrementally maintained shadow variables equal a from-scratch recomputation,
     * which uses the arbitrary graph.
     *
     * @param shadowValueExtractors one per shadow variable to compare, each returning its value for every entity
     */
    @SafeVarargs
    static <Solution_> void assertShadowsAreAtFixedPoint(Solution_ solution,
            Function<Solution_, List<?>>... shadowValueExtractors) {
        var incrementalValueLists = Arrays.stream(shadowValueExtractors)
                .map(extractor -> new ArrayList<Object>(extractor.apply(solution)))
                .toList();

        SolutionManager.updateShadowVariables(solution);

        for (var i = 0; i < shadowValueExtractors.length; i++) {
            var recomputedValueList = new ArrayList<Object>(shadowValueExtractors[i].apply(solution));
            assertThat(recomputedValueList).containsExactlyElementsOf(incrementalValueLists.get(i));
        }
    }

    static <Solution_> Solution_ solveWithFullAssert(Class<Solution_> solutionClass,
            Class<? extends ConstraintProvider> constraintProviderClass, Solution_ problem, Class<?>... entityClasses) {
        var solverConfig = new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withSolutionClass(solutionClass)
                .withEntityClasses(entityClasses)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                        .withConstraintProviderClass(constraintProviderClass))
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(1000L));
        return SolverFactory.<Solution_> create(solverConfig).buildSolver().solve(problem);
    }

    private DeclarativeShadowVariableAssertions() {
    }
}
