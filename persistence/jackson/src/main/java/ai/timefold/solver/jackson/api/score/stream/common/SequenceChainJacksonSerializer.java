package ai.timefold.solver.jackson.api.score.stream.common;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.score.stream.common.SequenceChain;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public final class SequenceChainJacksonSerializer<Value_, Difference_ extends Comparable<Difference_>>
        extends JsonSerializer<SequenceChain<Value_, Difference_>> {

    @Override
    public void serialize(SequenceChain<Value_, Difference_> sequenceChain, JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider) throws IOException {
        var breakToSerializedBreakMap = sequenceChain.getBreaks()
                .stream()
                .collect(Collectors.toMap(b -> b, SerializedBreak::of, (b1, b2) -> b1, IdentityHashMap::new));
        var serializedSequenceList = sequenceChain.getConsecutiveSequences()
                .stream()
                .map(s -> new SerializedSequence<>(breakToSerializedBreakMap.get(s.getPreviousBreak()),
                        breakToSerializedBreakMap.get(s.getNextBreak()),
                        List.copyOf(s.getItems())))
                .toList();
        jsonGenerator.writeObject(new SerializedSequenceChain<>(serializedSequenceList,
                // The map does not maintain consistent insertion order, so we create a new list.
                sequenceChain.getBreaks().stream()
                        .map(breakToSerializedBreakMap::get)
                        .toList()));
    }

}
