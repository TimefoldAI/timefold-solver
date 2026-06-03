package ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.score.SimpleScore;
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
import ai.timefold.solver.core.preview.api.neighborhood.stream.collector.NeighborhoodsCollectors;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.BiNeighborhoodsMapper;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class BiEnumeratingStreamTest {

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

    private static <A> UniLeftDatasetInstance<TestdataSolution, A> getInstance(DatasetSession<TestdataSolution> session,
            UniLeftDataset<TestdataSolution, A> dataset) {
        return (UniLeftDatasetInstance<TestdataSolution, A>) session.getInstance(dataset);
    }

    private static DatasetSession<TestdataSolution> createSession(
            EnumeratingStreamFactory<TestdataSolution> enumeratingStreamFactory,
            TestdataSolution solution) {
        var scoreDirector =
                new EasyScoreDirectorFactory<>(enumeratingStreamFactory.getSolutionDescriptor(), s -> SimpleScore.ZERO,
                        EnvironmentMode.PHASE_ASSERT)
                        .buildScoreDirector();
        scoreDirector.setWorkingSolution(solution);
        var sessionContext = new SessionContext<>(scoreDirector);
        var datasetSessionFactory = new DatasetSessionFactory<>(enumeratingStreamFactory);
        var datasetSession = datasetSessionFactory.buildSession(sessionContext);
        enumeratingStreamFactory.getSolutionDescriptor().visitAll(solution, datasetSession::insert);
        datasetSession.settle();
        return datasetSession;
    }

    // ************************************************************************
    // groupBy
    // ************************************************************************

    @Test
    void groupBy_1Mapping0Collector() {
        var factory = factory();
        var entityStream = entityStream(factory);
        var valueStream = (AbstractUniEnumeratingStream<TestdataSolution, TestdataValue>) factory
                .forEachNonDiscriminating(TestdataValue.class, false);
        var biStream = entityStream.join(valueStream,
                NeighborhoodsJoiners.equal(TestdataEntity::getValue, v -> v));
        BiNeighborhoodsMapper<TestdataSolution, TestdataEntity, TestdataValue, String> byValueCode =
                (view, entity, value) -> value.getCode();
        var groupedStream = (AbstractUniEnumeratingStream<TestdataSolution, String>) biStream.groupBy(byValueCode);
        var dataset = groupedStream.createLeftDataset();

        var solution = TestdataSolution.generateSolution(2, 4);
        var session = createSession(factory, solution);
        var instance = getInstance(session, dataset);

        assertThat(instance.iterator()).toIterable().map(UniTuple::getA)
                .containsExactlyInAnyOrder("Generated Value 0", "Generated Value 1");
    }

    @Test
    void groupBy_1Mapping1Collector() {
        var factory = factory();
        var entityStream = entityStream(factory);
        var valueStream = (AbstractUniEnumeratingStream<TestdataSolution, TestdataValue>) factory
                .forEachNonDiscriminating(TestdataValue.class, false);
        var biStream = entityStream.join(valueStream,
                NeighborhoodsJoiners.equal(TestdataEntity::getValue, v -> v));
        BiNeighborhoodsMapper<TestdataSolution, TestdataEntity, TestdataValue, TestdataValue> byValue =
                (view, entity, value) -> value;
        var groupedStream = biStream.groupBy(byValue,
                NeighborhoodsCollectors.toList((view, entity, value) -> entity.getCode()));
        var mappedStream = (AbstractUniEnumeratingStream<TestdataSolution, String>) groupedStream
                .map((view, value, entityCodes) -> value.getCode() + "=" + entityCodes.size());
        var dataset = mappedStream.createLeftDataset();

        var solution = TestdataSolution.generateSolution(2, 4);
        var session = createSession(factory, solution);
        var instance = getInstance(session, dataset);

        assertThat(instance.iterator()).toIterable().map(UniTuple::getA)
                .containsExactlyInAnyOrder("Generated Value 0=2", "Generated Value 1=2");
    }

}
