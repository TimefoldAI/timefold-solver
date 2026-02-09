package ai.timefold.solver.quarkus.jackson.api.score.stream.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.score.stream.common.SequenceChain;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public final class SequenceChainJacksonSerializer<Value_, Difference_ extends Comparable<Difference_>>
        extends JsonSerializer<SequenceChain<Value_, Difference_>> {

    @Override
    public void serialize(SequenceChain<Value_, Difference_> sequenceChain, JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider) throws IOException {
        var serializedSequenceList =
                new ArrayList<SerializableSequence<Value_>>(sequenceChain.getConsecutiveSequences().size());
        for (var sequence : sequenceChain.getConsecutiveSequences()) {
            var serializedSequence =
                    new SerializableSequence<>(sequence.isFirst(), sequence.isLast(), List.copyOf(sequence.getItems()));
            serializedSequenceList.add(serializedSequence);
        }
        jsonGenerator.writeObject(new SerializableSequenceChain<>(serializedSequenceList));
    }

}
