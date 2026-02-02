package ai.timefold.solver.jackson3.api.score.stream.common;

import java.util.List;

record SerializableSequenceChain<Value_>(List<SerializableSequence<Value_>> sequences) {

}
