package ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.api.solver.SolutionManager;
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
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.NeighborhoodsCollectors;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.UniNeighborhoodsCollector;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.UniNeighborhoodsCollectorAccumulator;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.UniNeighborhoodsCollectorValueHandle;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.UniNeighborhoodsMapper;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListValue;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

@NullMarked
class UniEnumeratingStreamTest {

    // ************************************************************************
    // Helpers
    // ************************************************************************

    private static EnumeratingStreamFactory<TestdataSolution> factory() {
        return new EnumeratingStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
    }

    private static AbstractUniEnumeratingStream<TestdataSolution, TestdataEntity> entityStream(
            EnumeratingStreamFactory<TestdataSolution> factory) {
        return (AbstractUniEnumeratingStream<TestdataSolution, TestdataEntity>) factory
                .forEachNonDiscriminating(TestdataEntity.class, false);
    }

    private static <Solution_, A> UniLeftDatasetInstance<Solution_, A> getInstance(DatasetSession<Solution_> session,
            UniLeftDataset<Solution_, A> dataset) {
        return (UniLeftDatasetInstance<Solution_, A>) session.getInstance(dataset);
    }

    private static <Solution_> DatasetSession<Solution_> createSession(
            EnumeratingStreamFactory<Solution_> enumeratingStreamFactory,
            Solution_ solution) {
        var scoreDirector =
                new EasyScoreDirectorFactory<>(enumeratingStreamFactory.getSolutionDescriptor(), s -> SimpleScore.ZERO,
                        EnvironmentMode.PHASE_ASSERT)
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

    // ************************************************************************
    // forEach
    // ************************************************************************

    @Test
    void forEach_basicVar() {
        var enumeratingStreamFactory =
                new EnumeratingStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var uniDataset = ((AbstractUniEnumeratingStream<TestdataSolution, TestdataEntity>) enumeratingStreamFactory
                .forEachNonDiscriminating(TestdataEntity.class, false))
                .createLeftDataset();

        var solution = TestdataSolution.generateSolution(2, 2);
        var datasetSession = createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getInstance(datasetSession, uniDataset);

        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(entity1, entity2);

        // Make incremental changes.
        var entity3 = new TestdataEntity("entity3", solution.getValueList().getFirst());
        datasetSession.insert(entity3);
        datasetSession.retract(entity2);
        datasetSession.settle();

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(entity1, entity3);
    }

    @Test
    void forEachIncludingNull_basicVar() {
        var enumeratingStreamFactory =
                new EnumeratingStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var uniDataset = ((AbstractUniEnumeratingStream<TestdataSolution, TestdataEntity>) enumeratingStreamFactory
                .forEachNonDiscriminating(TestdataEntity.class, true))
                .createLeftDataset();

        var solution = TestdataSolution.generateSolution(2, 2);
        var datasetSession = createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getInstance(datasetSession, uniDataset);

        var entity1 = solution.getEntityList().get(0);
        var entity2 = solution.getEntityList().get(1);

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(null, entity1, entity2);

        // Make incremental changes.
        var entity3 = new TestdataEntity("entity3", solution.getValueList().getFirst());
        datasetSession.insert(entity3);
        datasetSession.retract(entity2);
        datasetSession.settle();

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(null, entity1, entity3);
    }

    @Test
    void forEach_listVar() {
        var enumeratingStreamFactory =
                new EnumeratingStreamFactory<>(TestdataListSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var uniDataset = ((AbstractUniEnumeratingStream<TestdataListSolution, TestdataListEntity>) enumeratingStreamFactory
                .forEachNonDiscriminating(TestdataListEntity.class, false))
                .createLeftDataset();

        var solution = TestdataListSolution.generateInitializedSolution(2, 2);
        var datasetSession = createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getInstance(datasetSession, uniDataset);

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
    void forEachIncludingNull_listVar() {
        var enumeratingStreamFactory =
                new EnumeratingStreamFactory<>(TestdataListSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var uniDataset = ((AbstractUniEnumeratingStream<TestdataListSolution, TestdataListEntity>) enumeratingStreamFactory
                .forEachNonDiscriminating(TestdataListEntity.class, true))
                .createLeftDataset();

        var solution = TestdataListSolution.generateInitializedSolution(2, 2);
        var datasetSession = createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getInstance(datasetSession, uniDataset);

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

    @Test
    void forEach_listVarIncludingPinned() {
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
        partiallyPinnedEntity.setPinIndex(1);
        // 1 value, not pinned.
        var unpinnedEntity = solution.getEntityList().get(2);
        unpinnedEntity.setPinned(false);
        unpinnedEntity.setPinIndex(0);

        var datasetSession = createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getInstance(datasetSession, uniDataset);

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
    void forEachIncludingNull_listVarIncludingPinned() {
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
        partiallyPinnedEntity.setPinIndex(1);
        // 1 value, not pinned.
        var unpinnedEntity = solution.getEntityList().get(2);
        unpinnedEntity.setPinned(false);
        unpinnedEntity.setPinIndex(0);

        var datasetSession = createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getInstance(datasetSession, uniDataset);

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
    void forEachExcludingPinned_listVar() { // Entities with planningPin true will be skipped.
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
        partiallyPinnedEntity.setPinIndex(1);
        // 1 value, not pinned.
        var unpinnedEntity = solution.getEntityList().get(2);
        unpinnedEntity.setPinned(false);
        unpinnedEntity.setPinIndex(0);

        var datasetSession = createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getInstance(datasetSession, uniDataset);

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
    void forEachExcludingPinnedIncludingNull_listVar() { // Entities with planningPin true will be skipped.
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
        partiallyPinnedEntity.setPinIndex(1);
        // 1 value, not pinned.
        var unpinnedEntity = solution.getEntityList().get(2);
        unpinnedEntity.setPinned(false);
        unpinnedEntity.setPinIndex(0);

        var datasetSession = createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getInstance(datasetSession, uniDataset);

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
    void forEach_listVarIncludingPinnedValues() {
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
        var fullyPinnedEntity = solution.getEntityList().getFirst();
        fullyPinnedEntity.setPinned(true);
        fullyPinnedEntity.setValueList(List.of(value1));
        // 2 values, 1 pinned.
        var partiallyPinnedEntity = solution.getEntityList().get(1);
        partiallyPinnedEntity.setPinIndex(1);
        partiallyPinnedEntity.setValueList(List.of(value2, value3));
        // 1 value, not pinned.
        var unpinnedEntity = solution.getEntityList().get(2);
        unpinnedEntity.setPinned(false);
        unpinnedEntity.setPinIndex(0);
        unpinnedEntity.setValueList(List.of(value4));
        // Properly set shadow variables based on the changes above.
        SolutionManager.updateShadowVariables(solution);

        var datasetSession = createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getInstance(datasetSession, uniDataset);

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(value1, value2, value3, value4, unassignedValue);
    }

    @Test
    void forEachIncludingNull_listVarIncludingPinnedValues() {
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
        var fullyPinnedEntity = solution.getEntityList().getFirst();
        fullyPinnedEntity.setPinned(true);
        fullyPinnedEntity.setValueList(List.of(value1));
        // 2 values, 1 pinned.
        var partiallyPinnedEntity = solution.getEntityList().get(1);
        partiallyPinnedEntity.setPinIndex(1);
        partiallyPinnedEntity.setValueList(List.of(value2, value3));
        // 1 value, not pinned.
        var unpinnedEntity = solution.getEntityList().get(2);
        unpinnedEntity.setPinned(false);
        unpinnedEntity.setPinIndex(0);
        unpinnedEntity.setValueList(List.of(value4));
        // Properly set shadow variables based on the changes above.
        SolutionManager.updateShadowVariables(solution);

        var datasetSession = createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getInstance(datasetSession, uniDataset);

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(null, value1, value2, value3, value4, unassignedValue);
    }

    @Test
    void forEachExcludingPinned_listVarValues() {
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
        var fullyPinnedEntity = solution.getEntityList().getFirst();
        fullyPinnedEntity.setPinned(true);
        fullyPinnedEntity.setValueList(List.of(value0));
        // 2 values, 1 pinned.
        var partiallyPinnedEntity = solution.getEntityList().get(1);
        partiallyPinnedEntity.setPinIndex(1);
        partiallyPinnedEntity.setValueList(List.of(value1, value2));
        // 1 value, not pinned.
        var unpinnedEntity = solution.getEntityList().get(2);
        unpinnedEntity.setPinned(false);
        unpinnedEntity.setPinIndex(0);
        unpinnedEntity.setValueList(List.of(value3));
        // Fully pinned, but not initially present in the solution.
        var entityAddedLater = new TestdataPinnedWithIndexListEntity("entity4", value4);
        entityAddedLater.setPinned(true);
        // Properly set shadow variables based on the changes above.
        SolutionManager.updateShadowVariables(solution);

        var datasetSession = createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getInstance(datasetSession, uniDataset);

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(value2, value3, value4);
    }

    @Test
    void forEachExcludingPinnedIncludingNull_listVarValues() {
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
        var fullyPinnedEntity = solution.getEntityList().getFirst();
        fullyPinnedEntity.setPinned(true);
        fullyPinnedEntity.setValueList(List.of(value0));
        // 2 values, 1 pinned.
        var partiallyPinnedEntity = solution.getEntityList().get(1);
        partiallyPinnedEntity.setPinIndex(1);
        // 1 value, not pinned.
        partiallyPinnedEntity.setValueList(List.of(value1, value2));
        var unpinnedEntity = solution.getEntityList().get(2);
        unpinnedEntity.setPinned(false);
        unpinnedEntity.setPinIndex(0);
        unpinnedEntity.setValueList(List.of(value3));
        // Fully pinned, but not initially present in the solution.
        var entityAddedLater = new TestdataPinnedWithIndexListEntity("entity4", value4);
        entityAddedLater.setPinned(true);
        // Properly set shadow variables based on the changes above.
        SolutionManager.updateShadowVariables(solution);

        var datasetSession = createSession(enumeratingStreamFactory, solution);
        var uniDatasetInstance = getInstance(datasetSession, uniDataset);

        assertThat(uniDatasetInstance.iterator())
                .toIterable()
                .map(UniTuple::getA)
                .containsExactly(null, value2, value3, value4);
    }

    // ************************************************************************
    // groupBy
    // ************************************************************************

    @Test
    void groupBy_1Mapping0Collector() {
        var factory = factory();
        UniNeighborhoodsMapper<TestdataSolution, TestdataEntity, TestdataValue> byValue =
                (view, entity) -> entity.getValue();
        var groupedStream = entityStream(factory).groupBy(byValue);
        var dataset = groupedStream.createLeftDataset();

        // generateSolution(2 values, 4 entities): e0→v0, e1→v1, e2→v0, e3→v1
        var solution = TestdataSolution.generateSolution(2, 4);
        var session = createSession(factory, solution);
        var instance = getInstance(session, dataset);

        var v0 = solution.getValueList().get(0);
        var v1 = solution.getValueList().get(1);

        assertThat(instance.iterator()).toIterable().map(UniTuple::getA).containsExactlyInAnyOrder(v0, v1);

        // Reassign e2 from v0 to v1; v0 still has e0.
        var e2 = solution.getEntityList().get(2);
        e2.setValue(v1);
        session.update(e2);
        session.settle();

        assertThat(instance.iterator()).toIterable().map(UniTuple::getA).containsExactlyInAnyOrder(v0, v1);

        // Reassign e0 from v0 to v1 → v0 group disappears.
        var e0 = solution.getEntityList().getFirst();
        e0.setValue(v1);
        session.update(e0);
        session.settle();

        assertThat(instance.iterator()).toIterable().map(UniTuple::getA).containsExactly(v1);
    }

    @Test
    void groupBy_1Mapping1Collector() {
        var factory = factory();
        UniNeighborhoodsMapper<TestdataSolution, TestdataEntity, TestdataValue> byValue =
                (view, entity) -> entity.getValue();
        var groupedStream = entityStream(factory).groupBy(byValue, NeighborhoodsCollectors.toList());
        var mappedStream = (AbstractUniEnumeratingStream<TestdataSolution, String>) groupedStream
                .map((view, value, entities) -> value.getCode() + "=" + entities.size());
        var dataset = mappedStream.createLeftDataset();

        var solution = TestdataSolution.generateSolution(2, 4);
        var session = createSession(factory, solution);
        var instance = getInstance(session, dataset);

        assertThat(instance.iterator()).toIterable().map(UniTuple::getA)
                .containsExactlyInAnyOrder("Generated Value 0=2", "Generated Value 1=2");

        var v0 = solution.getValueList().getFirst();
        var newEntity = new TestdataEntity("New Entity", v0);
        solution.getEntityList().add(newEntity);
        session.insert(newEntity);
        session.settle();

        assertThat(instance.iterator()).toIterable().map(UniTuple::getA)
                .containsExactlyInAnyOrder("Generated Value 0=3", "Generated Value 1=2");
    }

    @Test
    void groupBy_0Mapping1Collector() {
        var factory = factory();
        var groupedStream = entityStream(factory)
                .groupBy(NeighborhoodsCollectors.<TestdataSolution, TestdataEntity> toList());
        var dataset = groupedStream.createLeftDataset();

        var solution = TestdataSolution.generateSolution(2, 3);
        var session = createSession(factory, solution);
        var instance = getInstance(session, dataset);

        assertThat(instance.iterator()).toIterable()
                .map(UniTuple::getA)
                .hasSize(1);

        var v0 = solution.getValueList().getFirst();
        var newEntity = new TestdataEntity("New Entity", v0);
        solution.getEntityList().add(newEntity);
        session.insert(newEntity);
        session.settle();

        assertThat(instance.iterator()).toIterable()
                .map(UniTuple::getA)
                .hasSize(1);
    }

    @Test
    void distinct() {
        var factory = factory();
        var mappedStream = (AbstractUniEnumeratingStream<TestdataSolution, TestdataValue>) entityStream(factory)
                .map((view, entity) -> entity.getValue())
                .distinct();
        var dataset = mappedStream.createLeftDataset();

        var solution = TestdataSolution.generateSolution(2, 4);
        var session = createSession(factory, solution);
        var instance = getInstance(session, dataset);
        var v0 = solution.getValueList().get(0);
        var v1 = solution.getValueList().get(1);

        assertThat(instance.iterator()).toIterable().map(UniTuple::getA).containsExactlyInAnyOrder(v0, v1);
    }

    @Test
    void groupBy_1Mapping1Collector_customCollector() {
        var factory = factory();
        UniNeighborhoodsMapper<TestdataSolution, TestdataEntity, TestdataValue> byValue =
                (view, entity) -> entity.getValue();
        UniNeighborhoodsCollector<TestdataSolution, TestdataEntity, int[], Integer> countCollector =
                new NeighborhoodsUniCountCollector();

        var groupedStream = entityStream(factory).groupBy(byValue, countCollector);
        var mappedStream = (AbstractUniEnumeratingStream<TestdataSolution, String>) groupedStream
                .map((view, value, count) -> value.getCode() + "=" + count);
        var dataset = mappedStream.createLeftDataset();

        var solution = TestdataSolution.generateSolution(2, 4);
        var session = createSession(factory, solution);
        var instance = getInstance(session, dataset);

        assertThat(instance.iterator()).toIterable().map(UniTuple::getA)
                .containsExactlyInAnyOrder("Generated Value 0=2", "Generated Value 1=2");

        var e0 = solution.getEntityList().getFirst();
        session.retract(e0);
        session.settle();

        assertThat(instance.iterator()).toIterable().map(UniTuple::getA)
                .containsExactlyInAnyOrder("Generated Value 0=1", "Generated Value 1=2");
    }

    @Test
    void groupBy_1Mapping0Collector_solutionView() {
        var factory = factory();
        var accessCount = new AtomicInteger();
        UniNeighborhoodsMapper<TestdataSolution, TestdataEntity, TestdataValue> mapper = (view, entity) -> {
            accessCount.incrementAndGet();
            return entity.getValue();
        };
        var groupedStream = entityStream(factory).groupBy(mapper);
        var dataset = groupedStream.createLeftDataset();

        var solution = TestdataSolution.generateSolution(2, 4);
        var session = createSession(factory, solution);
        var instance = getInstance(session, dataset);
        var v0 = solution.getValueList().get(0);
        var v1 = solution.getValueList().get(1);

        assertThat(instance.iterator()).toIterable().map(UniTuple::getA).containsExactlyInAnyOrder(v0, v1);
        assertThat(accessCount.get()).isPositive();

        var e2 = solution.getEntityList().get(2);
        var oldCount = accessCount.get();
        e2.setValue(v1);
        session.update(e2);
        session.settle();

        assertThat(instance.iterator()).toIterable().map(UniTuple::getA).containsExactlyInAnyOrder(v0, v1);
        assertThat(accessCount.get()).isGreaterThan(oldCount);
    }

    private static final class NeighborhoodsUniCountCollector
            implements UniNeighborhoodsCollector<TestdataSolution, TestdataEntity, int[], Integer> {
        @Override
        public Supplier<int[]> supplier() {
            return () -> new int[] { 0 };
        }

        @Override
        public UniNeighborhoodsCollectorAccumulator<TestdataSolution, TestdataEntity, int[]> accumulator() {
            return (view, container) -> new UniNeighborhoodsCollectorValueHandle<TestdataEntity>() {
                @Override
                public void add(@Nullable TestdataEntity entity) {
                    container[0]++;
                }

                @Override
                public void remove() {
                    container[0]--;
                }
            };
        }

        @Override
        public Function<int[], @Nullable Integer> finisher() {
            return c -> c[0];
        }
    }
}
