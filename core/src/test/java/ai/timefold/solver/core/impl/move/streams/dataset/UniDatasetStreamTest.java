package ai.timefold.solver.core.impl.move.streams.dataset;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.move.streams.dataset.uni.AbstractUniDataStream;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
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
                .forEachNonDiscriminating(TestdataEntity.class, false))
                .createDataset();

        var solution = TestdataSolution.generateSolution(2, 2);
        try (var datasetSession = UniDatasetStreamTest.createSession(dataStreamFactory, solution)) {
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
    void forEachBasicVariableIncludingNull() {
        var dataStreamFactory = new DataStreamFactory<>(TestdataSolution.buildSolutionDescriptor());
        var uniDataset = ((AbstractUniDataStream<TestdataSolution, TestdataEntity>) dataStreamFactory
                .forEachNonDiscriminating(TestdataEntity.class, true))
                .createDataset();

        var solution = TestdataSolution.generateSolution(2, 2);
        try (var datasetSession = UniDatasetStreamTest.createSession(dataStreamFactory, solution)) {
            var uniDatasetInstance = datasetSession.getInstance(uniDataset);

            var entity1 = solution.getEntityList().get(0);
            var entity2 = solution.getEntityList().get(1);

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactly(null, entity1, entity2);

            // Make incremental changes.
            var entity3 = new TestdataEntity("entity3", solution.getValueList().get(0));
            datasetSession.insert(entity3);
            datasetSession.retract(entity2);
            datasetSession.settle();

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactly(null, entity1, entity3);
        }
    }

    @Test
    void forEachListVariable() {
        var dataStreamFactory = new DataStreamFactory<>(TestdataListSolution.buildSolutionDescriptor());
        var uniDataset = ((AbstractUniDataStream<TestdataListSolution, TestdataListEntity>) dataStreamFactory
                .forEachNonDiscriminating(TestdataListEntity.class, false))
                .createDataset();

        var solution = TestdataListSolution.generateInitializedSolution(2, 2);
        try (var datasetSession = createSession(dataStreamFactory, solution)) {
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

    @Test
    void forEachListVariableIncludingNull() {
        var dataStreamFactory = new DataStreamFactory<>(TestdataListSolution.buildSolutionDescriptor());
        var uniDataset = ((AbstractUniDataStream<TestdataListSolution, TestdataListEntity>) dataStreamFactory
                .forEachNonDiscriminating(TestdataListEntity.class, true))
                .createDataset();

        var solution = TestdataListSolution.generateInitializedSolution(2, 2);
        try (var datasetSession = createSession(dataStreamFactory, solution)) {
            var uniDatasetInstance = datasetSession.getInstance(uniDataset);

            var entity1 = solution.getEntityList().get(0);
            var entity2 = solution.getEntityList().get(1);

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactly(null, entity1, entity2);

            // Make incremental changes.
            var entity3 = new TestdataListEntity("entity3");
            datasetSession.insert(entity3);
            datasetSession.retract(entity2);
            datasetSession.settle();

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactly(null, entity1, entity3);
        }
    }

    private static <Solution_> DatasetSession<Solution_> createSession(DataStreamFactory<Solution_> dataStreamFactory,
            Solution_ solution) {
        var scoreDirector = new EasyScoreDirectorFactory<>(dataStreamFactory.getSolutionDescriptor(), s -> SimpleScore.ZERO)
                .buildScoreDirector();
        scoreDirector.setWorkingSolution(solution);
        var sessionContext = new SessionContext<>(scoreDirector);
        var datasetSessionFactory = new DatasetSessionFactory<>(dataStreamFactory);
        var datasetSession = datasetSessionFactory.buildSession(sessionContext);
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
                        .forEachNonDiscriminating(TestdataPinnedWithIndexListEntity.class, false))
                        .createDataset();

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

        try (var datasetSession = createSession(dataStreamFactory, solution)) {
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
    void forEachListVariableIncludingPinnedAndNull() {
        var dataStreamFactory = new DataStreamFactory<>(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        var uniDataset =
                ((AbstractUniDataStream<TestdataPinnedWithIndexListSolution, TestdataPinnedWithIndexListEntity>) dataStreamFactory
                        .forEachNonDiscriminating(TestdataPinnedWithIndexListEntity.class, true))
                        .createDataset();

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

        try (var datasetSession = createSession(dataStreamFactory, solution)) {
            var uniDatasetInstance = datasetSession.getInstance(uniDataset);

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactly(null, fullyPinnedEntity, partiallyPinnedEntity, unpinnedEntity);

            // Make incremental changes.
            var entity4 = new TestdataPinnedWithIndexListEntity("entity4");
            entity4.setPinned(true);
            datasetSession.insert(entity4);
            datasetSession.retract(partiallyPinnedEntity);
            datasetSession.settle();

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactly(null, fullyPinnedEntity, unpinnedEntity, entity4);
        }
    }

    @Test
    void forEachListVariableExcludingPinned() { // Entities with planningPin true will be skipped.
        var dataStreamFactory = new DataStreamFactory<>(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        var uniDataset =
                ((AbstractUniDataStream<TestdataPinnedWithIndexListSolution, TestdataPinnedWithIndexListEntity>) dataStreamFactory
                        .forEachExcludingPinned(TestdataPinnedWithIndexListEntity.class, false))
                        .createDataset();

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

        try (var datasetSession = createSession(dataStreamFactory, solution)) {
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
    void forEachListVariableExcludingPinnedIncludingNull() { // Entities with planningPin true will be skipped.
        var dataStreamFactory = new DataStreamFactory<>(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        var uniDataset =
                ((AbstractUniDataStream<TestdataPinnedWithIndexListSolution, TestdataPinnedWithIndexListEntity>) dataStreamFactory
                        .forEachExcludingPinned(TestdataPinnedWithIndexListEntity.class, true))
                        .createDataset();

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

        try (var datasetSession = createSession(dataStreamFactory, solution)) {
            var uniDatasetInstance = datasetSession.getInstance(uniDataset);

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactly(null, partiallyPinnedEntity, unpinnedEntity);

            // Make incremental changes.
            var entity4 = new TestdataPinnedWithIndexListEntity("entity4");
            entity4.setPinned(true);
            datasetSession.insert(entity4);
            datasetSession.retract(partiallyPinnedEntity);
            datasetSession.settle();

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactly(null, unpinnedEntity);
        }
    }

    @Test
    void forEachListVariableIncludingPinnedValues() {
        var dataStreamFactory = new DataStreamFactory<>(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        var uniDataset =
                ((AbstractUniDataStream<TestdataPinnedWithIndexListSolution, TestdataPinnedWithIndexListValue>) dataStreamFactory
                        .forEachNonDiscriminating(TestdataPinnedWithIndexListValue.class, false))
                        .createDataset();

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
        // Properly set shadow variables based on the changes above.
        solution.getEntityList().forEach(TestdataPinnedWithIndexListEntity::setUpShadowVariables);

        try (var datasetSession = createSession(dataStreamFactory, solution)) {
            var uniDatasetInstance = datasetSession.getInstance(uniDataset);

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactly(value1, value2, value3, value4, unassignedValue);
        }
    }

    @Test
    void forEachListVariableIncludingPinnedValuesAndNull() {
        var dataStreamFactory = new DataStreamFactory<>(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor());
        var uniDataset =
                ((AbstractUniDataStream<TestdataPinnedWithIndexListSolution, TestdataPinnedWithIndexListValue>) dataStreamFactory
                        .forEachNonDiscriminating(TestdataPinnedWithIndexListValue.class, true))
                        .createDataset();

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
        // Properly set shadow variables based on the changes above.
        solution.getEntityList().forEach(TestdataPinnedWithIndexListEntity::setUpShadowVariables);

        try (var datasetSession = createSession(dataStreamFactory, solution)) {
            var uniDatasetInstance = datasetSession.getInstance(uniDataset);

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactly(null, value1, value2, value3, value4, unassignedValue);
        }
    }

    @Test
    void forEachListVariableExcludingPinnedValues() {
        var solutionDescriptor = TestdataPinnedWithIndexListSolution.buildSolutionDescriptor();
        var dataStreamFactory = new DataStreamFactory<>(solutionDescriptor);
        var uniDataset =
                ((AbstractUniDataStream<TestdataPinnedWithIndexListSolution, TestdataPinnedWithIndexListValue>) dataStreamFactory
                        .forEachExcludingPinned(TestdataPinnedWithIndexListValue.class, false))
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
        // Properly set shadow variables based on the changes above.
        solution.getEntityList().forEach(TestdataPinnedWithIndexListEntity::setUpShadowVariables);

        try (var datasetSession = createSession(dataStreamFactory, solution)) {
            var uniDatasetInstance = datasetSession.getInstance(uniDataset);

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactly(value2, value3, value4);
        }
    }

    @Test
    void forEachListVariableExcludingPinnedValuesIncludingNull() {
        var solutionDescriptor = TestdataPinnedWithIndexListSolution.buildSolutionDescriptor();
        var dataStreamFactory = new DataStreamFactory<>(solutionDescriptor);
        var uniDataset =
                ((AbstractUniDataStream<TestdataPinnedWithIndexListSolution, TestdataPinnedWithIndexListValue>) dataStreamFactory
                        .forEachExcludingPinned(TestdataPinnedWithIndexListValue.class, true))
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
        // 1 value, not pinned.
        partiallyPinnedEntity.setValueList(List.of(value1, value2));
        var unpinnedEntity = solution.getEntityList().get(2);
        unpinnedEntity.setPinned(false);
        unpinnedEntity.setPlanningPinToIndex(0);
        unpinnedEntity.setValueList(List.of(value3));
        // Fully pinned, but not initially present in the solution.
        var entityAddedLater = new TestdataPinnedWithIndexListEntity("entity4", value4);
        entityAddedLater.setPinned(true);
        // Properly set shadow variables based on the changes above.
        solution.getEntityList().forEach(TestdataPinnedWithIndexListEntity::setUpShadowVariables);

        try (var datasetSession = createSession(dataStreamFactory, solution)) {
            var uniDatasetInstance = datasetSession.getInstance(uniDataset);

            assertThat(uniDatasetInstance.iterator())
                    .toIterable()
                    .map(t -> t.factA)
                    .containsExactly(null, value2, value3, value4);
        }
    }

}