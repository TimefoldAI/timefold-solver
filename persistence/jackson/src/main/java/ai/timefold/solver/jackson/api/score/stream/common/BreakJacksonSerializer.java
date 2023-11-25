package ai.timefold.solver.jackson.api.score.stream.common;

import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.common.Break;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Jackson binding support for a {@link Break} type.
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
public final class BreakJacksonSerializer<Value_, Difference_ extends Comparable<Difference_>>
        extends JsonSerializer<Break<Value_, Difference_>> {

    static final String FIELD_PREVIOUS_SEQUENCE_END = "previous_sequence_end";
    static final String FIELD_NEXT_SEQUENCE_START = "next_sequence_start";
    static final String FIELD_LENGTH = "length";
    static final String FIELD_FIRST = "first";
    static final String FIELD_LAST = "last";

    @Override
    public void serialize(Break<Value_, Difference_> brk, JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeObjectField(FIELD_LENGTH, brk.getLength());
        jsonGenerator.writeBooleanField(FIELD_FIRST, brk.isFirst());
        jsonGenerator.writeBooleanField(FIELD_LAST, brk.isLast());
        jsonGenerator.writeObjectField(FIELD_PREVIOUS_SEQUENCE_END, brk.getPreviousSequenceEnd());
        jsonGenerator.writeObjectField(FIELD_NEXT_SEQUENCE_START, brk.getNextSequenceStart());
        jsonGenerator.writeEndObject();
    }

}
