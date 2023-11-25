package ai.timefold.solver.jackson.api.score.stream.common;

import static ai.timefold.solver.jackson.api.score.stream.common.BreakJacksonSerializer.FIELD_FIRST;
import static ai.timefold.solver.jackson.api.score.stream.common.BreakJacksonSerializer.FIELD_LAST;
import static ai.timefold.solver.jackson.api.score.stream.common.BreakJacksonSerializer.FIELD_LENGTH;
import static ai.timefold.solver.jackson.api.score.stream.common.BreakJacksonSerializer.FIELD_NEXT_SEQUENCE_START;
import static ai.timefold.solver.jackson.api.score.stream.common.BreakJacksonSerializer.FIELD_PREVIOUS_SEQUENCE_END;

import java.io.IOException;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.common.Break;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Extend this to implement {@link Break} deserialization specific for your domain.
 * Only necessary if constraints use
 * {@link ConstraintCollectors#toConsecutiveSequences(BiFunction, ToIntFunction) consecutive collector}
 * products in their justifications.
 *
 * @param <Value_> User-provided type of the value in the sequence; must be serializable by Jackson.
 * @param <Difference_> User-provided type of the difference between values in the sequence; must be serializable by Jackson.
 */
public abstract class AbstractBreakJacksonDeserializer<Value_, Difference_ extends Comparable<Difference_>>
        extends JsonDeserializer<Break<Value_, Difference_>> {

    @Override
    public Break<Value_, Difference_> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        JsonNode node = jsonParser.readValueAsTree();
        var isFirst = node.get(FIELD_FIRST).asBoolean();
        var isLast = node.get(FIELD_LAST).asBoolean();
        var previousSequenceEnd =
                deserializationContext.readTreeAsValue(node.get(FIELD_PREVIOUS_SEQUENCE_END), getValueClass());
        var nextSequenceStart = deserializationContext.readTreeAsValue(node.get(FIELD_NEXT_SEQUENCE_START), getValueClass());
        var length = deserializationContext.readTreeAsValue(node.get(FIELD_LENGTH), getDifferenceClass());
        return new JacksonBreakImpl<>(previousSequenceEnd, nextSequenceStart, length, isFirst, isLast);
    }

    protected abstract Class<Value_> getValueClass();

    protected abstract Class<Difference_> getDifferenceClass();

    private record JacksonBreakImpl<Value_, Difference_ extends Comparable<Difference_>>(Value_ previousSequenceEnd,
            Value_ nextSequenceStart, Difference_ length, boolean first, boolean last)
            implements
                Break<Value_, Difference_> {

        @Override
        public boolean isFirst() {
            return first();
        }

        @Override
        public boolean isLast() {
            return last();
        }

        @Override
        public Value_ getPreviousSequenceEnd() {
            return previousSequenceEnd();
        }

        @Override
        public Value_ getNextSequenceStart() {
            return nextSequenceStart();
        }

        @Override
        public Difference_ getLength() {
            return length;
        }

    }

}
