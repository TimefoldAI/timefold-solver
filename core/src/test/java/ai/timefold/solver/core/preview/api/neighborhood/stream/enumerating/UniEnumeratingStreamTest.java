package ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.DatasetSession;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.DatasetSessionFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.AbstractUniEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniLeftDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniLeftDatasetInstance;
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

class UniEnumeratingStreamTest {

    @Test
    void forEachBasicVariable() {
        var enumeratingStreamFactory =
                new EnumeratingStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var uniDataset = ((AbstractUniEnumeratingStream<TestdataSolution, TestdataEntity>) enumeratingStreamFactory
                .forEachNonDiscriminating(TestdataEntity.class, false))
                .createLeftDataset();

        var solution = TestdataSolution.generateSolution(2, 2);
        var datasetSession = UniEnumeratingStreamTest.createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getDatasetInstance(datasetSession, uniDataset);

        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(entity1, entity2);

        // Make incremental changes.
        var entity3 = new TestdataEntity("entity3", solution.getValueList().get(0));
        datasetSession.insert(entity3);
        datasetSession.retract(entity2);
        datasetSession.settle();

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(entity1, entity3);
    }

    private static <Solution_, A> UniLeftDatasetInstance<Solution_, A> getDatasetInstance(DatasetSession<Solution_> session,
            UniLeftDataset<Solution_, A> dataset) {
        return (UniLeftDatasetInstance<Solution_, A>) session.getInstance(dataset);
    }

    @Test
    void forEachBasicVariableIncludingNull() {
        var enumeratingStreamFactory =
                new EnumeratingStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var uniDataset = ((AbstractUniEnumeratingStream<TestdataSolution, TestdataEntity>) enumeratingStreamFactory
                .forEachNonDiscriminating(TestdataEntity.class, true))
                .createLeftDataset();

        var solution = TestdataSolution.generateSolution(2, 2);
        var datasetSession = UniEnumeratingStreamTest.createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getDatasetInstance(datasetSession, uniDataset);

        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(null, entity1, entity2);

        // Make incremental changes.
        var entity3 = new TestdataEntity("entity3", solution.getValueList().get(0));
        datasetSession.insert(entity3);
        datasetSession.retract(entity2);
        datasetSession.settle();

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(null, entity1, entity3);
    }

    @Test
    void forEachListVariable() {
        var enumeratingStreamFactory =
                new EnumeratingStreamFactory<>(TestdataListSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var uniDataset = ((AbstractUniEnumeratingStream<TestdataListSolution, TestdataListEntity>) enumeratingStreamFactory
                .forEachNonDiscriminating(TestdataListEntity.class, false))
                .createLeftDataset();

        var solution = TestdataListSolution.generateInitializedSolution(2, 2);
        var datasetSession = createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getDatasetInstance(datasetSession, uniDataset);

        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(entity1, entity2);

        // Make incremental changes.
        var entity3 = new TestdataListEntity("entity3");
        datasetSession.insert(entity3);
        datasetSession.retract(entity2);
        datasetSession.settle();

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(entity1, entity3);
    }

    @Test
    void forEachListVariableIncludingNull() {
        var enumeratingStreamFactory =
                new EnumeratingStreamFactory<>(TestdataListSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var uniDataset = ((AbstractUniEnumeratingStream<TestdataListSolution, TestdataListEntity>) enumeratingStreamFactory
                .forEachNonDiscriminating(TestdataListEntity.class, true))
                .createLeftDataset();

        var solution = TestdataListSolution.generateInitializedSolution(2, 2);
        var datasetSession = createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getDatasetInstance(datasetSession, uniDataset);

        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(null, entity1, entity2);

        // Make incremental changes.
        var entity3 = new TestdataListEntity("entity3");
        datasetSession.insert(entity3);
        datasetSession.retract(entity2);
        datasetSession.settle();

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(null, entity1, entity3);
    }

    private static <Solution_> DatasetSession<Solution_> createSession(
            EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            Solution_ solution) {
        var scoreDirector =
                new EasyScoreDirectorFactory<>(enumeratingStreamFactory.getSolutionDescriptor(), s -> SimpleScore.ZERO)
                        .buildScoreDirector();
        scoreDirector.setWorkingSolution(solution);
        var sessionContext = new SessionContext<>(scoreDirector);
        var datasetSessionFactory = new DatasetSessionFactory<>(enumeratingStreamFactory);
        var datasetSession = datasetSessionFactory.buildSession(sessionContext);
        var solutionDescriptor = enumeratingStreamFactory.getSolutionDescriptor();

        solutionDescriptor.visitAll(solution, datasetSession::insert);

        datasetSession.settle();
        return datasetSession;
    }

    @Test
    void forEachListVariableIncludingPinned() {
        var enumeratingStreamFactory =
                new EnumeratingStreamFactory<>(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor(),
                        EnvironmentMode.PHASE_ASSERT);
        var uniDataset =
                ((AbstractUniEnumeratingStream<TestdataPinnedWithIndexListSolution, TestdataPinnedWithIndexListEntity>) enumeratingStreamFactory
                        .forEachNonDiscriminating(TestdataPinnedWithIndexListEntity.class, false))
                        .createLeftDataset();

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

        var datasetSession = createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getDatasetInstance(datasetSession, uniDataset);

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(fullyPinnedEntity, partiallyPinnedEntity, unpinnedEntity);

        // Make incremental changes.
        var entity4 = new TestdataPinnedWithIndexListEntity("entity4");
        entity4.setPinned(true);
        datasetSession.insert(entity4);
        datasetSession.retract(partiallyPinnedEntity);
        datasetSession.settle();

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(fullyPinnedEntity, unpinnedEntity, entity4);
    }

    @Test
    void forEachListVariableIncludingPinnedAndNull() {
        var enumeratingStreamFactory =
                new EnumeratingStreamFactory<>(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor(),
                        EnvironmentMode.PHASE_ASSERT);
        var uniDataset =
                ((AbstractUniEnumeratingStream<TestdataPinnedWithIndexListSolution, TestdataPinnedWithIndexListEntity>) enumeratingStreamFactory
                        .forEachNonDiscriminating(TestdataPinnedWithIndexListEntity.class, true))
                        .createLeftDataset();

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

        var datasetSession = createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getDatasetInstance(datasetSession, uniDataset);

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(null, fullyPinnedEntity, partiallyPinnedEntity, unpinnedEntity);

        // Make incremental changes.
        var entity4 = new TestdataPinnedWithIndexListEntity("entity4");
        entity4.setPinned(true);
        datasetSession.insert(entity4);
        datasetSession.retract(partiallyPinnedEntity);
        datasetSession.settle();

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(null, fullyPinnedEntity, unpinnedEntity, entity4);
    }

    @Test
    void forEachListVariableExcludingPinned() { // Entities with planningPin true will be skipped.
        var enumeratingStreamFactory =
                new EnumeratingStreamFactory<>(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor(),
                        EnvironmentMode.PHASE_ASSERT);
        var uniDataset =
                ((AbstractUniEnumeratingStream<TestdataPinnedWithIndexListSolution, TestdataPinnedWithIndexListEntity>) enumeratingStreamFactory
                        .forEachExcludingPinned(TestdataPinnedWithIndexListEntity.class, false))
                        .createLeftDataset();

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

        var datasetSession = createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getDatasetInstance(datasetSession, uniDataset);

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(partiallyPinnedEntity, unpinnedEntity);

        // Make incremental changes.
        var entity4 = new TestdataPinnedWithIndexListEntity("entity4");
        entity4.setPinned(true);
        datasetSession.insert(entity4);
        datasetSession.retract(partiallyPinnedEntity);
        datasetSession.settle();

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(unpinnedEntity);
    }

    @Test
    void forEachListVariableExcludingPinnedIncludingNull() { // Entities with planningPin true will be skipped.
        var enumeratingStreamFactory =
                new EnumeratingStreamFactory<>(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor(),
                        EnvironmentMode.PHASE_ASSERT);
        var uniDataset =
                ((AbstractUniEnumeratingStream<TestdataPinnedWithIndexListSolution, TestdataPinnedWithIndexListEntity>) enumeratingStreamFactory
                        .forEachExcludingPinned(TestdataPinnedWithIndexListEntity.class, true))
                        .createLeftDataset();

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

        var datasetSession = createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getDatasetInstance(datasetSession, uniDataset);

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(null, partiallyPinnedEntity, unpinnedEntity);

        // Make incremental changes.
        var entity4 = new TestdataPinnedWithIndexListEntity("entity4");
        entity4.setPinned(true);
        datasetSession.insert(entity4);
        datasetSession.retract(partiallyPinnedEntity);
        datasetSession.settle();

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(null, unpinnedEntity);
    }

    @Test
    void forEachListVariableIncludingPinnedValues() {
        var enumeratingStreamFactory =
                new EnumeratingStreamFactory<>(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor(),
                        EnvironmentMode.PHASE_ASSERT);
        var uniDataset =
                ((AbstractUniEnumeratingStream<TestdataPinnedWithIndexListSolution, TestdataPinnedWithIndexListValue>) enumeratingStreamFactory
                        .forEachNonDiscriminating(TestdataPinnedWithIndexListValue.class, false))
                        .createLeftDataset();

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

        var datasetSession = createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getDatasetInstance(datasetSession, uniDataset);

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(value1, value2, value3, value4, unassignedValue);
    }

    @Test
    void forEachListVariableIncludingPinnedValuesAndNull() {
        var enumeratingStreamFactory =
                new EnumeratingStreamFactory<>(TestdataPinnedWithIndexListSolution.buildSolutionDescriptor(),
                        EnvironmentMode.PHASE_ASSERT);
        var uniDataset =
                ((AbstractUniEnumeratingStream<TestdataPinnedWithIndexListSolution, TestdataPinnedWithIndexListValue>) enumeratingStreamFactory
                        .forEachNonDiscriminating(TestdataPinnedWithIndexListValue.class, true))
                        .createLeftDataset();

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

        var datasetSession = createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getDatasetInstance(datasetSession, uniDataset);

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(null, value1, value2, value3, value4, unassignedValue);
    }

    @Test
    void forEachListVariableExcludingPinnedValues() {
        var solutionDescriptor = TestdataPinnedWithIndexListSolution.buildSolutionDescriptor();
        var enumeratingStreamFactory = new EnumeratingStreamFactory<>(solutionDescriptor, EnvironmentMode.PHASE_ASSERT);
        var uniDataset =
                ((AbstractUniEnumeratingStream<TestdataPinnedWithIndexListSolution, TestdataPinnedWithIndexListValue>) enumeratingStreamFactory
                        .forEachExcludingPinned(TestdataPinnedWithIndexListValue.class, false))
                        .createLeftDataset();

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

        var datasetSession = createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getDatasetInstance(datasetSession, uniDataset);

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(value2, value3, value4);
    }

    @Test
    void forEachListVariableExcludingPinnedValuesIncludingNull() {
        var solutionDescriptor = TestdataPinnedWithIndexListSolution.buildSolutionDescriptor();
        var enumeratingStreamFactory = new EnumeratingStreamFactory<>(solutionDescriptor, EnvironmentMode.PHASE_ASSERT);
        var uniDataset =
                ((AbstractUniEnumeratingStream<TestdataPinnedWithIndexListSolution, TestdataPinnedWithIndexListValue>) enumeratingStreamFactory
                        .forEachExcludingPinned(TestdataPinnedWithIndexListValue.class, true))
                        .createLeftDataset();

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

        var datasetSession = createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getDatasetInstance(datasetSession, uniDataset);

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(null, value2, value3, value4);
    }

}