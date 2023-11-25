package ai.timefold.solver.jackson.api.score.stream.common;

import java.util.Collection;
import java.util.List;

import ai.timefold.solver.core.api.score.stream.common.Break;
import ai.timefold.solver.core.api.score.stream.common.Sequence;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(scope = SerializedSequence.class, generator = ObjectIdGenerators.IntSequenceGenerator.class)
record DeserializedSequence<Value_, Difference_ extends Comparable<Difference_>>(
        @JsonProperty("previous_break") DeserializedBreak<Value_, Difference_> previousBreak,
        @JsonProperty("next_break") DeserializedBreak<Value_, Difference_> nextBreak,
        List<Value_> items)
        implements
            Sequence<Value_, Difference_> {

    @Override
    public Value_ getFirstItem() {
        return items.get(0);
    }

    @Override
    public Value_ getLastItem() {
        return items.get(items.size() - 1);
    }

    @Override
    public boolean isFirst() {
        return previousBreak == null;
    }

    @Override
    public boolean isLast() {
        return nextBreak == null;
    }

    @Override
    public Break<Value_, Difference_> getPreviousBreak() {
        return previousBreak();
    }

    @Override
    public Break<Value_, Difference_> getNextBreak() {
        return nextBreak();
    }

    @Override
    public Collection<Value_> getItems() {
        return items();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Difference_ getLength() {
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
