package ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.DatasetSession;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.DatasetSessionFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.EnumeratingStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractLeftDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.AbstractUniEnumeratingStream;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniLeftDataset;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni.UniLeftDatasetInstance;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.preview.api.neighborhood.stream.enumerating.collector.NeighborhoodsCollectors;
import ai.timefold.solver.core.preview.api.neighborhood.stream.function.UniNeighborhoodsMapper;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.junit.jupiter.api.Test;

/**
 * Proves that {@code groupBy(key, collectAndThen(toList(), Sample::of))} yields a cached
 * {@code UniDataset<Solution_, Sample<Entity_>>}
 * that is safe for a pillar move to hold across a settle:
 * each settle that changes a group produces a fresh, immutable {@link Sample}, never a live view into the group's mutating
 * accumulator.
 * This is the mechanism {@code PillarDatasetUtil} and the four full-pillar move providers are built on;
 * this test predates and does not depend on either.
 */
class PillarDatasetTest {

    private static EnumeratingStreamFactory<TestdataSolution> factory() {
        return new EnumeratingStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
    }

    @SuppressWarnings("unchecked")
    private static UniLeftDatasetInstance<TestdataSolution, Sample<TestdataEntity>> buildInstance(
            EnumeratingStreamFactory<TestdataSolution> factory, TestdataSolution solution,
            DatasetSession<TestdataSolution>[] sessionOut) {
        var entityStream = (AbstractUniEnumeratingStream<TestdataSolution, TestdataEntity>) factory
                .forEachNonDiscriminating(TestdataEntity.class, false);
        UniNeighborhoodsMapper<TestdataSolution, TestdataEntity, TestdataValue> byValue =
                (view, entity) -> entity.getValue();
        var grouped = entityStream.groupBy(byValue,
                NeighborhoodsCollectors.collectAndThen(
                        NeighborhoodsCollectors.<TestdataSolution, TestdataEntity> toList(),
                        Sample::of));
        var mapped = (AbstractUniEnumeratingStream<TestdataSolution, Sample<TestdataEntity>>) grouped
                .map((view, value, pillar) -> pillar);
        var dataset = (UniLeftDataset<TestdataSolution, Sample<TestdataEntity>>) mapped.asCachedDataset();

        var scoreDirector = new EasyScoreDirectorFactory<>(factory.getSolutionDescriptor(),
                (TestdataSolution s) -> SimpleScore.ZERO, EnvironmentMode.PHASE_ASSERT).buildScoreDirector();
        scoreDirector.setWorkingSolution(solution);
        DatasetSession<TestdataSolution> session =
                new DatasetSessionFactory<>(factory).buildSession(new SessionContext<>(scoreDirector));
        factory.getSolutionDescriptor().visitAll(solution, session::insert);
        session.settle();
        sessionOut[0] = session;

        return (UniLeftDatasetInstance<TestdataSolution, Sample<TestdataEntity>>) session
                .getInstance((AbstractLeftDataset<TestdataSolution, UniTuple<Sample<TestdataEntity>>>) dataset);
    }

    private static List<Sample<TestdataEntity>> rowsOf(
            UniLeftDatasetInstance<TestdataSolution, Sample<TestdataEntity>> instance) {
        var rows = new ArrayList<Sample<TestdataEntity>>();
        instance.iterator().forEachRemaining(tuple -> rows.add(tuple.getA()));
        return rows;
    }

    @Test
    void oneRowPerDistinctValue() {
        var factory = factory();
        var v0 = new TestdataValue("v0");
        var v1 = new TestdataValue("v1");
        var e0 = new TestdataEntity("e0", v0);
        var e1 = new TestdataEntity("e1", v0);
        var e2 = new TestdataEntity("e2", v1);
        var solution = new TestdataSolution("solution");
        solution.setValueList(List.of(v0, v1));
        solution.setEntityList(new ArrayList<>(List.of(e0, e1, e2)));

        @SuppressWarnings("unchecked")
        var sessionOut = (DatasetSession<TestdataSolution>[]) new DatasetSession[1];
        var instance = buildInstance(factory, solution, sessionOut);

        var rows = rowsOf(instance);
        assertThat(rows).hasSize(2);
        var v0Pillar = rows.stream().filter(p -> p.size() == 2).findFirst().orElseThrow();
        var v1Pillar = rows.stream().filter(p -> p.size() == 1).findFirst().orElseThrow();
        assertThat(v0Pillar.contains(e0)).isTrue();
        assertThat(v0Pillar.contains(e1)).isTrue();
        assertThat(v1Pillar.contains(e2)).isTrue();
    }

    @Test
    void pillarHeldAcrossASettleDoesNotChange() {
        var factory = factory();
        var v0 = new TestdataValue("v0");
        var v1 = new TestdataValue("v1");
        var e0 = new TestdataEntity("e0", v0);
        var e1 = new TestdataEntity("e1", v0);
        var e2 = new TestdataEntity("e2", v1);
        var solution = new TestdataSolution("solution");
        solution.setValueList(List.of(v0, v1));
        solution.setEntityList(new ArrayList<>(List.of(e0, e1, e2)));

        @SuppressWarnings("unchecked")
        var sessionOut = (DatasetSession<TestdataSolution>[]) new DatasetSession[1];
        var instance = buildInstance(factory, solution, sessionOut);
        var session = sessionOut[0];

        var held = rowsOf(instance).stream().filter(p -> p.size() == 2).findFirst().orElseThrow();

        // Move e1 out of v0's group.
        e1.setValue(v1);
        session.update(e1);
        session.settle();

        // The held reference must be unaffected...
        assertThat(held.size()).isEqualTo(2);
        assertThat(held.contains(e0)).isTrue();
        assertThat(held.contains(e1)).isTrue();

        // ...while the dataset's current row for v0 is a different, smaller object.
        var current = rowsOf(instance).stream().filter(p -> p.size() == 1).findFirst().orElseThrow();
        assertThat(current).isNotSameAs(held);
        assertThat(current.contains(e0)).isTrue();
        assertThat(current.contains(e1)).isFalse();
    }

    @Test
    void emptyGroupProducesNoPillarRow() {
        var factory = factory();
        var solution = new TestdataSolution("solution");
        solution.setValueList(List.of());
        solution.setEntityList(new ArrayList<>());

        @SuppressWarnings("unchecked")
        var sessionOut = (DatasetSession<TestdataSolution>[]) new DatasetSession[1];
        var instance = buildInstance(factory, solution, sessionOut);

        assertThat(rowsOf(instance)).isEmpty();
    }

    @Test
    void lastMemberRetractionDoesNotBuildAnEmptyPillar() {
        var factory = factory();
        var v0 = new TestdataValue("v0");
        var v1 = new TestdataValue("v1");
        var e0 = new TestdataEntity("e0", v0);
        var solution = new TestdataSolution("solution");
        solution.setValueList(List.of(v0, v1));
        solution.setEntityList(new ArrayList<>(List.of(e0)));

        @SuppressWarnings("unchecked")
        var sessionOut = (DatasetSession<TestdataSolution>[]) new DatasetSession[1];
        var instance = buildInstance(factory, solution, sessionOut);
        var session = sessionOut[0];

        assertThat(rowsOf(instance)).hasSize(1);

        // e0 was the only member of v0's group; move it away entirely.
        e0.setValue(v1);
        assertThatCode(() -> {
            session.update(e0);
            session.settle();
        }).doesNotThrowAnyException();

        assertThat(rowsOf(instance)).hasSize(1); // v1's group now, v0's group is gone.
        assertThat(rowsOf(instance).getFirst().contains(e0)).isTrue();
    }

}
