package ai.timefold.solver.jackson.api.score.stream.common;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@JsonIdentityInfo(scope = SerializedSequence.class, generator = ObjectIdGenerators.IntSequenceGenerator.class)
record SerializedSequence<Value_>(
        @JsonProperty("previous_break") SerializedBreak<Value_> previousBreak,
        @JsonProperty("next_break") SerializedBreak<Value_> nextBreak,
        List<Value_> items) {

}
