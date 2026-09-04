package ai.timefold.solver.core.impl.move.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.api.domain.valuerange.ValueRange;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.domain.valuerange.ListValueRange;
import ai.timefold.solver.core.impl.move.MoveDirector;
import ai.timefold.solver.core.impl.score.director.easy.EasyScoreDirectorFactory;
import ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample.Sample;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEasyScoreCalculator;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedEntity;
import ai.timefold.solver.core.testdomain.unassignedvar.TestdataAllowsUnassignedSolution;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingScoreCalculator;
import ai.timefold.solver.core.testdomain.valuerange.entityproviding.unassignedvar.TestdataAllowsUnassignedEntityProvidingSolution;

import org.junit.jupiter.api.Test;

class SampleValueRangesTest {

    @Test
    void of_sampleBased_excludesNullAndChecksLegalityAcrossEveryMember() {
        var solutionDescriptor = TestdataAllowsUnassignedEntityProvidingSolution.buildSolutionDescriptor();
        var variableMetaModel = solutionDescriptor.getMetaModel()
                .genuineEntity(TestdataAllowsUnassignedEntityProvidingEntity.class).basicVariable();
        var scoreDirectorFactory = new EasyScoreDirectorFactory<>(solutionDescriptor,
                new TestdataAllowsUnassignedEntityProvidingScoreCalculator(), EnvironmentMode.PHASE_ASSERT);
        var scoreDirector = scoreDirectorFactory.buildScoreDirector();

        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var v3 = new TestdataValue("v3");
        var entityA = new TestdataAllowsUnassignedEntityProvidingEntity("a", List.of(v1, v2));
        var entityB = new TestdataAllowsUnassignedEntityProvidingEntity("b", List.of(v1, v2)); // content-equal to A's range
        var entityC = new TestdataAllowsUnassignedEntityProvidingEntity("c", List.of(v1, v3)); // genuinely different

        var solution = new TestdataAllowsUnassignedEntityProvidingSolution("s");
        solution.setEntityList(List.of(entityA, entityB, entityC));
        scoreDirector.setWorkingSolution(solution);

        var moveDirector = new MoveDirector<>(scoreDirector);
        var sample = Sample.of(List.of(entityA, entityB, entityC));

        var ranges = SampleValueRanges.of(sample, variableMetaModel, moveDirector);

        // v1 is legal for every member; v2 is not (out of range for C).
        assertThat(ranges.containsInEvery(v1)).isTrue();
        assertThat(ranges.containsInEvery(v2)).isFalse();
        // The variable allows unassigned, so the cached range is null-wrapped;
        // containsInEvery(...) must reject null directly, or it would be treated as a legal destination.
        assertThat(ranges.containsInEvery(null)).isFalse();
    }

    @Test
    void of_solutionScoped_readsOneRangeForTheWholeSample() {
        var solutionDescriptor = TestdataAllowsUnassignedSolution.buildSolutionDescriptor();
        var variableMetaModel = solutionDescriptor.getMetaModel()
                .genuineEntity(TestdataAllowsUnassignedEntity.class).basicVariable();
        var scoreDirectorFactory = new EasyScoreDirectorFactory<>(solutionDescriptor,
                new TestdataAllowsUnassignedEasyScoreCalculator(), EnvironmentMode.PHASE_ASSERT);
        var scoreDirector = scoreDirectorFactory.buildScoreDirector();

        var v1 = new TestdataValue("v1");
        var v2 = new TestdataValue("v2");
        var entityA = new TestdataAllowsUnassignedEntity("a", v1);
        var entityB = new TestdataAllowsUnassignedEntity("b", v1);
        var entityC = new TestdataAllowsUnassignedEntity("c", v1);

        var solution = new TestdataAllowsUnassignedSolution("s");
        solution.setValueList(List.of(v1, v2));
        solution.setEntityList(List.of(entityA, entityB, entityC));
        scoreDirector.setWorkingSolution(solution);

        var moveDirector = spy(new MoveDirector<>(scoreDirector));
        var sample = Sample.of(List.of(entityA, entityB, entityC));

        var ranges = SampleValueRanges.of(sample, variableMetaModel, moveDirector);

        // Every member shares one solution-scoped range instance: one lookup, not one per member.
        assertThat(ranges.distinctRangeSet()).hasSize(1);
        assertThat(ranges.containsInEvery(v1)).isTrue();
        assertThat(ranges.containsInEvery(v2)).isTrue();
        assertThat(ranges.containsInEvery(null)).isFalse();
        verify(moveDirector, times(1)).getValueRange(eq(variableMetaModel), any());
    }

    @Test
    void bailOutSizeOf_clampsHugeSizeInsteadOfOverflowing() {
        // Long.MAX_VALUE * BAIL_OUT_SAFETY_MULTIPLIER would overflow negative,
        // and FilteringIterator reads a negative bailOutSize as "bail-out disabled" - turning hasNext() into an infinite loop.
        assertThat(SampleValueRanges.bailOutSizeOf(new HugeValueRange())).isPositive();
    }

    @Test
    void pickExactly_returnsNullWhenIntersectionIsEmpty() {
        var smallest = new ListValueRange<>(List.of("a", "b"));
        var other = new ListValueRange<>(List.of("c", "d"));
        var random = new Random(0);

        var ranges = SampleValueRanges.of(Set.of(smallest, other));

        assertThat(ranges.pickExactly(random, null)).isNull();
    }

    @Test
    void pickExactly_excludesTheGivenValue() {
        var only = new ListValueRange<>(List.of("only"));
        var random = new Random(0);

        var ranges = SampleValueRanges.of(Set.of(only));

        assertThat(ranges.pickExactly(random, "only")).isNull();
    }

    @Test
    void findDestination_singleRangeReturnsAMember() {
        var range = new ListValueRange<>(List.of("a", "b", "c"));
        var random = new Random(0);

        var ranges = SampleValueRanges.of(Set.of(range));

        assertThat(ranges.findDestination(random, null)).isIn("a", "b", "c");
    }

    @Test
    void findDestination_singleRangeExcludesTheGivenValueEvenAsTheOnlyCandidate() {
        var only = new ListValueRange<>(List.of("only"));
        var random = new Random(0);

        // The single-distinct-range case still has to honor the exclusion (MassChange's "not the current value" rule):
        // here the range's one element IS the excluded value,
        // so no destination exists at all - this must come back null, not the excluded value itself.
        var ranges = SampleValueRanges.of(Set.of(only));

        assertThat(ranges.findDestination(random, "only")).isNull();
    }

    @Test
    void findDestination_multiRangeReturnsAnIntersectionMemberOrNullWhenDisjoint() {
        var overlapping = new ListValueRange<>(List.of("a", "b"));
        var other = new ListValueRange<>(List.of("b", "c"));
        var random = new Random(0);

        var overlappingRanges = SampleValueRanges.of(Set.of(overlapping, other));
        assertThat(overlappingRanges.findDestination(random, null)).isEqualTo("b");

        var disjointA = new ListValueRange<>(List.of("a"));
        var disjointB = new ListValueRange<>(List.of("b"));
        var disjointRanges = SampleValueRanges.of(Set.of(disjointA, disjointB));
        assertThat(disjointRanges.findDestination(random, null)).isNull();
    }

    @Test
    void equals_ignoresOrderButNotMembership() {
        var rangeA = new ListValueRange<>(List.of("a"));
        var rangeB = new ListValueRange<>(List.of("b"));
        var rangeC = new ListValueRange<>(List.of("c"));

        assertThat(SampleValueRanges.of(Set.of(rangeA, rangeB))).isEqualTo(SampleValueRanges.of(Set.of(rangeB, rangeA)));
        assertThat(SampleValueRanges.of(Set.of(rangeA, rangeB))).isNotEqualTo(SampleValueRanges.of(Set.of(rangeA, rangeC)));
        assertThat(SampleValueRanges.of(Set.of(rangeA))).isNotEqualTo(SampleValueRanges.of(Set.of(rangeA, rangeB)));
    }

    /** A range too large for {@code getSize() * BAIL_OUT_SAFETY_MULTIPLIER} to fit in a {@code long}. */
    private static final class HugeValueRange implements ValueRange<Long> {

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean contains(Long value) {
            return true;
        }

        @Override
        public long getSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public Long get(long index) {
            return index;
        }

        @Override
        public Iterator<Long> createOriginalIterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Long> createRandomIterator(RandomGenerator workingRandom) {
            throw new UnsupportedOperationException();
        }

    }

}
