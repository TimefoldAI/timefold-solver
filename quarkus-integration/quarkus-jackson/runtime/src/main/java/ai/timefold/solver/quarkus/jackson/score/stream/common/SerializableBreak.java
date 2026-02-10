package ai.timefold.solver.quarkus.jackson.score.stream.common;

import ai.timefold.solver.core.api.score.stream.common.Break;

import com.fasterxml.jackson.annotation.JsonProperty;

record SerializableBreak<Value_>(
        @JsonProperty("previous_sequence_end") Value_ previousSequenceEnd,
        @JsonProperty("next_sequence_start") Value_ nextSequenceStart,
        boolean first, boolean last) {

    static <Value_> SerializableBreak<Value_> of(Break<Value_, ?> brk) {
        if (brk == null) {
            return null;
        }
        return new SerializableBreak<>(brk.getPreviousSequenceEnd(), brk.getNextSequenceStart(), brk.isFirst(), brk.isLast());
    }

}
