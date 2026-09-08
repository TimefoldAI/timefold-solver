package ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.DatasetSession;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.DatasetSessionFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi.AbstractBiEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi.BiLeftDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi.BiLeftDatasetInstance;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractLeftDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.AbstractUniEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniLeftDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniLeftDatasetInstance;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.NeighborhoodsCollectors;
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
        return (UniLeftDatasetInstance<TestdataSolution, A>) session
                .getInstance((AbstractLeftDataset<TestdataSolution, UniTuple<A>>) dataset);
    }

    private static <A, B> BiLeftDatasetInstance<TestdataSolution, A, B> getBiInstance(
            DatasetSession<TestdataSolution> session, BiLeftDataset<TestdataSolution, A, B> dataset) {
        return (BiLeftDatasetInstance<TestdataSolution, A, B>) session
                .getInstance((AbstractLeftDataset<TestdataSolution, BiTuple<A, B>>) dataset);
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
    // concat
    // ************************************************************************

    @Test
    void concat_biBi() {
        var factory = factory();
        var valueStream = (AbstractUniEnumeratingStream<TestdataSolution, TestdataValue>) factory
                .forEachNonDiscriminating(TestdataValue.class, false);
        var biStream = entityStream(factory).join(valueStream,
                NeighborhoodsJoiners.equal(TestdataEntity::getValue, v -> v));
        // generateSolution(2 values, 4 entities): e0→v0, e1→v1, e2→v0, e3→v1
        var v0Stream = biStream.filter((view, entity, value) -> value.getCode().equals("Generated Value 0"));
        var v1Stream = biStream.filter((view, entity, value) -> value.getCode().equals("Generated Value 1"));
        var dataset = ((AbstractBiEnumeratingStream<TestdataSolution, TestdataEntity, TestdataValue>) v0Stream
                .concat(v1Stream)).asCachedDataset();

        var solution = TestdataSolution.generateSolution(2, 4);
        var session = createSession(factory, solution);
        var instance = getBiInstance(session, dataset);

        var e0 = solution.getEntityList().get(0);
        var e1 = solution.getEntityList().get(1);
        var e2 = solution.getEntityList().get(2);
        var e3 = solution.getEntityList().get(3);

        assertThat(instance.iterator()).toIterable()
                .extracting(BiTuple::getA, BiTuple::getB)
                .containsExactlyInAnyOrder(
                        tuple(e0, e0.getValue()), tuple(e1, e1.getValue()),
                        tuple(e2, e2.getValue()), tuple(e3, e3.getValue()));
    }

    @Test
    void concat_uniToBiWithPadding() {
        var factory = factory();
        var valueStream = (AbstractUniEnumeratingStream<TestdataSolution, TestdataValue>) factory
                .forEachNonDiscriminating(TestdataValue.class, false);
        // generateSolution(2 values, 4 entities): e0→v0, e1→v1, e2→v0, e3→v1
        var uniStream = entityStream(factory)
                .filter((view, entity) -> entity.getValue().getCode().equals("Generated Value 0"));
        var biStream = entityStream(factory)
                .filter((view, entity) -> entity.getValue().getCode().equals("Generated Value 1"))
                .join(valueStream, NeighborhoodsJoiners.equal(TestdataEntity::getValue, v -> v));
        var dataset = ((AbstractBiEnumeratingStream<TestdataSolution, TestdataEntity, TestdataValue>) uniStream
                .concat(biStream, TestdataEntity::getValue)).asCachedDataset();

        var solution = TestdataSolution.generateSolution(2, 4);
        var session = createSession(factory, solution);
        var instance = getBiInstance(session, dataset);

        var e0 = solution.getEntityList().get(0);
        var e1 = solution.getEntityList().get(1);
        var e2 = solution.getEntityList().get(2);
        var e3 = solution.getEntityList().get(3);

        assertThat(instance.iterator()).toIterable()
                .extracting(BiTuple::getA, BiTuple::getB)
                .containsExactlyInAnyOrder(
                        tuple(e0, e0.getValue()), tuple(e2, e2.getValue()),
                        tuple(e1, e1.getValue()), tuple(e3, e3.getValue()));
    }

    @Test
    void concat_biToUniWithPadding() {
        var factory = factory();
        var valueStream = (AbstractUniEnumeratingStream<TestdataSolution, TestdataValue>) factory
                .forEachNonDiscriminating(TestdataValue.class, false);
        // generateSolution(2 values, 4 entities): e0→v0, e1→v1, e2→v0, e3→v1
        var biStream = entityStream(factory)
                .filter((view, entity) -> entity.getValue().getCode().equals("Generated Value 0"))
                .join(valueStream, NeighborhoodsJoiners.equal(TestdataEntity::getValue, v -> v));
        var uniStream = entityStream(factory)
                .filter((view, entity) -> entity.getValue().getCode().equals("Generated Value 1"));
        var dataset = ((AbstractBiEnumeratingStream<TestdataSolution, TestdataEntity, TestdataValue>) biStream
                .concat(uniStream, TestdataEntity::getValue)).asCachedDataset();

        var solution = TestdataSolution.generateSolution(2, 4);
        var session = createSession(factory, solution);
        var instance = getBiInstance(session, dataset);

        var e0 = solution.getEntityList().get(0);
        var e1 = solution.getEntityList().get(1);
        var e2 = solution.getEntityList().get(2);
        var e3 = solution.getEntityList().get(3);

        assertThat(instance.iterator()).toIterable()
                .extracting(BiTuple::getA, BiTuple::getB)
                .containsExactlyInAnyOrder(
                        tuple(e0, e0.getValue()), tuple(e2, e2.getValue()),
                        tuple(e1, e1.getValue()), tuple(e3, e3.getValue()));
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
        var dataset = groupedStream.asCachedDataset();

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
        var dataset = mappedStream.asCachedDataset();

        var solution = TestdataSolution.generateSolution(2, 4);
        var session = createSession(factory, solution);
        var instance = getInstance(session, dataset);

        assertThat(instance.iterator()).toIterable().map(UniTuple::getA)
                .containsExactlyInAnyOrder("Generated Value 0=2", "Generated Value 1=2");
    }

}
