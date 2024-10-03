package ai.timefold.solver.jackson.api.score.stream.common;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.score.stream.common.Break;
import ai.timefold.solver.core.api.score.stream.common.Sequence;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

record DeserializableSequenceChain<Value_, Difference_ extends Comparable<Difference_>>(
        List<DeserializableSequence<Value_, Difference_>> sequences)
        implements
            SequenceChain<Value_, Difference_> {

    @Override
    public @NonNull Collection<Sequence<Value_, Difference_>> getConsecutiveSequences() {
        return Collections.unmodifiableCollection(sequences());
    }

    @Override
    public @NonNull Collection<Break<Value_, Difference_>> getBreaks() {
        throw new UnsupportedOperationException("""
                Deserialized %s does not carry break information.
                It can be computed from the sequences."""
                .formatted(getClass().getSimpleName()));
    }

    @Override
    public @Nullable Sequence<Value_, Difference_> getFirstSequence() {
        return sequences().get(0);
    }

    @Override
    public @Nullable Sequence<Value_, Difference_> getLastSequence() {
        return sequences().get(sequences().size() - 1);
    }

    @Override
    public @Nullable Break<Value_, Difference_> getFirstBreak() {
        throw new UnsupportedOperationException("""
                Deserialized %s does not carry break information.
                It can be computed from the sequences."""
                .formatted(getClass().getSimpleName()));
    }

    @Override
    public @Nullable Break<Value_, Difference_> getLastBreak() {
        throw new UnsupportedOperationException("""
                Deserialized %s does not carry break information.
                It can be computed from the sequences."""
                .formatted(getClass().getSimpleName()));
    }
}
