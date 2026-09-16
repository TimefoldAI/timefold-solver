package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset;

import static ai.timefold.solver.core.testutil.NeighborhoodTestUtils.createSession;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
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

    @Test
    void asCachedDatasetUni_sizeAndIteratorsVisitEveryElementExactlyOnce() {
        var moveStreamFactory =
                new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var entityDataset = moveStreamFactory.forEach(TestdataEntity.class, false).asCachedDataset();

        var solution = TestdataSolution.generateSolution(2, 3); // 2 values, 3 entities.
        var session = createSession(moveStreamFactory, solution);
        var instance = session.getInstance(entityDataset);

        assertThat(instance.size()).isEqualTo(3);

        // The Dataset wires in exactly this instance's elements.
        var uniquelySeen = new HashSet<TestdataEntity>();
        var exhaustiveIterator = instance.exhaustiveIterator(RandomSource.seeded(0L).moveIteratorUsage());
        while (exhaustiveIterator.hasNext()) {
            uniquelySeen.add(exhaustiveIterator.next());
        }
        assertThat(uniquelySeen).containsExactlyInAnyOrderElementsOf(solution.getEntityList());
    }

    @Test
    void asCachedDatasetUni_randomIteratorNeverEndsAndCanRepeat() {
        var moveStreamFactory =
                new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var entityDataset = moveStreamFactory.forEach(TestdataEntity.class, false).asCachedDataset();

        var solution = TestdataSolution.generateSolution(2, 2); // 2 values, 2 entities.
        var session = createSession(moveStreamFactory, solution);
        var instance = session.getInstance(entityDataset);

        var random = RandomSource.seeded(0L).moveIteratorUsage();
        var randomIterator = instance.iterator(random);

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
    void asCachedDatasetSameStreamTwice_sharesOneDataset() {
        var moveStreamFactory =
                new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var stream = moveStreamFactory.forEach(TestdataEntity.class, false);

        assertThat(stream.asCachedDataset()).isEqualTo(stream.asCachedDataset());
    }

    @Test
    void cachedAndJustInTimeJoin_agreeOnEveryRow() {
        var moveStreamFactory =
                new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
        var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
        BiNeighborhoodsJoiner<TestdataEntity, TestdataValue> joiner =
                NeighborhoodsJoiners.equal(TestdataEntity::getValue, v -> v);

        // Cached: the join is materialized in bavet, via a.join(b).asCachedDataset().
        var cachedDataset = entityStream.join(valueStream, joiner).asCachedDataset();
        // Just-in-time: the join is computed inside the BiDatasetInstance, via a.asCachedDataset().join(b).
        var entityDataset = entityStream.asCachedDataset();
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
        var moveStreamFactory =
                new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
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
        var cachedDataset = entityStream.join(valueStream, joiner).asCachedDataset();

        var solution = new TestdataSolution("solution");
        solution.setEntityList(List.of(rareEntity, commonEntity));
        solution.setValueList(valueList);
        var session = createSession(moveStreamFactory, solution);
        var instance = session.getInstance(cachedDataset);

        var random = RandomSource.seeded(0L).moveIteratorUsage();
        for (var draw = 0; draw < 200; draw++) {
            var iterator = instance.iterator(rareEntity, random);
            assertThat(iterator.hasNext())
                    .as("draw %d: the rare entity's single partner must always be found once indexed", draw)
                    .isTrue();
            assertThat(iterator.next()).isNotNull();
        }

        var seenCommonValueSet = new HashSet<TestdataValue>();
        for (var draw = 0; draw < 2_000; draw++) {
            var iterator = instance.iterator(commonEntity, random);
            assertThat(iterator.hasNext()).isTrue();
            seenCommonValueSet.add(iterator.next());
        }
        assertThat(seenCommonValueSet).hasSize(99);

        // A retract+settle must invalidate the per-A index, not leave it stale.
        moveStreamFactory.getSolutionDescriptor().visitAll(solution, session::retract);
        session.settle();
        assertThat(instance.size(rareEntity)).isZero();
        assertThat(instance.iterator(rareEntity, random).hasNext()).isFalse();
    }

    @Test
    void cachedJoin_lessThanProducesPairsOrderedLeftBeforeRight() {
        var moveStreamFactory =
                new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
        var joiner = NeighborhoodsJoiners.lessThan(TestdataValue::getCode);
        var cachedDataset = valueStream.join(valueStream, joiner).asCachedDataset();

        var solution = TestdataSolution.generateSolution(3, 0); // 3 values, 0 entities: only the values matter here.
        var session = createSession(moveStreamFactory, solution);
        var instance = session.getInstance(cachedDataset);
        var v0 = solution.getValueList().get(0);
        var v1 = solution.getValueList().get(1);
        var v2 = solution.getValueList().get(2);

        assertThat(collectPairs(instance)).containsExactlyInAnyOrder(
                List.of(v0, v1), List.of(v0, v2), List.of(v1, v2));
    }

    @Test
    void justInTimeJoin_lessThanProducesPairsOrderedLeftBeforeRight() {
        var moveStreamFactory =
                new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
        var joiner = NeighborhoodsJoiners.lessThan(TestdataValue::getCode);
        var valueDataset = valueStream.asCachedDataset();
        var justInTimeDataset = valueDataset.join(valueStream, joiner);

        var solution = TestdataSolution.generateSolution(3, 0);
        var session = createSession(moveStreamFactory, solution);
        var instance = session.getInstance(justInTimeDataset);
        var v0 = solution.getValueList().get(0);
        var v1 = solution.getValueList().get(1);
        var v2 = solution.getValueList().get(2);

        assertThat(collectPairs(instance)).containsExactlyInAnyOrder(
                List.of(v0, v1), List.of(v0, v2), List.of(v1, v2));
    }

    private static <A, B> Set<List<Object>> collectPairs(BiDatasetInstance<A, B> instance) {
        Set<List<Object>> pairs = new HashSet<>();
        var iterator = instance.exhaustiveIterator(RandomSource.seeded(0L).moveIteratorUsage());
        while (iterator.hasNext()) {
            iterator.next();
            pairs.add(Arrays.asList(iterator.a(), iterator.b()));
        }
        return pairs;
    }

    private static Set<TestdataValue> collectValues(BiDatasetInstance<TestdataEntity, TestdataValue> instance,
            TestdataEntity a) {
        var result = new HashSet<TestdataValue>();
        var iterator = instance.exhaustiveIterator(a, RandomSource.seeded(0L).moveIteratorUsage());
        while (iterator.hasNext()) {
            var value = iterator.next();
            if (value != null) {
                result.add(value);
            }
        }
        return result;
    }

}
