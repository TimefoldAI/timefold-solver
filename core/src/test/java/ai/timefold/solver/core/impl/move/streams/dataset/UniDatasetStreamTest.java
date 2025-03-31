package ai.timefold.solver.core.impl.move.streams.dataset;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;

import org.junit.jupiter.api.Test;

class UniDatasetStreamTest {

    @Test
    void forEachBasicVariable() {
        var dataStreamFactory = new DataStreamFactory<>(TestdataSolution.buildSolutionDescriptor());
        var uniDataset = ((AbstractUniDataStream<TestdataSolution, TestdataEntity>) dataStreamFactory
                .forEach(TestdataEntity.class))
                .createDataset();

        var solution = TestdataSolution.generateSolution(2, 2);
        var datasetSession = UniDatasetStreamTest.createSession(dataStreamFactory, solution);
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

    @Test
    void forEachListVariable() {
        var dataStreamFactory = new DataStreamFactory<>(TestdataListSolution.buildSolutionDescriptor());
        var uniDataset = ((AbstractUniDataStream<TestdataListSolution, TestdataListEntity>) dataStreamFactory
                .forEach(TestdataListEntity.class))
                .createDataset();

        var solution = TestdataListSolution.generateInitializedSolution(2, 2);
        var datasetSession = createSession(dataStreamFactory, solution);
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

    private static <Solution_> DatasetSession<Solution_> createSession(DataStreamFactory<Solution_> dataStreamFactory,
            Solution_ solution) {
        var datasetSessionFactory = new DatasetSessionFactory<>(dataStreamFactory);
        var datasetSession = datasetSessionFactory.buildSession();
        datasetSession.initialize(solution);

        var solutionDescriptor = dataStreamFactory.getSolutionDescriptor();
        solutionDescriptor.visitAll(solution, datasetSession::insert);

        datasetSession.settle();
        return datasetSession;
    }

}
