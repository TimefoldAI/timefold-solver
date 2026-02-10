package ai.timefold.solver.quarkus.jackson.score.stream.common;

import java.util.List;

record SerializableSequenceChain<Value_>(List<SerializableSequence<Value_>> sequences) {

}
