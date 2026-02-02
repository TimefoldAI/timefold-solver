package ai.timefold.solver.jackson3.api.score.stream.common;

import java.util.List;

import ai.timefold.solver.core.api.score.stream.common.Sequence;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public final class SequenceJacksonSerializer<Value_, Difference_ extends Comparable<Difference_>>
        extends ValueSerializer<Sequence<Value_, Difference_>> {

    @Override
    public void serialize(Sequence<Value_, Difference_> sequence, JsonGenerator jsonGenerator,
            SerializationContext serializerProvider) throws JacksonException {
        jsonGenerator.writePOJO(
                new SerializableSequence<>(
                        SerializableBreak.of(sequence.getPreviousBreak()),
                        SerializableBreak.of(sequence.getNextBreak()),
                        List.copyOf(sequence.getItems())));
    }

}
