package ai.timefold.solver.jackson.api.score.stream.common;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.api.score.stream.common.Break;
import ai.timefold.solver.core.api.score.stream.common.Sequence;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;

record DeserializedSequenceChain<Value_, Difference_ extends Comparable<Difference_>>(
        List<DeserializedSequence<Value_, Difference_>> sequences,
        List<DeserializedBreak<Value_, Difference_>> breaks)
        implements
            SequenceChain<Value_, Difference_> {

    @Override
    public Collection<Sequence<Value_, Difference_>> getConsecutiveSequences() {
        return Collections.unmodifiableCollection(sequences());
    }

    @Override
    public Collection<Break<Value_, Difference_>> getBreaks() {
        return Collections.unmodifiableCollection(breaks());
    }

    @Override
    public Sequence<Value_, Difference_> getFirstSequence() {
        return sequences().get(0);
    }

    @Override
    public Sequence<Value_, Difference_> getLastSequence() {
        return sequences().get(sequences().size() - 1);
    }

    @Override
    public Break<Value_, Difference_> getFirstBreak() {
        if (breaks().isEmpty()) {
            return null;
        }
        return breaks().get(0);
    }

    @Override
    public Break<Value_, Difference_> getLastBreak() {
        if (breaks().isEmpty()) {
            return null;
        }
        return breaks().get(breaks().size() - 1);
    }
}
