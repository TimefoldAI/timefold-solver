package ai.timefold.solver.core.impl.neighborhood.stream.dataset.sample;

import static ai.timefold.solver.core.testutil.NeighborhoodTestUtils.createSession;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.neighborhood.stream.DefaultMoveStreamFactory;
import ai.timefold.solver.core.impl.solver.random.RandomSource;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.PillarSampler;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample.Decision;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sampler;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Samplers;
import ai.timefold.solver.core.preview.api.neighborhood.stream.joiner.NeighborhoodsJoiners;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

class SamplingIteratorTest {

    @Test
    void samplingIteratorDrainsSourceWhenSamplerNeverStops() {
        var moveStreamFactory =
                new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var valueDataset = moveStreamFactory.forEach(TestdataValue.class, false).asCachedDataset();

        var solution = TestdataSolution.generateSolution(5, 0); // 5 values, 0 entities.
        var session = createSession(moveStreamFactory, solution);
        var instance = session.getInstance(valueDataset);

        var random = RandomSource.seeded(0L).moveIteratorUsage();
        var sampleIterator = instance.samplingIterator(Samplers.all(), random);

        // First sample should contain all values.
        assertThat(sampleIterator.hasNext()).isTrue();
        var firstSample = sampleIterator.next();
        assertThat(firstSample.size()).isEqualTo(5);
        assertThat(firstSample).contains(solution.getValueList().toArray(new TestdataValue[0]));

        // Second sample should also contain all values (fresh source).
        assertThat(sampleIterator.hasNext()).isTrue();
        var secondSample = sampleIterator.next();
        assertThat(secondSample.size()).isEqualTo(5);
        assertThat(secondSample).contains(solution.getValueList().toArray(new TestdataValue[0]));
    }

    @Test
    void emptyDatasetHasNoNextSample() {
        var moveStreamFactory =
                new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var valueDataset = moveStreamFactory.forEach(TestdataValue.class, false).asCachedDataset();

        var solution = new TestdataSolution("solution");
        solution.setEntityList(List.of());
        solution.setValueList(List.of());
        var session = createSession(moveStreamFactory, solution);
        var instance = session.getInstance(valueDataset);

        var random = RandomSource.seeded(0L).moveIteratorUsage();
        var sampleIterator = instance.samplingIterator(Samplers.all(), random);

        assertThat(sampleIterator.hasNext()).isFalse();
    }

    @Test
    void samplerResetAndDecideAreCalledCorrectly() {
        var moveStreamFactory =
                new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var valueDataset = moveStreamFactory.forEach(TestdataValue.class, false).asCachedDataset();

        var solution = TestdataSolution.generateSolution(3, 0);
        var session = createSession(moveStreamFactory, solution);
        var instance = session.getInstance(valueDataset);

        var resetCallCount = new int[1];
        var decideCalls = new ArrayList<Integer>();
        var recordingSampler = new Sampler<TestdataValue>() {
            @Override
            public void reset(@NonNull RandomGenerator random) {
                resetCallCount[0]++;
            }

            @Override
            public Decision evaluate(int sizeSoFar, TestdataValue candidate) {
                decideCalls.add(sizeSoFar);
                return Samplers.<TestdataValue> exactly(2).evaluate(sizeSoFar, candidate);
            }
        };

        var random = RandomSource.seeded(0L).moveIteratorUsage();
        var sampleIterator = instance.samplingIterator(recordingSampler, random);

        // Draw first sample.
        assertThat(sampleIterator).hasNext();
        assertThat(sampleIterator.next()).isNotNull();

        // Reset should have been called exactly once.
        assertThat(resetCallCount[0]).isEqualTo(1);
        // decide should have been called for every candidate offered, starting with the first.
        assertThat(decideCalls).isNotEmpty();
        // First decide call should have sizeSoFar == 0: the first candidate is no longer an unconditionally accepted seed.
        assertThat(decideCalls.getFirst()).isZero();
    }

    @Test
    void sampleContainsNoDuplicates() {
        var moveStreamFactory =
                new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var valueDataset = moveStreamFactory.forEach(TestdataValue.class, false).asCachedDataset();

        var solution = TestdataSolution.generateSolution(5, 0);
        var session = createSession(moveStreamFactory, solution);
        var instance = session.getInstance(valueDataset);

        var random = RandomSource.seeded(0L).moveIteratorUsage();
        var sampleIterator = instance.samplingIterator(Samplers.all(), random);

        var firstSample = sampleIterator.next();
        var seenElements = new ArrayList<TestdataValue>();
        firstSample.iterator().forEachRemaining(seenElements::add);

        assertThat(seenElements).doesNotHaveDuplicates();
    }

    @Test
    void sampleCanContainAndCheckForNullability() {
        var moveStreamFactory =
                new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var valueDataset = moveStreamFactory.forEach(TestdataValue.class, false).asCachedDataset();

        var solution = TestdataSolution.generateSolution(3, 0);
        var session = createSession(moveStreamFactory, solution);
        var instance = session.getInstance(valueDataset);

        var random = RandomSource.seeded(0L).moveIteratorUsage();
        var sampleIterator = instance.samplingIterator(Samplers.all(), random);

        var sample = sampleIterator.next();

        // Sample's contains method can be called with null without error (even though null is not in this sample).
        assertThat(sample.contains(null)).isFalse();
    }

    @Test
    void samplerMinimumSizeIsCheckedAfterDeduplication() {
        record Candidate(String code) {
        }
        // Two distinct instances, equal by content:
        // the assembler must count the sample's distinct members, not the raw candidate count,
        // when it checks minimumSize.
        var source = List.of(new Candidate("only"), new Candidate("only")).iterator();

        var sampler = new Sampler<Candidate>() {
            @Override
            public int minimumSize() {
                return 2;
            }

            @Override
            public Decision evaluate(int sizeSoFar, Candidate candidate) {
                return Decision.ACCEPT; // Never stops itself; only the drained source ends the sample.
            }
        };

        var random = RandomSource.seeded(0L).moveIteratorUsage();
        // Both candidates are equal,
        // so only 1 distinct member is ever accepted - below the sampler's minimumSize of 2.
        // Must be discarded entirely (null), not returned as an undersized Sample of size 1.
        assertThat(SampleAssembler.assemble(source, random, sampler)).isNull();
    }

    @Test
    void duplicateCandidateDoesNotEndSampleBelowMinimumSize() {
        // A duplicate right where Samplers.exactly(2) would decide ACCEPT_AND_STOP (sizeSoFar + 1 >= 2)
        // must not end the sample: the duplicate does not grow the distinct member set, so the sampler's
        // size accounting was wrong about having reached its target. The source must still be drained
        // until a genuinely new member arrives, per the contract now documented on Sample.Decision.ACCEPT_AND_STOP.
        var source = List.of("a", "a", "b").iterator();
        var random = RandomSource.seeded(0L).moveIteratorUsage();

        var sample = SampleAssembler.assemble(source, random, Samplers.<String> exactly(2));

        assertThat(sample).isNotNull();
        assertThat(sample.size()).isEqualTo(2);
        assertThat(sample).contains("a", "b");
    }

    // ===== Bi tests =====

    @Test
    void biSamplingIteratorEmptySliceHasNoNextSample() {
        var moveStreamFactory =
                new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
        var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
        var joiner = NeighborhoodsJoiners.<TestdataEntity, TestdataValue, String> equal(TestdataEntity::getCode,
                value -> value.getCode().split("-")[0]);
        var biDataset = entityStream.asCachedDataset().join(valueStream, joiner);

        var solution = new TestdataSolution("solution");
        solution.setEntityList(List.of(new TestdataEntity("probe")));
        solution.setValueList(List.of(new TestdataValue("other-value")));
        var session = createSession(moveStreamFactory, solution);
        var instance = session.getInstance(biDataset);

        var probe = new TestdataEntity("probe");
        var random = RandomSource.seeded(0L).moveIteratorUsage();
        var sampleIterator = instance.samplingIterator(probe, Samplers.all(), random);

        assertThat(sampleIterator.hasNext()).isFalse();
    }

    @Test
    void biSamplingIteratorDrainsSliceWhenSamplerNeverStops() {
        var moveStreamFactory =
                new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
        var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
        var joiner = NeighborhoodsJoiners.<TestdataEntity, TestdataValue, String> equal(TestdataEntity::getCode,
                value -> value.getCode().split("-")[0]);
        var biDataset = entityStream.asCachedDataset().join(valueStream, joiner);

        var probe = new TestdataEntity("probe");
        var solution = new TestdataSolution("solution");
        solution.setEntityList(List.of(probe));
        var probeValue1 = new TestdataValue("probe-1");
        var probeValue2 = new TestdataValue("probe-2");
        solution.setValueList(List.of(
                probeValue1,
                probeValue2,
                new TestdataValue("other-1")));
        var session = createSession(moveStreamFactory, solution);
        var instance = session.getInstance(biDataset);

        var random = RandomSource.seeded(0L).moveIteratorUsage();
        var sampleIterator = instance.samplingIterator(probe, Samplers.all(), random);

        // First sample should contain only the matching values.
        assertThat(sampleIterator.hasNext()).isTrue();
        var firstSample = sampleIterator.next();
        assertThat(firstSample.size()).isEqualTo(2);
        assertThat(firstSample).contains(probeValue1, probeValue2);

        // Second sample should also contain the matching values (fresh source).
        assertThat(sampleIterator.hasNext()).isTrue();
        var secondSample = sampleIterator.next();
        assertThat(secondSample.size()).isEqualTo(2);
        assertThat(secondSample).contains(probeValue1, probeValue2);
    }

    @Test
    void biSamplingIteratorReachesAllMatchingMembersWithFilteredJoin() {
        var moveStreamFactory =
                new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
        var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
        // Filtering joiner that rejects most pairs: "probe" entity only matches values starting with "probe-match-".
        var joiner = NeighborhoodsJoiners.<TestdataSolution, TestdataEntity, TestdataValue> filtering(
                (solutionView, entity, value) -> !entity.getCode().equals("probe")
                        || value.getCode().startsWith("probe-match-"));
        // Just-in-time dataset with FilteringIterator in the path.
        var biDataset = entityStream.asCachedDataset().join(valueStream, joiner);

        var probe = new TestdataEntity("probe");
        var solution = new TestdataSolution("solution");
        solution.setEntityList(List.of(probe));
        // Create a slice with 30 matching values and 70 non-matching.
        // With iterator(a, random) and bail-out, many draws would fail, creating false negatives.
        // With exhaustiveIterator(a, random), all 30 are guaranteed to be reached.
        var valueList = new ArrayList<TestdataValue>();
        for (var i = 0; i < 30; i++) {
            valueList.add(new TestdataValue("probe-match-" + i));
        }
        for (var i = 0; i < 70; i++) {
            valueList.add(new TestdataValue("other-" + i));
        }
        solution.setValueList(valueList);
        var session = createSession(moveStreamFactory, solution);
        var instance = session.getInstance(biDataset);

        var random = RandomSource.seeded(0L).moveIteratorUsage();
        var sampleIterator = instance.samplingIterator(probe, Samplers.all(), random);

        // Sample should contain all 30 matching values despite the 70% rejection rate.
        assertThat(sampleIterator.hasNext()).isTrue();
        var sample = sampleIterator.next();
        assertThat(sample.size()).isEqualTo(30);
    }

    @Test
    void biSamplingIteratorDrainsCachedDatasetSliceWhenSamplerNeverStops() {
        var moveStreamFactory =
                new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
        var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
        var joiner = NeighborhoodsJoiners.<TestdataEntity, TestdataValue, String> equal(TestdataEntity::getCode,
                value -> value.getCode().split("-")[0]);
        // Cached dataset (materialized in bavet): join first, then cache.
        var biDataset = entityStream.join(valueStream, joiner).asCachedDataset();

        var probe = new TestdataEntity("probe");
        var solution = new TestdataSolution("solution");
        solution.setEntityList(List.of(probe));
        var probeValue1 = new TestdataValue("probe-1");
        var probeValue2 = new TestdataValue("probe-2");
        solution.setValueList(List.of(
                probeValue1,
                probeValue2,
                new TestdataValue("other-1")));
        var session = createSession(moveStreamFactory, solution);
        var instance = session.getInstance(biDataset);

        var random = RandomSource.seeded(0L).moveIteratorUsage();
        var sampleIterator = instance.samplingIterator(probe, Samplers.all(), random);

        // Cached dataset should also drain its slice correctly.
        assertThat(sampleIterator.hasNext()).isTrue();
        var sample = sampleIterator.next();
        assertThat(sample.size()).isEqualTo(2);
        assertThat(sample).contains(probeValue1, probeValue2);
    }

    @Test
    void biSamplingIteratorPillarSamplerReceivesTheSliceSelectorAsKey() {
        var moveStreamFactory =
                new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
        var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
        var joiner = NeighborhoodsJoiners.<TestdataEntity, TestdataValue, String> equal(TestdataEntity::getCode,
                value -> value.getCode().split("-")[0]);
        var biDataset = entityStream.asCachedDataset().join(valueStream, joiner);

        var probe = new TestdataEntity("probe");
        var solution = new TestdataSolution("solution");
        solution.setEntityList(List.of(probe));
        solution.setValueList(List.of(new TestdataValue("probe-1")));
        var session = createSession(moveStreamFactory, solution);
        var instance = session.getInstance(biDataset);

        var recordedKeys = new ArrayList<TestdataEntity>();
        var recordingSampler = new PillarSampler<TestdataEntity, TestdataValue>() {
            @Override
            public void reset(@NonNull RandomGenerator random, TestdataEntity key) {
                recordedKeys.add(key);
            }

            @Override
            public Decision evaluate(int sizeSoFar, TestdataValue candidate) {
                return Decision.ACCEPT;
            }
        };

        var random = RandomSource.seeded(0L).moveIteratorUsage();
        var sampleIterator = instance.samplingIterator(probe, recordingSampler, random);

        assertThat(sampleIterator.hasNext()).isTrue();
        sampleIterator.next();
        assertThat(recordedKeys).containsExactly(probe);
    }

    @Test
    void biSamplingIteratorEndsWhenSamplerRefusesBelowMinimumSize() {
        var moveStreamFactory =
                new DefaultMoveStreamFactory<>(TestdataSolution.buildSolutionDescriptor(), EnvironmentMode.PHASE_ASSERT);
        var entityStream = moveStreamFactory.forEach(TestdataEntity.class, false);
        var valueStream = moveStreamFactory.forEach(TestdataValue.class, false);
        var joiner = NeighborhoodsJoiners.<TestdataEntity, TestdataValue, String> equal(TestdataEntity::getCode,
                value -> value.getCode().split("-")[0]);
        var biDataset = entityStream.asCachedDataset().join(valueStream, joiner);

        var probe = new TestdataEntity("probe");
        var solution = new TestdataSolution("solution");
        solution.setEntityList(List.of(probe));
        // Only 2 matching values, but the sampler below demands at least 3.
        solution.setValueList(List.of(new TestdataValue("probe-1"), new TestdataValue("probe-2")));
        var session = createSession(moveStreamFactory, solution);
        var instance = session.getInstance(biDataset);

        var random = RandomSource.seeded(0L).moveIteratorUsage();
        var sampleIterator = instance.samplingIterator(probe, Samplers.pillar(Samplers.exactly(3)), random);

        assertThat(sampleIterator.hasNext()).isFalse();
    }

}
