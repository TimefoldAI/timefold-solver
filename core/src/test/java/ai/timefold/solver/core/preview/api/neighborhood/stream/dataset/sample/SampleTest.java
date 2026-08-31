package ai.timefold.solver.core.preview.api.neighborhood.stream.dataset.sample;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

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
    void ofAdoptsASequencedSetInsteadOfCopyingIt() {
        var memberSet = new LinkedHashSet<>(List.of("a", "b"));
        var sample = Sample.of(memberSet);
        memberSet.add("c");
        // A SequencedSet is adopted, not copied - later mutation of the caller's set leaks into the sample.
        assertThat(sample.size()).isEqualTo(3);
        assertThat(sample.contains("c")).isTrue();
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

}
