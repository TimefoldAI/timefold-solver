package ai.timefold.solver.jackson3.api.score.stream.common;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.score.stream.common.SequenceChain;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public final class SequenceChainJacksonSerializer<Value_, Difference_ extends Comparable<Difference_>>
        extends ValueSerializer<SequenceChain<Value_, Difference_>> {

    @Override
    public void serialize(SequenceChain<Value_, Difference_> sequenceChain, JsonGenerator jsonGenerator,
            SerializationContext serializerProvider) throws JacksonException {
        var serializedSequenceList =
                new ArrayList<SerializableSequence<Value_>>(sequenceChain.getConsecutiveSequences().size());
        for (var sequence : sequenceChain.getConsecutiveSequences()) {
            var serializedSequence =
                    new SerializableSequence<>(sequence.isFirst(), sequence.isLast(), List.copyOf(sequence.getItems()));
            serializedSequenceList.add(serializedSequence);
        }
        jsonGenerator.writePOJO(new SerializableSequenceChain<>(serializedSequenceList));
    }

}
