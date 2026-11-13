package ai.timefold.solver.core.testutil;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.Move;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.move.test.MoveTestContext;
import ai.timefold.solver.core.preview.api.neighborhood.test.NeighborhoodTestContext;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.pinned.TestdataListPinnedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.pinned.TestdataListPinnedEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.subset.TestdataListSubsetRangeEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.subset.TestdataListSubsetRangeSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.pinned.TestdataListUnassignedPinnedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.pinned.TestdataListUnassignedPinnedEntityProvidingSolution;

/**
 * Checks the moves a provider draws against the rules, without a list of the expected moves.
 */
public final class MoveProviderAssertions {

    /**
     * Checks that each expected key occurs, that no other key occurs,
     * and that each count is within the tolerance of the mean count.
     */
    public static <Move_> void assertEquallyOften(Collection<Move_> moves, Function<Move_, Object> keyFunction,
            Collection<?> expectedKeys, double tolerance) {
        var countMap = moves.stream().collect(Collectors.groupingBy(keyFunction, Collectors.counting()));
        assertThat(countMap.keySet()).containsExactlyInAnyOrderElementsOf(new ArrayList<Object>(expectedKeys));
        var mean = moves.size() / (double) countMap.size();
        for (var entry : countMap.entrySet()) {
            assertThat((double) entry.getValue())
                    .as("Count of (%s) in %s.", entry.getKey(), countMap)
                    .isBetween(mean * (1 - tolerance), mean * (1 + tolerance));
        }
    }

    /**
     * Runs each drawn move temporarily and checks that every list value is in its entity's range,
     * that the list of a fully pinned entity did not change, and that no pinned prefix changed.
     *
     * @return the drawn moves
     */
    public static <Solution_, Entity_, Value_> List<Move<Solution_>> assertDrawnListMovesKeepSolutionValid(
            NeighborhoodTestContext<Solution_> context,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Collection<Entity_> entities,
            Predicate<Entity_> fullyPinned, int drawCount) {
        var moveContext = context.getMoveTestContext();
        var protectedListMap = new HashMap<Entity_, List<Value_>>();
        moveContext.executeTemporarily(view -> {
        }, view -> {
            for (var entity : entities) {
                var list = listOf(view, variableMetaModel, entity);
                var protectedSize = fullyPinned.test(entity) ? list.size()
                        : view.getFirstUnpinnedIndex(variableMetaModel, entity);
                protectedListMap.put(entity, List.copyOf(list.subList(0, protectedSize)));
            }
        });
        // An iterator may end early, as a step does; each new stream starts a new iterator.
        var moveList = new ArrayList<Move<Solution_>>(drawCount);
        while (moveList.size() < drawCount) {
            var batch = context.getMovesAsStream().limit(drawCount - moveList.size()).toList();
            assertThat(batch).as("The provider produced no move.").isNotEmpty();
            moveList.addAll(batch);
        }
        for (var move : moveList) {
            try {
                executeAndCheck(moveContext, move, variableMetaModel, entities, fullyPinned, protectedListMap);
            } catch (IllegalStateException e) {
                throw new AssertionError("Move (%s) failed.".formatted(move), e);
            }
        }
        return moveList;
    }

    private static <Solution_, Entity_, Value_> void executeAndCheck(MoveTestContext<Solution_> moveContext,
            Move<Solution_> move, PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel,
            Collection<Entity_> entities, Predicate<Entity_> fullyPinned, Map<Entity_, List<Value_>> protectedListMap) {
        {
            moveContext.executeTemporarily(move, view -> {
                for (var entity : entities) {
                    var list = listOf(view, variableMetaModel, entity);
                    var protectedList = protectedListMap.get(entity);
                    assertThat(list)
                            .as("Entity (%s) after move (%s).", entity, move)
                            .allMatch(value -> view.isValueInRange(variableMetaModel, entity, value))
                            .hasSizeGreaterThanOrEqualTo(protectedList.size());
                    assertThat(list.subList(0, protectedList.size()))
                            .as("Pinned prefix of entity (%s) after move (%s).", entity, move)
                            .containsExactlyElementsOf(protectedList);
                    if (fullyPinned.test(entity)) {
                        assertThat(list).as("Pinned entity (%s) after move (%s).", entity, move)
                                .hasSameSizeAs(protectedList);
                    }
                }
            });
        }
    }

    private static <Solution_, Entity_, Value_> List<Value_> listOf(SolutionView<Solution_> view,
            PlanningListVariableMetaModel<Solution_, Entity_, Value_> variableMetaModel, Entity_ entity) {
        return IntStream.range(0, view.countValues(variableMetaModel, entity))
                .mapToObj(index -> view.getValueAtIndex(variableMetaModel, entity, index))
                .toList();
    }

    @FunctionalInterface
    interface ListEntityFactory<Entity_> {

        Entity_ create(String code, List<TestdataValue> valueRange, List<TestdataValue> valueList, boolean pinned,
                int pinIndex);

    }

    /**
     * Values v0 to v15, entity value ranges, one fully pinned entity (e0),
     * one entity pinned up to index 2 (e1), one entity pinned up to its size (e4) and two free entities.
     * With unassigned values, v14 and v15 are unassigned and e1 and e2 accept them; otherwise they are not used.
     */
    private static <Entity_> List<Entity_> listEntityRangeEntities(List<TestdataValue> values, boolean withUnassignedValue,
            ListEntityFactory<Entity_> factory) {
        Function<int[], List<TestdataValue>> pick = indices -> Arrays.stream(indices).mapToObj(values::get).toList();
        var unassigned = withUnassignedValue ? new int[] { 14, 15 } : new int[0];
        return List.of(
                factory.create("e0", pick.apply(new int[] { 0, 1, 2, 3, 4, 5 }), pick.apply(new int[] { 0, 1 }), true, 0),
                factory.create("e1", pick.apply(concat(new int[] { 3, 4, 5, 6, 7, 8 }, unassigned)),
                        pick.apply(new int[] { 3, 4, 5 }), false, 2),
                factory.create("e2", pick.apply(concat(new int[] { 5, 6, 7, 8, 9, 10, 11 }, unassigned)),
                        pick.apply(new int[] { 6, 7, 8 }), false, 0),
                factory.create("e3", pick.apply(new int[] { 0, 1, 2, 6, 9, 10, 11 }), pick.apply(new int[] { 9, 10, 11, 2 }),
                        false, 0),
                factory.create("e4", pick.apply(new int[] { 12, 13, 6, 7, 8 }), pick.apply(new int[] { 12, 13 }), false, 2));
    }

    public static TestdataListPinnedEntityProvidingSolution listPinnedSolution(List<TestdataValue> values) {
        var solution = new TestdataListPinnedEntityProvidingSolution();
        solution.setEntityList(listEntityRangeEntities(values, false, (code, range, list, pinned, pinIndex) -> {
            var entity = new TestdataListPinnedEntityProvidingEntity(code, range);
            entity.setValueList(new ArrayList<>(list));
            entity.setPinned(pinned);
            entity.setPinIndex(pinIndex);
            return entity;
        }));
        return solution;
    }

    public static TestdataListUnassignedPinnedEntityProvidingSolution listUnassignedPinnedSolution(List<TestdataValue> values) {
        var solution = new TestdataListUnassignedPinnedEntityProvidingSolution();
        solution.setEntityList(listEntityRangeEntities(values, true, (code, range, list, pinned, pinIndex) -> {
            var entity = new TestdataListUnassignedPinnedEntityProvidingEntity(code, range);
            entity.setValueList(new ArrayList<>(list));
            entity.setPinned(pinned);
            entity.setPinIndex(pinIndex);
            return entity;
        }));
        return solution;
    }

    /**
     * A solution value range: entity 0 is fully pinned, entity 1 is pinned up to index 1.
     */
    public static TestdataPinnedWithIndexListSolution listSolutionRangeSolution() {
        var solution = TestdataPinnedWithIndexListSolution.generateInitializedSolution(12, 4);
        solution.getEntityList().get(0).setPinned(true);
        solution.getEntityList().get(1).setPinIndex(1);
        return solution;
    }

    /**
     * e1 holds v1, e2 holds v2, v3 and v4 are in the value range and unassigned,
     * and vOut is a problem fact of the same class outside the value range.
     */
    public static TestdataListSubsetRangeSolution listSubsetRangeSolution() {
        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        var v4 = new TestdataValue("v4");
        var vOut = new TestdataValue("vOut");
        return new TestdataListSubsetRangeSolution(List.of(v1, v2, v3, v4, vOut), List.of(v1, v2, v3, v4),
                List.of(new TestdataListSubsetRangeEntity("e1", v1), new TestdataListSubsetRangeEntity("e2", v2)));
    }

    public static List<TestdataValue> listValues() {
        return IntStream.range(0, 16).mapToObj(i -> new TestdataValue("v" + i)).toList();
    }

    private static int[] concat(int[] left, int[] right) {
        return IntStream.concat(Arrays.stream(left), Arrays.stream(right)).toArray();
    }

    /**
     * One entity holds 10 values which only it and one empty entity accept; 300 entities hold one value of their own.
     * Valid moves are rare per draw, but they exist.
     */
    public static TestdataListPinnedEntityProvidingSolution sparseSolution() {
        var values = IntStream.range(0, 10).mapToObj(i -> new TestdataValue("v" + i)).toList();
        var entities = new ArrayList<TestdataListPinnedEntityProvidingEntity>();
        var full = new TestdataListPinnedEntityProvidingEntity("full", values);
        full.setValueList(new ArrayList<>(values));
        entities.add(full);
        entities.add(new TestdataListPinnedEntityProvidingEntity("empty", values));
        for (var i = 0; i < 300; i++) {
            var own = new TestdataValue("x" + i);
            var entity = new TestdataListPinnedEntityProvidingEntity("e" + i, List.of(own));
            entity.setValueList(new ArrayList<>(List.of(own)));
            entities.add(entity);
        }
        var solution = new TestdataListPinnedEntityProvidingSolution();
        solution.setEntityList(entities);
        return solution;
    }

    /**
     * @return how many moves one iterator gives, up to the limit, before it reports no more
     */
    public static <Solution_> int movesBeforeEnd(NeighborhoodTestContext<Solution_> context, int limit) {
        var iterator = context.getMovesAsIterator();
        var count = 0;
        while (count < limit && iterator.hasNext()) {
            iterator.next();
            count++;
        }
        return count;
    }

    private MoveProviderAssertions() {
    }

}
