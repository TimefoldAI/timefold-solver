package ai.timefold.solver.jackson.api.score.stream.common;

import java.util.List;

record SerializableSequenceChain<Value_>(List<SerializableSequence<Value_>> sequences) {

}
