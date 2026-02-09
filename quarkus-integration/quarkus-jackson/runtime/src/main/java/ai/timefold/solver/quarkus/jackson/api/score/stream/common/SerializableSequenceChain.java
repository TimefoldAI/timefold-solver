package ai.timefold.solver.quarkus.jackson.api.score.stream.common;

import java.util.List;

record SerializableSequenceChain<Value_>(List<SerializableSequence<Value_>> sequences) {

}
