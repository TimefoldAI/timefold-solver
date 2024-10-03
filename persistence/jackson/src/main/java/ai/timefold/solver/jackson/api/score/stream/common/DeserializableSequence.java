package ai.timefold.solver.jackson.api.score.stream.common;

import java.util.Collection;
import java.util.List;

import ai.timefold.solver.core.api.score.stream.common.Break;
import ai.timefold.solver.core.api.score.stream.common.Sequence;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

record DeserializableSequence<Value_, Difference_ extends Comparable<Difference_>>(
        @JsonProperty("previous_break") DeserializableBreak<Value_, Difference_> previousBreak,
        @JsonProperty("next_break") DeserializableBreak<Value_, Difference_> nextBreak,
        boolean first, boolean last, List<Value_> items)
        implements
            Sequence<Value_, Difference_> {

    @Override
    public @NonNull Value_ getFirstItem() {
        return items.get(0);
    }

    @Override
    public @NonNull Value_ getLastItem() {
        return items.get(items.size() - 1);
    }

    @Override
    public boolean isFirst() {
        return first();
    }

    @Override
    public boolean isLast() {
        return last();
    }

    @Override
    public @Nullable Break<Value_, Difference_> getPreviousBreak() {
        return previousBreak();
    }

    @Override
    public @Nullable Break<Value_, Difference_> getNextBreak() {
        return nextBreak();
    }

    @Override
    public @NonNull Collection<Value_> getItems() {
        return items();
    }

    @Override
    public int getCount() {
        return items.size();
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
                It can be computed from the items."""
                .formatted(Sequence.class.getSimpleName()));
    }
}
