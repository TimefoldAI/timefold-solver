package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.IntStream;

import ai.timefold.solver.core.impl.util.ScalingSequencedSet;

import org.junit.jupiter.api.Test;

class SampleTest {

    @Test
    void ofRemovesDuplicates() {
        var sample = Sample.of(List.of("a", "b", "a"));
        assertThat(sample.size()).isEqualTo(2);
        assertThat(sample).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void ofCopiesSoLaterMutationDoesNotLeak() {
        var memberList = new ArrayList<>(List.of("a", "b"));
        var sample = Sample.of(memberList);
        memberList.add("c");
        assertThat(sample.size()).isEqualTo(2);
        assertThat(sample.contains("c")).isFalse();
    }

    @Test
    void ofAlwaysCopiesEvenASequencedSet() {
        var memberSet = new LinkedHashSet<>(List.of("a", "b"));
        var sample = Sample.of(memberSet);
        memberSet.add("c");
        // of() always copies, regardless of the input's concrete type - no leak, unlike wrap().
        assertThat(sample.size()).isEqualTo(2);
        assertThat(sample.contains("c")).isFalse();
    }

    @Test
    void wrapAdoptsInsteadOfCopying() {
        var memberSet = new LinkedHashSet<>(List.of("a", "b"));
        var sample = Sample.wrap(memberSet);
        memberSet.add("c");
        // wrap() adopts, not copies - later mutation of the caller's collection leaks into the sample.
        assertThat(sample.size()).isEqualTo(3);
        assertThat(sample.contains("c")).isTrue();
    }

    @Test
    void wrapAcceptsAnArrayBackedList() {
        var members = Arrays.asList("c", "a", "b");
        var wrapped = Sample.wrap(members);
        var built = Sample.of(List.of("a", "b", "c"));
        assertThat(wrapped).isEqualTo(built);
        assertThat(built).isEqualTo(wrapped);
        assertThat(wrapped).hasSameHashCodeAs(built);
    }

    @Test
    void nullIsALegalMember() {
        var sample = Sample.of(Arrays.asList("a", null));
        assertThat(sample.size()).isEqualTo(2);
        assertThat(sample.contains(null)).isTrue();
        assertThat(sample.contains("zzz")).isFalse();
    }

    @Test
    void equalityIsOrderInsensitive() {
        var left = Sample.of(List.of("a", "b", "c"));
        var right = Sample.of(List.of("c", "a", "b"));
        assertThat(left).isEqualTo(right);
        assertThat(left).hasSameHashCodeAs(right);
    }

    @Test
    void equalityToleratesNullMembers() {
        var left = Sample.of(Arrays.asList("a", null));
        var right = Sample.of(Arrays.asList(null, "a"));
        assertThat(left).isEqualTo(right);
        assertThat(left).hasSameHashCodeAs(right);
    }

    @Test
    void differentMembersAreNotEqual() {
        assertThat(Sample.of(List.of("a"))).isNotEqualTo(Sample.of(List.of("b")));
    }

    @Test
    void wrapAdoptsAScalingOrderedSetInsteadOfCopyingIt() {
        var memberSet = new ScalingSequencedSet<String>(2);
        memberSet.addAll(List.of("a", "b"));
        var sample = Sample.wrap(memberSet);
        memberSet.add("c");
        // wrap() adopts, not copies - later mutation of the caller's set leaks into the sample.
        // SampleAssembler builds exactly this type, so adoption is what keeps the assembler's set out of a LinkedHashSet.
        assertThat(sample.size()).isEqualTo(3);
        assertThat(sample.contains("c")).isTrue();
        assertThat(sample.representative()).isEqualTo("a");
    }

    @Test
    void equalityHoldsAcrossSetImplementations() {
        var memberSet = new ScalingSequencedSet<String>(3);
        memberSet.addAll(List.of("c", "a", "b"));
        var left = Sample.of(memberSet);
        var right = Sample.of(List.of("a", "b", "c"));

        assertThat(left).isEqualTo(right);
        assertThat(right).isEqualTo(left);
        assertThat(left).hasSameHashCodeAs(right);
    }

    @Test
    void equalityHoldsAcrossSetImplementationsAboveTheListThreshold() {
        var memberList = IntStream.range(0, 20).boxed().toList();
        var memberSet = new ScalingSequencedSet<Integer>(20);
        memberSet.addAll(memberList.reversed());
        var left = Sample.of(memberSet);
        var right = Sample.of(memberList);

        assertThat(left).isEqualTo(right);
        assertThat(right).isEqualTo(left);
        assertThat(left).hasSameHashCodeAs(right);
    }

}
