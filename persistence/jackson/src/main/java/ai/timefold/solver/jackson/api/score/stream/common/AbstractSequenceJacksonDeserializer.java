package ai.timefold.solver.jackson.api.score.stream.common;

import static ai.timefold.solver.jackson.api.score.stream.common.SequenceJacksonSerializer.FIELD_ITEMS;
import static ai.timefold.solver.jackson.api.score.stream.common.SequenceJacksonSerializer.FIELD_LENGTH;
import static ai.timefold.solver.jackson.api.score.stream.common.SequenceJacksonSerializer.FIELD_NEXT_BREAK;
import static ai.timefold.solver.jackson.api.score.stream.common.SequenceJacksonSerializer.FIELD_PREVIOUS_BREAK;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.common.Break;
import ai.timefold.solver.core.api.score.stream.common.Sequence;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.type.TypeFactory;

/**
 * Extend this to implement {@link Sequence} deserialization specific for your domain.
 * Only necessary if constraints use
 * {@link ConstraintCollectors#toConsecutiveSequences(BiFunction, ToIntFunction) consecutive collector}
 * products in their justifications.
 *
 * @param <Value_> User-provided type of the value in the sequence; must be serializable by Jackson.
 * @param <Difference_> User-provided type of the difference between values in the sequence; must be serializable by Jackson.
 * @see AbstractBreakJacksonDeserializer Used to deserialize {@link Break} objects,
 *      which are included in {@link Sequence} objects.
 */
public abstract class AbstractSequenceJacksonDeserializer<Value_, Difference_ extends Comparable<Difference_>>
        extends JsonDeserializer<Sequence<Value_, Difference_>> {

    @Override
    public Sequence<Value_, Difference_> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        JsonNode node = jsonParser.readValueAsTree();
        Break<Value_, Difference_> previousSequenceEnd =
                deserializationContext.readTreeAsValue(node.get(FIELD_PREVIOUS_BREAK), Break.class);
        Break<Value_, Difference_> nextSequenceStart =
                deserializationContext.readTreeAsValue(node.get(FIELD_NEXT_BREAK), Break.class);
        List<Value_> items = deserializationContext.readTreeAsValue(node.get(FIELD_ITEMS),
                TypeFactory.defaultInstance().constructCollectionType(List.class, getValueClass()));
        Difference_ length = deserializationContext.readTreeAsValue(node.get(FIELD_LENGTH), getDifferenceClass());
        return new JacksonSequenceImpl<>(previousSequenceEnd, nextSequenceStart, items, length);
    }

    protected abstract Class<Value_> getValueClass();

    protected abstract Class<Difference_> getDifferenceClass();

    private record JacksonSequenceImpl<Value_, Difference_ extends Comparable<Difference_>>(
            Break<Value_, Difference_> previousBreak, Break<Value_, Difference_> nextBreak, Difference_ length,
            Collection<Value_> items, Value_ firstItem, Value_ lastItem,
            int itemCount) implements Sequence<Value_, Difference_> {

        public JacksonSequenceImpl(Break<Value_, Difference_> previousBreak, Break<Value_, Difference_> nextBreak,
                List<Value_> items, Difference_ length) {
            this(previousBreak, nextBreak, length, items,
                    items == null || items.isEmpty() ? null : items.get(0),
                    items == null || items.isEmpty() ? null : items.get(items.size() - 1),
                    items == null ? 0 : items.size());
        }

        @Override
        public Collection<Value_> getItems() {
            return items();
        }

        @Override
        public int getCount() {
            return itemCount();
        }

        @Override
        public Difference_ getLength() {
            return length();
        }

        @Override
        public Value_ getFirstItem() {
            return firstItem();
        }

        @Override
        public Value_ getLastItem() {
            return lastItem();
        }

        @Override
        public boolean isFirst() {
            return previousBreak() == null;
        }

        @Override
        public boolean isLast() {
            return nextBreak() == null;
        }

        @Override
        public Break<Value_, Difference_> getPreviousBreak() {
            return previousBreak();
        }

        @Override
        public Break<Value_, Difference_> getNextBreak() {
            return nextBreak();
        }

    }

}
