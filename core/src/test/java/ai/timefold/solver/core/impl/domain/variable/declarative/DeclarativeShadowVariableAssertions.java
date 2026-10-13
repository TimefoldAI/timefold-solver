package ai.timefold.solver.core.impl.domain.variable.declarative;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.violation.ShadowVariablesAssert;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.builtin.Moves;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Range;

import org.jspecify.annotations.Nullable;

final class DeclarativeShadowVariableAssertions {

    private static final long VIOLATION_DISPLAY_LIMIT = 10L;

    /**
     * Executes random list moves on the solution, and after each of them asserts that the shadow variables,
     * maintained incrementally by the graph of the expected structure, match {@link #assertMatchesArbitraryGraph}.
     *
     * @param unassigning whether the moves may unassign values, which ruin and recreate does
     *        even when the list variable does not allow unassigned values
     */
    static <Solution_, Entity_, Value_> void assertRandomListMovesMatchArbitraryGraph(
            SolutionDescriptor<Solution_> solutionDescriptor, Solution_ solution, GraphStructure expectedStructure,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> listVariableMetaModel,
            Function<Entity_, List<Value_>> listFunction, List<Entity_> entityList, List<Value_> valueList,
            boolean unassigning, int moveCount) {
        var entities = new ArrayList<>();
        solutionDescriptor.visitAllEntities(solution, entities::add);
        assertThat(GraphStructure.determineGraphStructure(solutionDescriptor, entities.toArray()).structure())
                .isEqualTo(expectedStructure);

        var context = MoveTester.build(solutionDescriptor.getMetaModel()).using(solution);
        assertMatchesArbitraryGraph(solutionDescriptor, solution, "Initial solution");
        var random = new Random(0);
        for (var moveIndex = 0; moveIndex < moveCount; moveIndex++) {
            var move = randomListMove(listVariableMetaModel, listFunction, entityList, valueList, unassigning, random);
            if (move != null) {
                context.execute(move);
                assertMatchesArbitraryGraph(solutionDescriptor, solution, "Move %d (%s)", moveIndex, move);
            }
        }
    }

    /**
     * Asserts that the shadow variables of the solution do not change when its declarative shadow variables
     * are recomputed from scratch with {@link GraphStructure#ARBITRARY},
     * the general graph that every other structure must agree with.
     * Building the graph marks all of its nodes changed, so that recomputation does not depend on any prior state.
     */
    static <Solution_> void assertMatchesArbitraryGraph(SolutionDescriptor<Solution_> solutionDescriptor,
            Solution_ solution, String description, Object... descriptionArgs) {
        var snapshot = ShadowVariablesAssert.takeSnapshot(solutionDescriptor, solution);
        var entities = new ArrayList<>();
        solutionDescriptor.visitAllEntities(solution, entities::add);
        DefaultShadowVariableSessionFactory.buildGraphForStructureAndDirection(
                new GraphStructure.GraphStructureAndDirection(GraphStructure.ARBITRARY, null, null),
                new DefaultShadowVariableSessionFactory.GraphDescriptor<>(solutionDescriptor,
                        ChangedVariableNotifier.empty(), entities.toArray()))
                .updateChanged();
        assertThat(snapshot.createShadowVariablesViolationMessage(VIOLATION_DISPLAY_LIMIT))
                .as(description, descriptionArgs)
                .isNull();
    }

    /**
     * Returns a random list move: an assignment, an unassignment of one or several values if unassigning,
     * a change, a swap, a sub list change or swap that may reverse, or a reversal.
     * Returns null when the solution has no room for the move picked.
     */
    private static <Solution_, Entity_, Value_> @Nullable Move<Solution_> randomListMove(
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> listVariableMetaModel,
            Function<Entity_, List<Value_>> listFunction, List<Entity_> entityList, List<Value_> valueList,
            boolean unassigning, Random random) {
        var assignedValueSet = Collections.newSetFromMap(new IdentityHashMap<Value_, Boolean>());
        var assignedEntityList = new ArrayList<Entity_>();
        for (var entity : entityList) {
            var list = listFunction.apply(entity);
            assignedValueSet.addAll(list);
            if (!list.isEmpty()) {
                assignedEntityList.add(entity);
            }
        }
        var moveType = unassigning ? random.nextInt(9) : 3 + random.nextInt(6);
        if (moveType == 0 || assignedEntityList.isEmpty()) {
            var unassignedValueList = valueList.stream().filter(value -> !assignedValueSet.contains(value)).toList();
            if (unassignedValueList.isEmpty()) {
                return null;
            }
            var entity = entityList.get(random.nextInt(entityList.size()));
            return Moves.assign(listVariableMetaModel, unassignedValueList.get(random.nextInt(unassignedValueList.size())),
                    entity, random.nextInt(listFunction.apply(entity).size() + 1));
        }
        var source = assignedEntityList.get(random.nextInt(assignedEntityList.size()));
        var sourceSize = listFunction.apply(source).size();
        var sourceIndex = random.nextInt(sourceSize);
        var sourceToIndex = sourceIndex + 1 + random.nextInt(sourceSize - sourceIndex);
        var destination = entityList.get(random.nextInt(entityList.size()));
        var destinationSize = listFunction.apply(destination).size();
        var other = assignedEntityList.get(random.nextInt(assignedEntityList.size()));
        var otherSize = listFunction.apply(other).size();
        var otherIndex = random.nextInt(otherSize);
        var otherToIndex = otherIndex + 1 + random.nextInt(otherSize - otherIndex);
        return switch (moveType) {
            case 1 -> Moves.unassign(listVariableMetaModel, source, sourceIndex);
            case 2 -> Moves.unassign(listVariableMetaModel, new Range<>(source, sourceIndex, sourceToIndex));
            case 3 -> {
                var destinationIndex = random.nextInt(destination == source ? sourceSize : destinationSize + 1);
                yield destination == source && destinationIndex == sourceIndex ? null
                        : Moves.change(listVariableMetaModel, source, sourceIndex, destination, destinationIndex);
            }
            case 4 -> other == source && otherIndex == sourceIndex ? null
                    : Moves.swap(listVariableMetaModel, source, sourceIndex, other, otherIndex);
            case 5, 6 -> {
                var remainingSize = destination == source ? sourceSize - (sourceToIndex - sourceIndex) : destinationSize;
                yield Moves.change(listVariableMetaModel, new Range<>(source, sourceIndex, sourceToIndex),
                        ElementPosition.of(destination, random.nextInt(remainingSize + 1)), random.nextBoolean());
            }
            case 7 -> other == source ? null
                    : Moves.swap(listVariableMetaModel, new Range<>(source, sourceIndex, sourceToIndex),
                            new Range<>(other, otherIndex, otherToIndex), random.nextBoolean());
            default -> sourceToIndex - sourceIndex < 2 ? null
                    : Moves.reverse(listVariableMetaModel, new Range<>(source, sourceIndex, sourceToIndex));
        };
    }

    private DeclarativeShadowVariableAssertions() {
    }
}
