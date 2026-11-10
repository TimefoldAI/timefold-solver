package ai.timefold.solver.core.preview.api.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTestContext;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.pinned.TestdataPinnedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.pinned.TestdataPinnedEntityProvidingSolution;

/**
 * Checks the moves a provider draws against the rules, without a list of the expected moves.
 */
final class MoveProviderAssertions {

    /**
     * Runs each drawn move temporarily and checks that every value is in its entity's range
     * and that no pinned entity changed.
     *
     * @return the drawn moves
     */
    static <Solution_, Entity_> List<Move<Solution_>> assertDrawnMovesKeepSolutionValid(
            NeighborhoodTestContext<Solution_> context,
            List<? extends PlanningVariableMetaModel<Solution_, Entity_, ?>> variableMetaModelList,
            Collection<Entity_> entities, int drawCount) {
        var moveContext = context.getMoveTestContext();
        var pinnedValueMap = new HashMap<List<Object>, Object>();
        moveContext.executeTemporarily(view -> {
        }, view -> {
            for (var variableMetaModel : variableMetaModelList) {
                for (var entity : entities) {
                    if (view.isPinned(variableMetaModel, entity)) {
                        pinnedValueMap.put(List.of(variableMetaModel, entity),
                                new ValueHolder(view.getValue(variableMetaModel, entity)));
                    }
                }
            }
        });
        var moveList = context.getMovesAsStream().limit(drawCount).toList();
        assertThat(moveList).hasSize(drawCount);
        for (var move : moveList) {
            moveContext.executeTemporarily(move, view -> {
                for (var variableMetaModel : variableMetaModelList) {
                    for (var entity : entities) {
                        var pinnedValue = pinnedValueMap.get(List.of(variableMetaModel, entity));
                        if (pinnedValue != null) {
                            assertThat(new ValueHolder(view.getValue(variableMetaModel, entity)))
                                    .as("Pinned entity (%s) changed by move (%s).", entity, move)
                                    .isEqualTo(pinnedValue);
                        } else {
                            assertThat(isValid(view, variableMetaModel, entity))
                                    .as("Entity (%s) has a value outside its range after move (%s).", entity, move)
                                    .isTrue();
                        }
                    }
                }
            });
        }
        return moveList;
    }

    private static <Solution_, Entity_, Value_> boolean isValid(SolutionView<Solution_> view,
            PlanningVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity) {
        var value = view.getValue(variableMetaModel, entity);
        if (value == null) {
            return variableMetaModel.allowsUnassigned();
        }
        return view.isValueInRange(variableMetaModel, entity, value);
    }

    /**
     * Checks that each expected key occurs, that no other key occurs,
     * and that each count is within the tolerance of the mean count.
     */
    static <Move_> void assertEquallyOften(Collection<Move_> moves, Function<Move_, Object> keyFunction,
            Collection<?> expectedKeys, double tolerance) {
        var countMap = moves.stream().collect(Collectors.groupingBy(keyFunction, Collectors.counting()));
        assertThat(countMap.keySet()).containsExactlyInAnyOrderElementsOf(new ArrayList<Object>(expectedKeys));
        var mean = moves.size() / (double) countMap.size();
        for (Map.Entry<Object, Long> entry : countMap.entrySet()) {
            assertThat((double) entry.getValue())
                    .as("Count of (%s) in %s.", entry.getKey(), countMap)
                    .isBetween(mean * (1 - tolerance), mean * (1 + tolerance));
        }
    }

    /**
     * Entity i holds value {@code i % valueCount} and accepts {@code rangeWidth} values from there on;
     * every sixth entity, from the first, is pinned.
     * Entities with the same value form pillars.
     */
    static TestdataPinnedEntityProvidingSolution pinnedEntityRangeSolution(int valueCount, int entityCount,
            int rangeWidth) {
        var valueList = IntStream.range(0, valueCount).mapToObj(i -> new TestdataValue("v" + i)).toList();
        var entityList = IntStream.range(0, entityCount)
                .mapToObj(i -> new TestdataPinnedEntityProvidingEntity("e" + i,
                        IntStream.range(0, rangeWidth).mapToObj(k -> valueList.get((i + k) % valueCount)).toList(),
                        valueList.get(i % valueCount), i % 6 == 0))
                .toList();
        return new TestdataPinnedEntityProvidingSolution("s", entityList);
    }

    // Wraps a value, so that null can be stored in a map.
    private record ValueHolder(Object value) {
    }

    private MoveProviderAssertions() {
    }

}
