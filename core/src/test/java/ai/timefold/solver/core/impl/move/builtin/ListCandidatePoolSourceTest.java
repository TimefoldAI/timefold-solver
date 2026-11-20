package ai.timefold.solver.core.impl.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.dataset.DefaultUniDatasetInstance;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniLeftDatasetInstance;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.SolutionView;
import ai.timefold.solver.core.preview.api.move.test.MoveTester;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.valuerange.pinned.TestdataListPinnedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.pinned.TestdataListPinnedEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.pinned.TestdataListUnassignedPinnedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.pinned.TestdataListUnassignedPinnedEntityProvidingSolution;
import ai.timefold.solver.core.testutil.MoveProviderAssertions;

import org.junit.jupiter.api.Test;

class ListCandidatePoolSourceTest {

    private static final int DRAW_COUNT = 120_000;

    private final List<TestdataValue> values = MoveProviderAssertions.listValues();
    private final TestdataListPinnedEntityProvidingSolution solution = MoveProviderAssertions.listPinnedSolution(values);
    private final List<TestdataListPinnedEntityProvidingEntity> entities = solution.getEntityList();

    // e0 is pinned; e1 is pinned up to index 2; e4 is pinned up to its size.
    private void withPool(
            Consumer<ListCandidatePoolSource<TestdataListPinnedEntityProvidingSolution, TestdataListPinnedEntityProvidingEntity, TestdataValue>> test) {
        var solutionMetaModel = TestdataListPinnedEntityProvidingSolution.buildSolutionDescriptor().getMetaModel();
        PlanningListVariableMetaModel<TestdataListPinnedEntityProvidingSolution, TestdataListPinnedEntityProvidingEntity, TestdataValue> variableMetaModel =
                solutionMetaModel.genuineEntity(TestdataListPinnedEntityProvidingEntity.class).listVariable();
        var movableEntities = entities.stream().filter(entity -> !entity.isPinned()).toList();
        MoveTester.build(solutionMetaModel).using(solution).executeTemporarily(view -> {
        }, (SolutionView<TestdataListPinnedEntityProvidingSolution> view) -> test.accept(new ListCandidatePoolSource<>(view,
                variableMetaModel, new Random(0), movableEntities, null, false)));
    }

    private ElementPosition at(int entityIndex, int index) {
        return ElementPosition.of(entities.get(entityIndex), index);
    }

    @Test
    void singleDestinationDrawIsUniformOverAllUnpinnedPositions() {
        withPool(pool -> {
            var draws = new ArrayList<ElementPosition>();
            for (var i = 0; i < DRAW_COUNT; i++) {
                draws.add(pool.drawDestinationOnce(position -> true));
            }
            MoveProviderAssertions.assertEquallyOften(draws, position -> position,
                    List.of(at(1, 2), at(1, 3), at(2, 0), at(2, 1), at(2, 2), at(2, 3), at(3, 0), at(3, 1), at(3, 2),
                            at(3, 3), at(3, 4), at(4, 2)),
                    0.1);
        });
    }

    @Test
    void singleDestinationDrawFromADatasetWithAGapIsUniformOverAllUnpinnedPositions() {
        // The movable entities come from a dataset whose list has a gap, as after a retract.
        var delegate =
                new UniLeftDatasetInstance<TestdataListPinnedEntityProvidingSolution, TestdataListPinnedEntityProvidingEntity>(
                        mock(AbstractDataset.class), 0);
        var retractedTuple = UniTuple.<TestdataListPinnedEntityProvidingEntity> of(entities.get(2), 1);
        delegate.insert(retractedTuple);
        entities.stream().filter(entity -> !entity.isPinned()).forEach(entity -> delegate.insert(UniTuple.of(entity, 1)));
        delegate.retract(retractedTuple);
        var instance = new DefaultUniDatasetInstance<>(delegate);
        var solutionMetaModel = TestdataListPinnedEntityProvidingSolution.buildSolutionDescriptor().getMetaModel();
        PlanningListVariableMetaModel<TestdataListPinnedEntityProvidingSolution, TestdataListPinnedEntityProvidingEntity, TestdataValue> variableMetaModel =
                solutionMetaModel.genuineEntity(TestdataListPinnedEntityProvidingEntity.class).listVariable();
        MoveTester.build(solutionMetaModel).using(solution).executeTemporarily(view -> {
        }, (SolutionView<TestdataListPinnedEntityProvidingSolution> view) -> {
            var pool = ListCandidatePoolSource.of(view, variableMetaModel, new Random(0), instance, false);
            var draws = new ArrayList<ElementPosition>();
            for (var i = 0; i < DRAW_COUNT; i++) {
                draws.add(pool.drawDestinationOnce(position -> true));
            }
            MoveProviderAssertions.assertEquallyOften(draws, position -> position,
                    List.of(at(1, 2), at(1, 3), at(2, 0), at(2, 1), at(2, 2), at(2, 3), at(3, 0), at(3, 1), at(3, 2),
                            at(3, 3), at(3, 4), at(4, 2)),
                    0.1);
        });
    }

    @Test
    void singlePartnerDrawIsUniformOverAllUnpinnedValues() {
        // e4 has no unpinned value; its count repeats in the prefix sums.
        withPool(pool -> {
            var draws = new ArrayList<TestdataValue>();
            for (var i = 0; i < DRAW_COUNT; i++) {
                draws.add(pool.drawPartnerOnce(value -> true));
            }
            MoveProviderAssertions.assertEquallyOften(draws, value -> value,
                    List.of(values.get(5), values.get(6), values.get(7), values.get(8), values.get(9), values.get(10),
                            values.get(11), values.get(2)),
                    0.1);
        });
    }

    @Test
    void destinationDrawIsUniformOverTheAcceptingEntitiesOnly() {
        // v9 is accepted by e2 and e3 only.
        withPool(pool -> {
            var draws = new ArrayList<ElementPosition>();
            for (var i = 0; i < DRAW_COUNT; i++) {
                draws.add(pool.drawDestination(values.get(9), position -> true));
            }
            MoveProviderAssertions.assertEquallyOften(draws, position -> position,
                    List.of(at(2, 0), at(2, 1), at(2, 2), at(2, 3), at(3, 0), at(3, 1), at(3, 2), at(3, 3), at(3, 4)),
                    0.1);
        });
    }

    @Test
    void partnerDrawIsUniformOverTheValuesOfAcceptingEntitiesOnly() {
        withPool(pool -> {
            var draws = new ArrayList<TestdataValue>();
            for (var i = 0; i < DRAW_COUNT; i++) {
                draws.add(pool.drawPartner(values.get(9), false, value -> true));
            }
            MoveProviderAssertions.assertEquallyOften(draws, value -> value,
                    List.of(values.get(6), values.get(7), values.get(8), values.get(9), values.get(10), values.get(11),
                            values.get(2)),
                    0.1);
        });
    }

    @Test
    void searchingDrawsAreUniformWhenOneListIsMuchLongerThanTheOthers() {
        // e0 holds six values, e1 to e5 none: padding to e0 would be most of the slots, so prefix sums are used.
        var heldValues = IntStream.range(0, 6).mapToObj(i -> new TestdataValue("h" + i)).toList();
        var value = new TestdataValue("x");
        var range = new ArrayList<>(heldValues);
        range.add(value);
        var skewedEntities = IntStream.range(0, 6).mapToObj(i -> {
            var entity = new TestdataListPinnedEntityProvidingEntity("e" + i, range);
            entity.setValueList(new ArrayList<>(i == 0 ? heldValues : List.of()));
            return entity;
        }).toList();
        var skewedSolution = new TestdataListPinnedEntityProvidingSolution();
        skewedSolution.setEntityList(skewedEntities);
        var solutionMetaModel = TestdataListPinnedEntityProvidingSolution.buildSolutionDescriptor().getMetaModel();
        PlanningListVariableMetaModel<TestdataListPinnedEntityProvidingSolution, TestdataListPinnedEntityProvidingEntity, TestdataValue> variableMetaModel =
                solutionMetaModel.genuineEntity(TestdataListPinnedEntityProvidingEntity.class).listVariable();
        MoveTester.build(solutionMetaModel).using(skewedSolution).executeTemporarily(view -> {
        }, (SolutionView<TestdataListPinnedEntityProvidingSolution> view) -> {
            var pool = new ListCandidatePoolSource<>(view, variableMetaModel, new Random(0), skewedEntities, null,
                    false);
            var destinations = new ArrayList<ElementPosition>();
            var partners = new ArrayList<TestdataValue>();
            for (var i = 0; i < DRAW_COUNT; i++) {
                destinations.add(pool.drawDestination(value, position -> true));
                partners.add(pool.drawPartner(value, position -> true));
            }
            var expectedDestinations = new ArrayList<ElementPosition>();
            for (var index = 0; index <= heldValues.size(); index++) {
                expectedDestinations.add(ElementPosition.of(skewedEntities.getFirst(), index));
            }
            for (var entity : skewedEntities.subList(1, skewedEntities.size())) {
                expectedDestinations.add(ElementPosition.of(entity, 0));
            }
            MoveProviderAssertions.assertEquallyOften(destinations, position -> position, expectedDestinations, 0.1);
            MoveProviderAssertions.assertEquallyOften(partners, partner -> partner, heldValues, 0.1);
        });
    }

    @Test
    void drawsGiveUpWhenNothingIsAccepted() {
        withPool(pool -> {
            assertThat(pool.drawDestination(values.get(9), position -> false)).isNull();
            assertThat(pool.drawPartner(values.get(9), false, value -> false)).isNull();
            assertThat(pool.drawDestinationOnce(position -> false)).isNull();
            assertThat(pool.drawPartnerOnce(value -> false)).isNull();
        });
    }

    @Test
    void unassignedPartnersNeedUnassignedValues() {
        withPool(pool -> assertThatNullPointerException()
                .isThrownBy(() -> pool.drawPartner(values.get(9), true, value -> true)));
    }

    /**
     * v14 and v15 are unassigned, and only e1 and e2 accept them; v7 shares a range with more values than are unassigned,
     * so its unassigned partners are drawn from the unassigned-value dataset, which has a gap from a retracted row.
     */
    private void withUnassignedPool(
            Consumer<ListCandidatePoolSource<TestdataListUnassignedPinnedEntityProvidingSolution, TestdataListUnassignedPinnedEntityProvidingEntity, TestdataValue>> test) {
        var unassignedSolution = MoveProviderAssertions.listUnassignedPinnedSolution(values);
        var unassignedDelegate =
                new UniLeftDatasetInstance<TestdataListUnassignedPinnedEntityProvidingSolution, TestdataValue>(
                        mock(AbstractDataset.class), 0);
        var retractedTuple = UniTuple.of(values.get(14), 1);
        unassignedDelegate.insert(retractedTuple);
        unassignedDelegate.insert(UniTuple.of(values.get(14), 1));
        unassignedDelegate.insert(UniTuple.of(values.get(15), 1));
        unassignedDelegate.retract(retractedTuple);
        var unassignedInstance = new DefaultUniDatasetInstance<>(unassignedDelegate);

        var solutionMetaModel = TestdataListUnassignedPinnedEntityProvidingSolution.buildSolutionDescriptor().getMetaModel();
        PlanningListVariableMetaModel<TestdataListUnassignedPinnedEntityProvidingSolution, TestdataListUnassignedPinnedEntityProvidingEntity, TestdataValue> variableMetaModel =
                solutionMetaModel.genuineEntity(TestdataListUnassignedPinnedEntityProvidingEntity.class).listVariable();
        var movableEntities = unassignedSolution.getEntityList().stream().filter(entity -> !entity.isPinned()).toList();
        MoveTester.build(solutionMetaModel).using(unassignedSolution).executeTemporarily(view -> {
        }, (SolutionView<TestdataListUnassignedPinnedEntityProvidingSolution> view) -> test.accept(
                new ListCandidatePoolSource<>(view, variableMetaModel, new Random(0), movableEntities,
                        unassignedInstance, false)));
    }

    @Test
    void partnerDrawIncludesUnassignedValuesFromTheDatasetAtTheSameChance() {
        // v7: e1 has v5 movable, e2 has v6 to v8, e4 is pinned up to its size; v14 and v15 are unassigned.
        withUnassignedPool(pool -> {
            var draws = new ArrayList<TestdataValue>();
            for (var i = 0; i < DRAW_COUNT; i++) {
                draws.add(pool.drawPartner(values.get(7), true, value -> true));
            }
            MoveProviderAssertions.assertEquallyOften(draws, value -> value,
                    List.of(values.get(5), values.get(6), values.get(7), values.get(8), values.get(14), values.get(15)),
                    0.1);
        });
    }

    @Test
    void unassignedPartnersFromTheDatasetSkipItsGaps() {
        var unassignedValues = List.of(values.get(14), values.get(15));
        withUnassignedPool(pool -> {
            var draws = new ArrayList<TestdataValue>();
            for (var i = 0; i < DRAW_COUNT; i++) {
                draws.add(pool.drawPartner(values.get(7), true, unassignedValues::contains));
            }
            MoveProviderAssertions.assertEquallyOften(draws, value -> value, unassignedValues, 0.1);
        });
    }

    @Test
    void buildingThePoolDoesNotMoveTheSlotsHeldByAnotherIterator() {
        // A dataset with a gap, as after a retract; one gap among five does not make the dataset compact itself.
        var delegate =
                new UniLeftDatasetInstance<TestdataListPinnedEntityProvidingSolution, TestdataListPinnedEntityProvidingEntity>(
                        mock(AbstractDataset.class), 0);
        var retractedTuple = UniTuple.<TestdataListPinnedEntityProvidingEntity> of(entities.get(1), 1);
        delegate.insert(retractedTuple);
        for (var entity : entities) {
            delegate.insert(UniTuple.of(entity, 1));
        }
        delegate.retract(retractedTuple);
        var instance = new DefaultUniDatasetInstance<>(delegate);
        var random = new Random(0);
        // Another move iterator of the same step retires a left element, which it remembers by its slot.
        var leftIterator = instance.retiringRandomIterator(random);
        var retiredEntity = leftIterator.next();
        leftIterator.retire();

        var solutionMetaModel = TestdataListPinnedEntityProvidingSolution.buildSolutionDescriptor().getMetaModel();
        PlanningListVariableMetaModel<TestdataListPinnedEntityProvidingSolution, TestdataListPinnedEntityProvidingEntity, TestdataValue> variableMetaModel =
                solutionMetaModel.genuineEntity(TestdataListPinnedEntityProvidingEntity.class).listVariable();
        MoveTester.build(solutionMetaModel).using(solution).executeTemporarily(view -> {
        }, (SolutionView<TestdataListPinnedEntityProvidingSolution> view) -> ListCandidatePoolSource
                // The pool reads the dataset on its first single draw.
                .of(view, variableMetaModel, random, instance, false).drawDestinationOnce(position -> true));

        var drawnEntities = new HashSet<TestdataListPinnedEntityProvidingEntity>();
        for (var i = 0; i < 1_000 && leftIterator.hasNext(); i++) {
            drawnEntities.add(leftIterator.next());
        }
        assertThat(drawnEntities).containsExactlyInAnyOrderElementsOf(
                entities.stream().filter(entity -> entity != retiredEntity).toList());
    }

}
