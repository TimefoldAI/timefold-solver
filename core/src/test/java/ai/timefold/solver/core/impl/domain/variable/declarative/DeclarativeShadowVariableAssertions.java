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
     * The kinds of list move drawn, each one the move the named provider produces.
     * The providers are not used themselves, since the ones that unassign refuse a list variable
     * that does not allow unassigned values, which ruin and recreate does anyway.
     */
    private enum ListMoveType {
        /** {@code ListAssignMoveProvider}: an unassigned value goes to any position of any list. */
        ASSIGN,
        /** {@code ListUnassignMoveProvider}: one value leaves its list. */
        UNASSIGN_VALUE,
        /** {@code SubListUnassignMoveProvider}: several consecutive values leave their list under one event. */
        UNASSIGN_SUB_LIST,
        /** {@code ListChangeMoveProvider}: one value moves to another position, in the same list or another. */
        CHANGE,
        /** {@code ListSwapMoveProvider}: two values exchange their positions. */
        SWAP,
        /** {@code SubListChangeMoveProvider}: a sub list moves to another position, possibly reversed. */
        SUB_LIST_CHANGE,
        /** {@code SubListSwapMoveProvider}: two sub lists of two different lists exchange places, possibly reversed. */
        SUB_LIST_SWAP,
        /** {@code TwoOptListMoveProvider}: a sub list is reversed in place. */
        REVERSE
    }

    private static final List<ListMoveType> NON_UNASSIGNING_MOVE_TYPE_LIST = List.of(ListMoveType.CHANGE, ListMoveType.SWAP,
            ListMoveType.SUB_LIST_CHANGE, ListMoveType.SUB_LIST_SWAP, ListMoveType.REVERSE);

    /**
     * A random sub list of an assigned entity's list: the values from fromIndex (inclusive) to toIndex (exclusive).
     */
    private record SubList<Entity_>(Entity_ entity, int fromIndex, int toIndex, int listSize) {
        Range<Entity_> range() {
            return new Range<>(entity, fromIndex, toIndex);
        }

        int length() {
            return toIndex - fromIndex;
        }
    }

    /**
     * Returns a random list move of one of the {@link ListMoveType}s,
     * excluding those that unassign values unless unassigning.
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
        var moveTypeList = unassigning ? List.of(ListMoveType.values()) : NON_UNASSIGNING_MOVE_TYPE_LIST;
        var moveType = assignedEntityList.isEmpty() ? ListMoveType.ASSIGN
                : moveTypeList.get(random.nextInt(moveTypeList.size()));
        if (moveType == ListMoveType.ASSIGN) {
            var unassignedValueList = valueList.stream().filter(value -> !assignedValueSet.contains(value)).toList();
            if (unassignedValueList.isEmpty()) {
                return null;
            }
            var entity = entityList.get(random.nextInt(entityList.size()));
            return Moves.assign(listVariableMetaModel, unassignedValueList.get(random.nextInt(unassignedValueList.size())),
                    entity, random.nextInt(listFunction.apply(entity).size() + 1));
        }
        var source = randomSubList(assignedEntityList, listFunction, random);
        var destination = entityList.get(random.nextInt(entityList.size()));
        var destinationSize = listFunction.apply(destination).size();
        var other = randomSubList(assignedEntityList, listFunction, random);
        return switch (moveType) {
            case UNASSIGN_VALUE -> Moves.unassign(listVariableMetaModel, source.entity(), source.fromIndex());
            case UNASSIGN_SUB_LIST -> Moves.unassign(listVariableMetaModel, source.range());
            case CHANGE -> {
                var destinationIndex =
                        random.nextInt(destination == source.entity() ? source.listSize() : destinationSize + 1);
                var unchanged = destination == source.entity() && destinationIndex == source.fromIndex();
                yield unchanged ? null
                        : Moves.change(listVariableMetaModel, source.entity(), source.fromIndex(), destination,
                                destinationIndex);
            }
            case SWAP -> source.entity() == other.entity() && source.fromIndex() == other.fromIndex() ? null
                    : Moves.swap(listVariableMetaModel, source.entity(), source.fromIndex(), other.entity(),
                            other.fromIndex());
            case SUB_LIST_CHANGE -> {
                // The destination index counts the values left once the sub list has been removed.
                var remainingSize = destination == source.entity() ? source.listSize() - source.length() : destinationSize;
                yield Moves.change(listVariableMetaModel, source.range(),
                        ElementPosition.of(destination, random.nextInt(remainingSize + 1)), random.nextBoolean());
            }
            case SUB_LIST_SWAP -> source.entity() == other.entity() ? null
                    : Moves.swap(listVariableMetaModel, source.range(), other.range(), random.nextBoolean());
            case REVERSE -> source.length() < 2 ? null : Moves.reverse(listVariableMetaModel, source.range());
            case ASSIGN -> throw new IllegalStateException("Impossible state: ASSIGN is handled above.");
        };
    }

    private static <Entity_, Value_> SubList<Entity_> randomSubList(List<Entity_> assignedEntityList,
            Function<Entity_, List<Value_>> listFunction, Random random) {
        var entity = assignedEntityList.get(random.nextInt(assignedEntityList.size()));
        var listSize = listFunction.apply(entity).size();
        var fromIndex = random.nextInt(listSize);
        return new SubList<>(entity, fromIndex, fromIndex + 1 + random.nextInt(listSize - fromIndex), listSize);
    }

    private DeclarativeShadowVariableAssertions() {
    }
}
