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
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;

final class DeclarativeShadowVariableAssertions {

    /**
     * Asserts that the incrementally maintained shadow variables equal a from-scratch recomputation.
     *
     * @param shadowValueExtractors one per shadow variable to compare, each returning its value for every entity
     */
    @SafeVarargs
    @SuppressWarnings("unchecked")
    static <Solution_> void assertShadowsAreAtFixedPoint(Solution_ solution,
            Function<Solution_, List<?>>... shadowValueExtractors) {
        var incrementalValueLists = Arrays.stream(shadowValueExtractors)
                .map(extractor -> new ArrayList<Object>(extractor.apply(solution)))
                .toList();

        recomputeFromScratch((Class<Solution_>) solution.getClass(), solution);

        for (var i = 0; i < shadowValueExtractors.length; i++) {
            var recomputedValueList = new ArrayList<Object>(shadowValueExtractors[i].apply(solution));
            assertThat(recomputedValueList).containsExactlyElementsOf(incrementalValueLists.get(i));
        }
    }

    /**
     * Recomputes every shadow variable of the solution from scratch.
     * The built-in shadow variables are recomputed by
     * {@link SolutionManager#updateShadowVariables(Class, Object...)}, since the variable reference graph
     * only ever covers declarative shadow variables.
     * The declarative shadow variables are then recomputed by {@link GraphStructure#ARBITRARY},
     * whatever structure the model would otherwise use:
     * building the graph marks all of its nodes changed, so this last pass alone decides their values,
     * making the reference independent of the graph under test.
     */
    private static <Solution_> void recomputeFromScratch(Class<Solution_> solutionClass, Solution_ solution) {
        var solutionDescriptor = SolutionDescriptor.buildSolutionDescriptor(solutionClass);
        var entityList = new ArrayList<>();
        solutionDescriptor.visitAllEntities(solution, entityList::add);
        var entities = entityList.toArray();
        SolutionManager.updateShadowVariables(solutionClass, entities);
        var graphDescriptor = new DefaultShadowVariableSessionFactory.GraphDescriptor<>(solutionDescriptor,
                ChangedVariableNotifier.empty(), entities);
        DefaultShadowVariableSessionFactory.buildGraphForStructureAndDirection(
                new GraphStructure.GraphStructureAndDirection(GraphStructure.ARBITRARY, null, null), graphDescriptor)
                .updateChanged();
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
