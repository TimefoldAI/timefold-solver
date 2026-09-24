package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListRuinRecreateMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListChangeMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.SubListSwapMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.kopt.KOptListMoveSelectorConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.score.director.ScoreDirectorFactoryConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.test.MoveTestContext;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Range;

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
        return SolverFactory.<Solution_> create(buildFullAssertSolverConfig(solutionClass, constraintProviderClass,
                entityClasses)).buildSolver().solve(problem);
    }

    /**
     * As {@link #solveWithFullAssert(Class, Class, Object, Class[])}, with sub list moves that may reverse
     * and ruin and recreate moves on top of the default list moves,
     * so that single moves change a list in several places.
     */
    static <Solution_> Solution_ solveWithFullAssertAndEveryListMove(Class<Solution_> solutionClass,
            Class<? extends ConstraintProvider> constraintProviderClass, Solution_ problem, Class<?>... entityClasses) {
        var solverConfig = buildFullAssertSolverConfig(solutionClass, constraintProviderClass, entityClasses)
                .withPhases(new ConstructionHeuristicPhaseConfig(),
                        new LocalSearchPhaseConfig().withMoveSelectorConfig(new UnionMoveSelectorConfig()
                                .withMoveSelectors(new ListChangeMoveSelectorConfig(), new ListSwapMoveSelectorConfig(),
                                        new SubListChangeMoveSelectorConfig().withSelectReversingMoveToo(true),
                                        new SubListSwapMoveSelectorConfig().withSelectReversingMoveToo(true),
                                        new KOptListMoveSelectorConfig(), new ListRuinRecreateMoveSelectorConfig())));
        return SolverFactory.<Solution_> create(solverConfig).buildSolver().solve(problem);
    }

    private static <Solution_> SolverConfig buildFullAssertSolverConfig(Class<Solution_> solutionClass,
            Class<? extends ConstraintProvider> constraintProviderClass, Class<?>... entityClasses) {
        return new SolverConfig()
                .withEnvironmentMode(EnvironmentMode.FULL_ASSERT)
                .withSolutionClass(solutionClass)
                .withEntityClasses(entityClasses)
                .withScoreDirectorFactory(new ScoreDirectorFactoryConfig()
                        .withConstraintProviderClass(constraintProviderClass))
                .withTerminationConfig(new TerminationConfig().withMoveCountLimit(1000L));
    }

    /**
     * Executes a random list move: an assignment, an unassignment, a change, a swap,
     * a sub list change or swap that may reverse, or a reversal.
     * Does nothing when the solution has no room for the move picked.
     */
    static <Solution_, Entity_, Value_> void executeRandomListMove(MoveTestContext<Solution_> context,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> listVariableMetaModel,
            Function<Entity_, List<Value_>> listFunction, List<Entity_> entityList, List<Value_> valueList,
            Random random) {
        var assignedValueSet = Collections.newSetFromMap(new IdentityHashMap<Value_, Boolean>());
        var assignedEntityList = new ArrayList<Entity_>();
        for (var entity : entityList) {
            var list = listFunction.apply(entity);
            assignedValueSet.addAll(list);
            if (!list.isEmpty()) {
                assignedEntityList.add(entity);
            }
        }
        var moveType = random.nextInt(8);
        if (moveType == 0 || assignedEntityList.isEmpty()) {
            var unassignedValueList = valueList.stream().filter(value -> !assignedValueSet.contains(value)).toList();
            if (!unassignedValueList.isEmpty()) {
                var entity = entityList.get(random.nextInt(entityList.size()));
                context.execute(Moves.assign(listVariableMetaModel,
                        unassignedValueList.get(random.nextInt(unassignedValueList.size())), entity,
                        random.nextInt(listFunction.apply(entity).size() + 1)));
            }
            return;
        }
        var source = assignedEntityList.get(random.nextInt(assignedEntityList.size()));
        var sourceSize = listFunction.apply(source).size();
        var sourceIndex = random.nextInt(sourceSize);
        var sourceToIndex = sourceIndex + 1 + random.nextInt(sourceSize - sourceIndex);
        var destination = entityList.get(random.nextInt(entityList.size()));
        var destinationSize = listFunction.apply(destination).size();
        var other = assignedEntityList.get(random.nextInt(assignedEntityList.size()));
        var otherIndex = random.nextInt(listFunction.apply(other).size());
        var otherToIndex = otherIndex + 1 + random.nextInt(listFunction.apply(other).size() - otherIndex);
        switch (moveType) {
            case 1 -> context.execute(Moves.unassign(listVariableMetaModel, source, sourceIndex));
            case 2 -> {
                var destinationIndex = random.nextInt(destination == source ? sourceSize : destinationSize + 1);
                if (destination != source || destinationIndex != sourceIndex) {
                    context.execute(Moves.change(listVariableMetaModel, source, sourceIndex, destination, destinationIndex));
                }
            }
            case 3 -> {
                if (other != source || otherIndex != sourceIndex) {
                    context.execute(Moves.swap(listVariableMetaModel, source, sourceIndex, other, otherIndex));
                }
            }
            case 4, 5 -> {
                var remainingSize = destination == source ? sourceSize - (sourceToIndex - sourceIndex) : destinationSize;
                context.execute(Moves.change(listVariableMetaModel, new Range<>(source, sourceIndex, sourceToIndex),
                        ElementPosition.of(destination, random.nextInt(remainingSize + 1)), random.nextBoolean()));
            }
            case 6 -> {
                if (other != source) {
                    context.execute(Moves.swap(listVariableMetaModel, new Range<>(source, sourceIndex, sourceToIndex),
                            new Range<>(other, otherIndex, otherToIndex), random.nextBoolean()));
                }
            }
            default -> {
                if (sourceToIndex - sourceIndex >= 2) {
                    context.execute(Moves.reverse(listVariableMetaModel, new Range<>(source, sourceIndex, sourceToIndex)));
                }
            }
        }
    }

    private DeclarativeShadowVariableAssertions() {
    }
}
