package ai.timefold.solver.jackson.api.score.stream.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.score.stream.common.Break;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.impl.util.CollectionUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public final class SequenceChainJacksonSerializer<Value_, Difference_ extends Comparable<Difference_>>
        extends JsonSerializer<SequenceChain<Value_, Difference_>> {

    @Override
    public void serialize(SequenceChain<Value_, Difference_> sequenceChain, JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider) throws IOException {
        // Convert breaks to a serializable format.
        var breakCount = sequenceChain.getBreaks().size();
        var breakToSerializedBreakMap =
                CollectionUtils.<Break<Value_, Difference_>, SerializedBreak<Value_>> newIdentityHashMap(breakCount);
        var serializedBreakList = new ArrayList<SerializedBreak<Value_>>(breakCount);
        for (var brk : sequenceChain.getBreaks()) {
            var serializedBreak = SerializedBreak.of(brk);
            serializedBreakList.add(serializedBreak);
            breakToSerializedBreakMap.putIfAbsent(brk, serializedBreak);
        }
        // Convert sequences to a serializable format.
        var serializedSequenceList =
                new ArrayList<SerializedSequence<Value_>>(sequenceChain.getConsecutiveSequences().size());
        for (var sequence : sequenceChain.getConsecutiveSequences()) {
            // Make sure to reuse the same serialized break instances for the same break instances.
            var serializedSequence = new SerializedSequence<>(
                    breakToSerializedBreakMap.get(sequence.getPreviousBreak()),
                    breakToSerializedBreakMap.get(sequence.getNextBreak()),
                    List.copyOf(sequence.getItems()));
            serializedSequenceList.add(serializedSequence);
        }
        jsonGenerator.writeObject(new SerializedSequenceChain<>(serializedSequenceList, serializedBreakList));
    }

}
