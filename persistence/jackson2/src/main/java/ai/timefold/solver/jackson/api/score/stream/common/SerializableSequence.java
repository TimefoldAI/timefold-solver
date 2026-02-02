package ai.timefold.solver.jackson.api.score.stream.common;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

record SerializableSequence<Value_>(
        @JsonProperty("previous_break") SerializableBreak<Value_> previousBreak,
        @JsonProperty("next_break") SerializableBreak<Value_> nextBreak,
        boolean first, boolean last, List<Value_> items) {

    public SerializableSequence(SerializableBreak<Value_> previousBreak, SerializableBreak<Value_> nextBreak,
            List<Value_> items) {
        this(previousBreak, nextBreak, previousBreak == null, nextBreak == null, items);
    }

    public SerializableSequence(boolean first, boolean last, List<Value_> items) {
        this(null, null, first, last, items);
    }

}
