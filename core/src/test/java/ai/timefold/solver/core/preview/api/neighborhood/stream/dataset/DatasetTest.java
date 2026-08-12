package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultNeighborhoodSession;
import ai.timefold.solver.core.impl.score.director.SessionContext;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.impl.solver.random.RandomSource;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.BiNeighborhoodsJoiner;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class DatasetTest {

    private static DefaultMoveStreamFactory<TestdataSolution> factory() {
        return new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
    }

    private static DefaultNeighborhoodSession<TestdataSolution> createSession(
            DefaultMoveStreamFactory<TestdataSolution> moveStreamFactory, TestdataSolution solution) {
        var scoreDirector = new EasyScoreDirectorFactory<>(moveStreamFactory.getSolutionDescriptor(),
                s -> SimpleScore.ZERO, EnvironmentMode.PHASE_ASSERT).buildScoreDirector();
        scoreDirector.setWorkingSolution(solution);
        var session = moveStreamFactory.createSession(new SessionContext<>(scoreDirector));
        moveStreamFactory.getSolutionDescriptor().visitAll(solution, session::insert);
        session.settle();
        return session;
    }

    @Test
    void registerUni_sizeAndIteratorsVisitEveryElementExactlyOnce() {
        var moveStreamFactory = factory();
        var entityDataset = moveStreamFactory.register(moveStreamFactory.forEach(TestdataEntity.class, false));

        var solution = TestdataSolution.generateSolution(2, 3); // 2 values, 3 entities.
        var session = createSession(moveStreamFactory, solution);
        var instance = session.getInstance(entityDataset);

        assertThat(instance.size()).isEqualTo(3);

        var seen = new HashSet<TestdataEntity>();
        instance.iterator().forEachRemaining(seen::add);
        assertThat(seen).containsExactlyInAnyOrderElementsOf(solution.getEntityList());

        var uniquelySeen = new HashSet<TestdataEntity>();
        var uniqueRandomIterator = instance.uniqueRandomIterator(RandomSource.seeded(0L).moveIteratorUsage());
        uniqueRandomIterator.forEachRemaining(uniquelySeen::add);
        assertThat(uniquelySeen).containsExactlyInAnyOrderElementsOf(solution.getEntityList());
    }

    @Test
    void registerUni_randomIteratorNeverEndsAndCanRepeat() {
        var moveStreamFactory = factory();
        var entityDataset = moveStreamFactory.register(moveStreamFactory.forEach(TestdataEntity.class, false));

        var solution = TestdataSolution.generateSolution(2, 2); // 2 values, 2 entities.
        var session = createSession(moveStreamFactory, solution);
        var instance = session.getInstance(entityDataset);

        var random = RandomSource.seeded(0L).moveIteratorUsage();
        var randomIterator = instance.randomIterator(random);

        // Draw far more times than there are elements; it must still never run dry.
        var draws = new ArrayList<TestdataEntity>();
        for (var i = 0; i < 100; i++) {
            assertThat(randomIterator.hasNext()).isTrue();
            draws.add(randomIterator.next());
        }
        assertThat(draws).containsAnyElementsOf(solution.getEntityList());
        // With only 2 elements and 100 draws, at least one must have repeated.
        assertThat(Set.of(draws)).hasSizeLessThan(draws.size());

        assertThat(randomIterator.hasNext()).isTrue();
        assertThatThrownBy(() -> randomIterator.forEachRemaining(e -> {
        }))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void registerSameStreamTwice_sharesOneDataset() {
        var moveStreamFactory = factory();
        var stream = moveStreamFactory.forEach(TestdataEntity.class, false);

        assertThat(moveStreamFactory.register(stream)).isEqualTo(moveStreamFactory.register(stream));
    }

    @Test
    void cachedAndJustInTimeJoin_agreeOnEveryRow() {
        var moveStreamFactory = factory();
        var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
        var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
        BiNeighborhoodsJoiner<TestdataEntity, TestdataValue> joiner =
                NeighborhoodsJoiners.equal(TestdataEntity::getValue, v -> v);

        // Cached: the join is materialized in bavet, via register(a.join(b)).
        var cachedDataset = moveStreamFactory.register(entityStream.join(valueStream, joiner));
        // Just-in-time: the join is computed inside the BiDatasetInstance, via register(a).join(b).
        var entityDataset = moveStreamFactory.register(entityStream);
        var justInTimeDataset = entityDataset.join(valueStream, joiner);

        var solution = TestdataSolution.generateSolution(4, 2); // 4 values, 2 entities.
        var session = createSession(moveStreamFactory, solution);

        var cachedInstance = session.getInstance(cachedDataset);
        var justInTimeInstance = session.getInstance(justInTimeDataset);

        assertThat(cachedInstance.size()).isEqualTo(justInTimeInstance.size());
        assertThat(collectPairs(cachedInstance)).isEqualTo(collectPairs(justInTimeInstance));

        // Keyed lookup must also agree, for every left value actually present.
        for (var entity : solution.getEntityList()) {
            assertThat(collectValues(cachedInstance, entity)).isEqualTo(collectValues(justInTimeInstance, entity));
        }
    }

    @Test
    void cachedJoin_perALookupOnSkewedBucketsAlwaysFindsItsMatch() {
        var moveStreamFactory = factory();
        var rareEntity = new TestdataEntity("rare");
        var commonEntity = new TestdataEntity("common");
        var valueList = new ArrayList<TestdataValue>();
        valueList.add(new TestdataValue("rare"));
        for (var i = 0; i < 99; i++) {
            valueList.add(new TestdataValue("common"));
        }

        var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
        var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
        var joiner =
                NeighborhoodsJoiners.equal(TestdataEntity::getCode, TestdataValue::getCode);
        var cachedDataset = moveStreamFactory.register(entityStream.join(valueStream, joiner));

        var solution = new TestdataSolution("solution");
        solution.setEntityList(List.of(rareEntity, commonEntity));
        solution.setValueList(valueList);
        var session = createSession(moveStreamFactory, solution);
        var instance = session.getInstance(cachedDataset);

        var random = RandomSource.seeded(0L).moveIteratorUsage();
        for (var draw = 0; draw < 200; draw++) {
            var iterator = instance.randomIterator(rareEntity, random);
            assertThat(iterator.hasNext())
                    .as("draw %d: the rare entity's single partner must always be found once indexed", draw)
                    .isTrue();
            assertThat(iterator.next()).isNotNull();
        }

        var seenCommonValueSet = new HashSet<TestdataValue>();
        for (var draw = 0; draw < 2_000; draw++) {
            var iterator = instance.randomIterator(commonEntity, random);
            assertThat(iterator.hasNext()).isTrue();
            seenCommonValueSet.add(iterator.next());
        }
        assertThat(seenCommonValueSet).hasSize(99);

        // A retract+settle must invalidate the per-A index, not leave it stale.
        moveStreamFactory.getSolutionDescriptor().visitAll(solution, session::retract);
        session.settle();
        assertThat(instance.size(rareEntity)).isZero();
        assertThat(instance.randomIterator(rareEntity, random).hasNext()).isFalse();
    }

    private static Set<List<Object>> collectPairs(BiDatasetInstance<TestdataEntity, TestdataValue> instance) {
        Set<List<Object>> pairs = new HashSet<>();
        var iterator = instance.iterator();
        while (iterator.hasNext()) {
            iterator.next();
            pairs.add(Arrays.asList(iterator.getA(), iterator.getB()));
        }
        return pairs;
    }

    private static Set<TestdataValue> collectValues(BiDatasetInstance<TestdataEntity, TestdataValue> instance,
            TestdataEntity a) {
        var result = new HashSet<TestdataValue>();
        var iterator = instance.iterator(a);
        while (iterator.hasNext()) {
            var value = iterator.next();
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

}
