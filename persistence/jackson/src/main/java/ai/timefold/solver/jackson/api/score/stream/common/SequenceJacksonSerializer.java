package ai.timefold.solver.jackson.api.score.stream.common;

import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.common.Sequence;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Jackson binding support for a {@link Sequence} type.
 * Only necessary if constraints use
 * {@link ConstraintCollectors#toConsecutiveSequences(BiFunction, ToIntFunction) consecutive collector}
 * products in their justifications.
 * <p>
 * It is recommended for the user to implement a custom serializer for the value and difference types.
 * To minimize the resulting JSON,
 * the value serializer should be able to handle reference identity (see {@link JsonIdentityInfo}).
 *
 * @param <Value_> User-provided type of the value in the sequence; must be serializable by Jackson.
 * @param <Difference_> User-provided type of the difference between values in the sequence; must be serializable by Jackson.
 */
public final class SequenceJacksonSerializer<Value_, Difference_ extends Comparable<Difference_>>
        extends JsonSerializer<Sequence<Value_, Difference_>> {

    static final String FIELD_PREVIOUS_BREAK = "previous_break";
    static final String FIELD_NEXT_BREAK = "next_break";
    static final String FIELD_ITEMS = "items";
    static final String FIELD_LENGTH = "length";

    @Override
    public void serialize(Sequence<Value_, Difference_> sequence, JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField(FIELD_ITEMS, sequence.getItems());
        jsonGenerator.writeObjectField(FIELD_LENGTH, sequence.getLength());
        jsonGenerator.writeObjectField(FIELD_PREVIOUS_BREAK, sequence.getPreviousBreak());
        jsonGenerator.writeObjectField(FIELD_NEXT_BREAK, sequence.getNextBreak());
        jsonGenerator.writeEndObject();
    }

}
