package ai.timefold.solver.core.impl.move.streams.dataset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.ListVariableStateDemand;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListValue;

import org.junit.jupiter.api.Test;

class UniDatasetStreamTest {

    @Test
    void forEachBasicVariable() {
        var dataStreamFactory = new DataStreamFactory<>(TestdataSolution.buildSolutionDescriptor());
        var uniDataset = ((AbstractUniDataStream<TestdataSolution, TestdataEntity>) dataStreamFactory
                .forEachNonDiscriminating(TestdataEntity.class))
                .createDataset();

        var supplyManager = mock(SupplyManager.class);
        var solution = TestdataSolution.generateSolution(2, 2);
        try (var datasetSession = UniDatasetStreamTest.createSession(dataStreamFactory, solution, supplyManager)) {
            var uniDatasetInstance = datasetSession.getInstance(uniDataset);

            var entity1 = solution.getEntityList().get(0);
            var entity2 = solution.getEntityList().get(1);

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactly(entity1, entity2);

            // Make incremental changes.
            var entity3 = new TestdataEntity("entity3", solution.getValueList().get(0));
            datasetSession.insert(entity3);
            datasetSession.retract(entity2);
            datasetSession.settle();

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactly(entity1, entity3);
        }
    }

    @Test
    void forEachListVariable() {
        var dataStreamFactory = new DataStreamFactory<>(TestdataListSolution.buildSolutionDescriptor());
        var uniDataset = ((AbstractUniDataStream<TestdataListSolution, TestdataListEntity>) dataStreamFactory
                .forEachNonDiscriminating(TestdataListEntity.class))
                .createDataset();

        var supplyManager = mock(SupplyManager.class);
        var solution = TestdataListSolution.generateInitializedSolution(2, 2);
        try (var datasetSession = createSession(dataStreamFactory, solution, supplyManager)) {
            var uniDatasetInstance = datasetSession.getInstance(uniDataset);

            var entity1 = solution.getEntityList().get(0);
            var entity2 = solution.getEntityList().get(1);

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactly(entity1, entity2);

            // Make incremental changes.
            var entity3 = new TestdataListEntity("entity3");
            datasetSession.insert(entity3);
            datasetSession.retract(entity2);
            datasetSession.settle();

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactly(entity1, entity3);
        }
    }

    private static <Solution_> DatasetSession<Solution_> createSession(DataStreamFactory<Solution_> dataStreamFactory,
            Solution_ solution, SupplyManager supplyManager) {
        var datasetSessionFactory = new DatasetSessionFactory<>(dataStreamFactory);
        var datasetSession = datasetSessionFactory.buildSession();
        datasetSession.initialize(solution, supplyManager);

        var solutionDescriptor = dataStreamFactory.getSolutionDescriptor();
        solutionDescriptor.visitAll(solution, datasetSession::insert);

        datasetSession.settle();
        return datasetSession;
    }

    @Test
    void forEachListVariableIncludingPinned() {
        var dataStreamFactory = new DataStreamFactory<>(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        var uniDataset =
                ((AbstractUniDataStream<TestdataPinnedWithIndexListSolution, TestdataPinnedWithIndexListEntity>) dataStreamFactory
                        .forEachNonDiscriminating(TestdataPinnedWithIndexListEntity.class))
                        .createDataset();

        var supplyManager = mock(SupplyManager.class);

        // Prepare the solution;
        var solution = TestdataPinnedWithIndexListSolution.generateInitializedSolution(5, 3);
        // 1 value, entity pinned.
        var fullyPinnedEntity = solution.getEntityList().get(0);
        fullyPinnedEntity.setPinned(true);
        // 2 values, 1 pinned.
        var partiallyPinnedEntity = solution.getEntityList().get(1);
        partiallyPinnedEntity.setPlanningPinToIndex(1);
        // 1 value, not pinned.
        var unpinnedEntity = solution.getEntityList().get(2);
        unpinnedEntity.setPinned(false);
        unpinnedEntity.setPlanningPinToIndex(0);

        try (var datasetSession = createSession(dataStreamFactory, solution, supplyManager)) {
            var uniDatasetInstance = datasetSession.getInstance(uniDataset);

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactly(fullyPinnedEntity, partiallyPinnedEntity, unpinnedEntity);

            // Make incremental changes.
            var entity4 = new TestdataPinnedWithIndexListEntity("entity4");
            entity4.setPinned(true);
            datasetSession.insert(entity4);
            datasetSession.retract(partiallyPinnedEntity);
            datasetSession.settle();

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactly(fullyPinnedEntity, unpinnedEntity, entity4);
        }
    }

    @Test
    void forEachListVariableExcludingPinned() { // Entities with planningPin true will be skipped.
        var dataStreamFactory = new DataStreamFactory<>(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        var uniDataset =
                ((AbstractUniDataStream<TestdataPinnedWithIndexListSolution, TestdataPinnedWithIndexListEntity>) dataStreamFactory
                        .forEachExcludingPinned(TestdataPinnedWithIndexListEntity.class))
                        .createDataset();

        var supplyManager = mock(SupplyManager.class);

        // Prepare the solution;
        var solution = TestdataPinnedWithIndexListSolution.generateInitializedSolution(5, 3);
        // 1 value, entity pinned.
        var fullyPinnedEntity = solution.getEntityList().get(0);
        fullyPinnedEntity.setPinned(true);
        // 2 values, 1 pinned.
        var partiallyPinnedEntity = solution.getEntityList().get(1);
        partiallyPinnedEntity.setPinned(false);
        partiallyPinnedEntity.setPlanningPinToIndex(1);
        // 1 value, not pinned.
        var unpinnedEntity = solution.getEntityList().get(2);
        unpinnedEntity.setPinned(false);
        unpinnedEntity.setPlanningPinToIndex(0);

        try (var datasetSession = createSession(dataStreamFactory, solution, supplyManager)) {
            var uniDatasetInstance = datasetSession.getInstance(uniDataset);

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactly(partiallyPinnedEntity, unpinnedEntity);

            // Make incremental changes.
            var entity4 = new TestdataPinnedWithIndexListEntity("entity4");
            entity4.setPinned(true);
            datasetSession.insert(entity4);
            datasetSession.retract(partiallyPinnedEntity);
            datasetSession.settle();

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactly(unpinnedEntity);
        }
    }

    @Test
    void forEachListVariableIncludingPinnedValues() {
        var dataStreamFactory = new DataStreamFactory<>(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        var uniDataset =
                ((AbstractUniDataStream<TestdataPinnedWithIndexListSolution, TestdataPinnedWithIndexListValue>) dataStreamFactory
                        .forEachNonDiscriminating(TestdataPinnedWithIndexListValue.class))
                        .createDataset();

        var supplyManager = mock(SupplyManager.class);

        // Prepare the solution;
        var solution = TestdataPinnedWithIndexListSolution.generateInitializedSolution(5, 3);
        var value1 = solution.getValueList().get(0);
        var value2 = solution.getValueList().get(1);
        var value3 = solution.getValueList().get(2);
        var value4 = solution.getValueList().get(3);
        var unassignedValue = solution.getValueList().get(4);
        // 1 value, entity pinned.
        var fullyPinnedEntity = solution.getEntityList().get(0);
        fullyPinnedEntity.setPinned(true);
        fullyPinnedEntity.setValueList(List.of(value1));
        // 2 values, 1 pinned.
        var partiallyPinnedEntity = solution.getEntityList().get(1);
        partiallyPinnedEntity.setPlanningPinToIndex(1);
        partiallyPinnedEntity.setValueList(List.of(value2, value3));
        // 1 value, not pinned.
        var unpinnedEntity = solution.getEntityList().get(2);
        unpinnedEntity.setPinned(false);
        unpinnedEntity.setPlanningPinToIndex(0);
        unpinnedEntity.setValueList(List.of(value4));

        try (var datasetSession = createSession(dataStreamFactory, solution, supplyManager)) {
            var uniDatasetInstance = datasetSession.getInstance(uniDataset);

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactly(value1, value2, value3, value4, unassignedValue);

            // Make incremental changes.
            var entity4 = new TestdataPinnedWithIndexListEntity("entity4", unassignedValue);
            datasetSession.insert(entity4); // This will add the value to the dataset.
            datasetSession.retract(partiallyPinnedEntity); // This will remove the pin on value3.
            datasetSession.settle();

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactly(value1, value2, value3, value4, unassignedValue);
        }
    }

    @Test
    void forEachListVariableExcludingPinnedValues() {
        var solutionDescriptor = TestdataPinnedWithIndexListSolution.buildSolutionDescriptor();
        var dataStreamFactory = new DataStreamFactory<>(solutionDescriptor);
        var uniDataset =
                ((AbstractUniDataStream<TestdataPinnedWithIndexListSolution, TestdataPinnedWithIndexListValue>) dataStreamFactory
                        .forEachExcludingPinned(TestdataPinnedWithIndexListValue.class))
                        .createDataset();

        // Prepare the solution;
        var solution = TestdataPinnedWithIndexListSolution.generateInitializedSolution(5, 3);
        var value0 = solution.getValueList().get(0);
        var value1 = solution.getValueList().get(1);
        var value2 = solution.getValueList().get(2);
        var value3 = solution.getValueList().get(3);
        var value4 = solution.getValueList().get(4); // Initially unassigned.
        // 1 value, entity pinned.
        var fullyPinnedEntity = solution.getEntityList().get(0);
        fullyPinnedEntity.setPinned(true);
        fullyPinnedEntity.setValueList(List.of(value0));
        // 2 values, 1 pinned.
        var partiallyPinnedEntity = solution.getEntityList().get(1);
        partiallyPinnedEntity.setPlanningPinToIndex(1);
        partiallyPinnedEntity.setValueList(List.of(value1, value2));
        // 1 value, not pinned.
        var unpinnedEntity = solution.getEntityList().get(2);
        unpinnedEntity.setPinned(false);
        unpinnedEntity.setPlanningPinToIndex(0);
        unpinnedEntity.setValueList(List.of(value3));
        // Fully pinned, but not initially present in the solution.
        var entityAddedLater = new TestdataPinnedWithIndexListEntity("entity4", value4);
        entityAddedLater.setPinned(true);

        // Emulate pinning logic.
        // Otherwise we'd have to mock everything from score director down.
        // We're not testing pin detection; we're testing that the session can ignore values already marked as pinned.
        var supplyManager = mock(SupplyManager.class);
        var listVariableStateSupply = mock(ListVariableStateSupply.class);
        var effectiveEntityList = new ArrayList<>(List.of(fullyPinnedEntity, partiallyPinnedEntity, unpinnedEntity));
        doAnswer(invocation -> {
            var element = (TestdataPinnedWithIndexListValue) invocation.getArgument(0);
            for (var entity : effectiveEntityList) {
                var indexOf = entity.getValueList().indexOf(element);
                if (indexOf < 0) {
                    continue;
                }
                if (entity.isPinned()) {
                    return true;
                } else {
                    var pinToIndex = entity.getPinIndex();
                    return indexOf < pinToIndex;
                }
            }
            return false;
        }).when(listVariableStateSupply).isPinned(any());
        doReturn(listVariableStateSupply).when(supplyManager).demand(any(ListVariableStateDemand.class));

        try (var datasetSession = createSession(dataStreamFactory, solution, supplyManager)) {
            var uniDatasetInstance = datasetSession.getInstance(uniDataset);

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactly(value2, value3, value4);

            // Make incremental changes.
            partiallyPinnedEntity.setPlanningPinToIndex(0);
            effectiveEntityList.add(entityAddedLater);
            effectiveEntityList.remove(fullyPinnedEntity);
            datasetSession.insert(entityAddedLater); // This will add the value to the dataset, but make it pinned.
            datasetSession.retract(fullyPinnedEntity); // This will remove value0.
            datasetSession.update(partiallyPinnedEntity); // This will remove the pin from value1.
            datasetSession.settle();

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactlyInAnyOrder(value0, value1, value2, value3);
        }
    }

}
