package ai.timefold.solver.jackson.api.score.stream.common;

import java.util.List;

record SerializedSequenceChain<Value_>(
        List<SerializedSequence<Value_>> sequences,
        List<SerializedBreak<Value_>> breaks) {

}
