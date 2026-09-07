package ai.timefold.solver.core.impl.move;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirector;
import ai.timefold.solver.core.impl.score.director.stream.BavetConstraintStreamScoreDirectorFactory;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.testdomain.list.shadowhistory.TestdataListEntityWithShadowHistory;
import ai.timefold.solver.core.testdomain.list.shadowhistory.TestdataListSolutionWithShadowHistory;
import ai.timefold.solver.core.testdomain.list.shadowhistory.TestdataListValueWithShadowHistory;
import ai.timefold.solver.core.testdomain.list.shadowhistory.TestdataListWithShadowHistoryConstraintProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * Drives every {@link MoveDirector} list operation whose bracket shape puts the undo merge under pressure -
 * overlapping ranges on one entity, two entities pending at once,
 * brackets that change the list's length, and reversed content -
 * then undoes it and checks the solution is exactly as it was.
 * <p>
 * The merge makes undo fire a single {@code afterListVariableChanged} per bracket where it used to fire two,
 * so anything that depended on the notification that is no longer sent shows up here as a stale shadow variable,
 * and any wrongly restored range shows up as a corrupted score.
 */
class MoveDirectorUndoShadowVariableTest {

    // One descriptor instance for both the meta model and the score director:
    // the score director rejects a variable descriptor that did not come from its own solution descriptor.
    private static final SolutionDescriptor<TestdataListSolutionWithShadowHistory> SOLUTION_DESCRIPTOR =
            TestdataListSolutionWithShadowHistory.buildSolutionDescriptor();

    private static final PlanningListVariableMetaModel<TestdataListSolutionWithShadowHistory, TestdataListEntityWithShadowHistory, TestdataListValueWithShadowHistory> VARIABLE_META_MODEL =
            SOLUTION_DESCRIPTOR.getMetaModel()
                    .genuineEntity(TestdataListEntityWithShadowHistory.class)
                    .listVariable("valueList", TestdataListValueWithShadowHistory.class);

    /**
     * @param valueCodeLists the value codes each entity's list variable starts with, one inner list per entity
     * @param operation the move operations to apply to those entities
     */
    private record UndoCase(String name, List<List<String>> valueCodeLists,
            BiConsumer<MoveDirector<TestdataListSolutionWithShadowHistory, SimpleScore>, List<TestdataListEntityWithShadowHistory>> operation) {

        @Override
        public String toString() {
            return name;
        }
    }

    private static Stream<UndoCase> undoCases() {
        return Stream.of(
                // massMoveValues records two brackets for one entity when the destination entity is
                // also a source entity: an unassign bracket [0, 3) that collapses to [0, 1), then an
                // assign bracket [1, 1) that expands to [1, 3).
                new UndoCase("massMoveWithinTheSameEntity", List.of(List.of("v1", "v2", "v3", "v4")),
                        (moveDirector, entities) -> {
                            var entity = entities.getFirst();
                            var moved = List.of(entity.getValueList().get(0), entity.getValueList().get(2));
                            moveDirector.massMoveValues(VARIABLE_META_MODEL, moved, ElementPosition.of(entity, 3));
                        }),
                // Two operations in one move, each bracketing BOTH entities, every bracket at index 0.
                new UndoCase("twoOppositeCrossEntityMovesSharingIndexZero",
                        List.of(List.of("v1", "v2", "v3"), List.of("v4")),
                        (moveDirector, entities) -> {
                            moveDirector.moveValuesBetweenLists(VARIABLE_META_MODEL, entities.get(0), 0, 2,
                                    entities.get(1), 0, false);
                            moveDirector.moveValuesBetweenLists(VARIABLE_META_MODEL, entities.get(1), 0, 1,
                                    entities.get(0), 0, false);
                        }),
                // [beforeDest, beforeSrc, afterSrc, afterDest]: two entities pending at once, the source
                // bracket collapsing to empty while the destination bracket keeps its length, plus an
                // unassignment of the replaced value.
                new UndoCase("crossEntityReplaceValue", List.of(List.of("v1", "v2"), List.of("v3")),
                        (moveDirector, entities) -> moveDirector.replaceValue(VARIABLE_META_MODEL, entities.getFirst(), 1,
                                entities.get(1), 0)),
                // One bracket, but the spans inside it have different lengths, so every index between
                // them shifts.
                new UndoCase("swapUnequalSpansWithinOneList", List.of(List.of("v1", "v2", "v3", "v4", "v5")),
                        (moveDirector, entities) -> moveDirector.swapValuesInList(VARIABLE_META_MODEL,
                                entities.getFirst(), 0, 1, 2, 4, false)),
                // Both lists change length, in opposite directions.
                new UndoCase("swapUnequalSpansBetweenTwoLists",
                        List.of(List.of("v1", "v2", "v3"), List.of("v4", "v5")),
                        (moveDirector, entities) -> moveDirector.swapValuesBetweenLists(VARIABLE_META_MODEL,
                                entities.getFirst(), 0, 1, entities.get(1), 0, 2, false)),
                // destinationIndex < fromIndex, reversed: the bracket's fromIndex is the destination, and
                // the restored content differs element by element from the mutated content.
                new UndoCase("reversingBackwardSubListMove", List.of(List.of("v1", "v2", "v3", "v4", "v5")),
                        (moveDirector, entities) -> moveDirector.moveValuesInList(VARIABLE_META_MODEL,
                                entities.getFirst(), 3, 5, 1, true)),
                // Two brackets for one entity with the same fromIndex and the same length, plus the
                // unassign/assign element events to unwind.
                new UndoCase("unassignThenReassignTheSameIndexRange", List.of(List.of("v1", "v2", "v3", "v4")),
                        (moveDirector, entities) -> {
                            var entity = entities.getFirst();
                            var removed = moveDirector.unassignValues(VARIABLE_META_MODEL, entity, 1, 3);
                            moveDirector.assignValuesAndAdd(VARIABLE_META_MODEL, removed, entity, 1);
                        }));
    }

    @ParameterizedTest
    @MethodSource("undoCases")
    void undoRestoresListShadowVariablesAndScore(UndoCase undoCase) {
        var solution = buildSolution(undoCase.valueCodeLists());
        var scoreDirector = buildScoreDirector(solution);
        var expectedListState = listStateOf(solution);
        var expectedShadowState = shadowStateOf(solution);

        var moveDirector = new MoveDirector<>(scoreDirector).ephemeral();
        undoCase.operation().accept(moveDirector, solution.getEntityList());
        moveDirector.close(); // This undoes the move.

        assertSoftly(softly -> {
            softly.assertThat(listStateOf(solution)).isEqualTo(expectedListState);
            softly.assertThat(shadowStateOf(solution)).isEqualTo(expectedShadowState);
        });
        // Score corruption check: the incremental score after undo must equal a from-scratch score.
        scoreDirector.assertWorkingScoreFromScratch(scoreDirector.calculateScore(), undoCase.name());
    }

    private static TestdataListSolutionWithShadowHistory buildSolution(List<List<String>> valueCodeLists) {
        var valueList = new ArrayList<TestdataListValueWithShadowHistory>();
        var entityList = new ArrayList<TestdataListEntityWithShadowHistory>();
        for (var i = 0; i < valueCodeLists.size(); i++) {
            var entityValueList = new ArrayList<TestdataListValueWithShadowHistory>();
            for (var valueCode : valueCodeLists.get(i)) {
                var value = new TestdataListValueWithShadowHistory(valueCode);
                valueList.add(value);
                entityValueList.add(value);
            }
            entityList.add(new TestdataListEntityWithShadowHistory(String.valueOf((char) ('A' + i)), entityValueList));
        }
        var solution = new TestdataListSolutionWithShadowHistory();
        solution.setValueList(valueList);
        solution.setEntityList(entityList);
        return solution;
    }

    private static BavetConstraintStreamScoreDirector<TestdataListSolutionWithShadowHistory, SimpleScore>
            buildScoreDirector(TestdataListSolutionWithShadowHistory solution) {
        SolutionManager.updateShadowVariables(solution);
        var scoreDirectorFactory =
                new BavetConstraintStreamScoreDirectorFactory<TestdataListSolutionWithShadowHistory, SimpleScore>(
                        SOLUTION_DESCRIPTOR, new TestdataListWithShadowHistoryConstraintProvider(),
                        EnvironmentMode.FULL_ASSERT);
        var scoreDirector = new BavetConstraintStreamScoreDirector.Builder<>(scoreDirectorFactory,
                EnvironmentMode.FULL_ASSERT).build();
        scoreDirector.setWorkingSolution(solution);
        scoreDirector.calculateScore();
        return scoreDirector;
    }

    private static Map<String, List<String>> listStateOf(TestdataListSolutionWithShadowHistory solution) {
        var listStateMap = new LinkedHashMap<String, List<String>>();
        for (var entity : solution.getEntityList()) {
            listStateMap.put(entity.getCode(), entity.getValueList().stream()
                    .map(TestdataListValueWithShadowHistory::getCode)
                    .toList());
        }
        return listStateMap;
    }

    /**
     * The current value of every shadow variable, keyed by value code.
     * The {@code *History} lists are deliberately excluded: they only grow, so they always differ after an undo.
     */
    private static Map<String, String> shadowStateOf(TestdataListSolutionWithShadowHistory solution) {
        var shadowStateMap = new LinkedHashMap<String, String>();
        for (var value : solution.getValueList()) {
            shadowStateMap.put(value.getCode(), "entity=%s index=%s previous=%s next=%s"
                    .formatted(value.getEntity(), value.getIndex(), value.getPrevious(), value.getNext()));
        }
        return shadowStateMap;
    }

}
