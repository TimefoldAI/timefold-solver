package ai.timefold.solver.jackson.api.score.stream.common;

import ai.timefold.solver.core.api.score.stream.common.Break;

import org.jspecify.annotations.NonNull;

import com.fasterxml.jackson.annotation.JsonProperty;

record DeserializableBreak<Value_, Difference_ extends Comparable<Difference_>>(
        @JsonProperty("previous_sequence_end") Value_ previousSequenceEnd,
        @JsonProperty("next_sequence_start") Value_ nextSequenceStart,
        boolean first, boolean last)
        implements
            Break<Value_, Difference_> {

    @Override
    public boolean isFirst() {
        return first();
    }

    @Override
    public boolean isLast() {
        return last();
    }

    @Override
    public @NonNull Value_ getPreviousSequenceEnd() {
        return previousSequenceEnd();
    }

    @Override
    public @NonNull Value_ getNextSequenceStart() {
        return nextSequenceStart();
    }

    @Override
    public @NonNull Difference_ getLength() {
        /*
         * Difference_ is a custom type, hidden deep within the object tree.
         * Allowing for deserialization of this would have significantly complicated the code,
         * and put extra burdens on the user.
         * This way, deserialization can happen out-of-the-box,
         * and the information can still be computed on the user side.
         */
        throw new UnsupportedOperationException("""
                Deserialized %s does not carry length information.
                It can be computed from the endpoints."""
                .formatted(getClass().getSimpleName()));
    }
}
