package ai.timefold.solver.core.impl.move.streams.dataset;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningListVariableMetaModel;
import ai.timefold.solver.core.impl.domain.solution.descriptor.DefaultPlanningVariableMetaModel;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementLocation;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningListVariableMetaModel;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningVariableMetaModel;
import ai.timefold.solver.core.preview.api.move.SolutionView;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class UniDatasetStreamTest {

    @Test
    void forEach() {
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

    private DatasetSession<TestdataListSolution> createSession(DataStreamFactory<TestdataListSolution> dataStreamFactory,
            TestdataListSolution solution) {
        var datasetSessionFactory = new DatasetSessionFactory<>(dataStreamFactory);
        var datasetSession = datasetSessionFactory.buildSession();
        var solutionView = new TestdataListSolutionView(solution);
        datasetSession.initialize();
        datasetSession.updateWorkingSolution(solutionView, solution);

        var solutionDescriptor = dataStreamFactory.getSolutionDescriptor();
        solutionDescriptor.visitAll(solution, datasetSession::insert);

        datasetSession.settle();
        return datasetSession;
    }

    @NullMarked
    private record TestdataListSolutionView(TestdataListSolution solution) implements SolutionView<TestdataListSolution> {

        @Override
        public <Entity_, Value_> @Nullable Value_
                getValue(PlanningVariableMetaModel<TestdataListSolution, Entity_, Value_> variableMetaModel, Entity_ entity) {
            var descriptor = ((DefaultPlanningVariableMetaModel<TestdataListSolution, Entity_, Value_>) variableMetaModel)
                    .variableDescriptor();
            return descriptor.getValue(entity);
        }

        @Override
        public <Entity_, Value_> Value_ getValueAtIndex(
                PlanningListVariableMetaModel<TestdataListSolution, Entity_, Value_> variableMetaModel, Entity_ entity,
                int index) {
            var descriptor = ((DefaultPlanningListVariableMetaModel<TestdataListSolution, Entity_, Value_>) variableMetaModel)
                    .variableDescriptor();
            return descriptor.getElement(entity, index);
        }

        @Override
        public <Entity_, Value_> ElementLocation getPositionOf(
                PlanningListVariableMetaModel<TestdataListSolution, Entity_, Value_> variableMetaModel, Value_ value) {
            var descriptor = ((DefaultPlanningListVariableMetaModel<TestdataListSolution, Entity_, Value_>) variableMetaModel)
                    .variableDescriptor();
            for (var entity : solution.getEntityList()) {
                var list = descriptor.getValue(entity);
                for (int i = 0; i < list.size(); i++) {
                    var position = list.indexOf(value);
                    if (position != -1) {
                        return ElementLocation.of(entity, position);
                    }
                }
            }
            return ElementLocation.unassigned();
        }

    }

}
