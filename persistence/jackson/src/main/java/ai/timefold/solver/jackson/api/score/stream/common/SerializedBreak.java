package ai.timefold.solver.jackson.api.score.stream.common;

import ai.timefold.solver.core.api.score.stream.common.Break;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(scope = SerializedBreak.class, generator = ObjectIdGenerators.IntSequenceGenerator.class)
record SerializedBreak<Value_>(
        @JsonProperty("previous_sequence_end") Value_ previousSequenceEnd,
        @JsonProperty("next_sequence_start") Value_ nextSequenceStart,
        boolean first, boolean last) {

    static <Value_> SerializedBreak<Value_> of(Break<Value_, ?> brk) {
        if (brk == null) {
            return null;
        }
        return new SerializedBreak<>(brk.getPreviousSequenceEnd(), brk.getNextSequenceStart(), brk.isFirst(), brk.isLast());
    }

}
