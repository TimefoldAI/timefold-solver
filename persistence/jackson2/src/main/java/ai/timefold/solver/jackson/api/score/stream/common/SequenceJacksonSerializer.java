package ai.timefold.solver.jackson.api.score.stream.common;

import java.io.IOException;
import java.util.List;

import ai.timefold.solver.core.api.score.stream.common.Sequence;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public final class SequenceJacksonSerializer<Value_, Difference_ extends Comparable<Difference_>>
        extends JsonSerializer<Sequence<Value_, Difference_>> {

    @Override
    public void serialize(Sequence<Value_, Difference_> sequence, JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeObject(
                new SerializableSequence<>(
                        SerializableBreak.of(sequence.getPreviousBreak()),
                        SerializableBreak.of(sequence.getNextBreak()),
                        List.copyOf(sequence.getItems())));
    }

}
