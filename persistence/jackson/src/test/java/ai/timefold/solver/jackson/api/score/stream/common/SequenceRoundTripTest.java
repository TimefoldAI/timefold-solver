package ai.timefold.solver.jackson.api.score.stream.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.List;

import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.common.Break;
import ai.timefold.solver.core.api.score.stream.common.Sequence;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.core.impl.score.stream.collector.SequenceCalculator;
import ai.timefold.solver.jackson.api.TimefoldJacksonModule;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

class SequenceRoundTripTest {

    @JsonIdentityInfo(scope = Item.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    private record Item(String id, int index) {

    }

    @Test
    void roundTrip() throws JsonProcessingException {
        // Prepare the data to be serialized.
        Item sequence1Item1 = new Item("sequence1Item1", 0);
        Item sequence1Item2 = new Item("sequence1Item2", 1);
        Item sequence2Item1 = new Item("sequence2Item1", 3);
        Item sequence2Item2 = new Item("sequence2Item2", 4);
        Item sequence2Item3 = new Item("sequence2Item3", 5);
        Item sequence3Item1 = new Item("sequence3Item1", 7);
        UniConstraintCollector<Item, SequenceCalculator<Integer>, SequenceChain<Item, Integer>> collector =
                (UniConstraintCollector) ConstraintCollectors.toConsecutiveSequences(Item::index);
        var context = collector.supplier().get();
        var accumulator = collector.accumulator();
        for (var item : List.of(sequence1Item1, sequence1Item2, sequence2Item1, sequence2Item2, sequence2Item3,
                sequence3Item1)) {
            accumulator.apply(context, item);
        }

        // Retrieve the instances to be serialized.
        var sequenceChain = collector.finisher().apply(context);
        var sequences = sequenceChain.getConsecutiveSequences().toArray(new Sequence[0]);
        var breaks = sequenceChain.getBreaks().toArray(new Break[0]);

        ObjectMapper objectMapper = JsonMapper.builder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .serializationInclusion(JsonInclude.Include.NON_NULL)
                .addModule(TimefoldJacksonModule.createModule())
                .build();

        assertSequenceChainRoundTrip(objectMapper, sequenceChain, """
                {
                  "sequences" : [ {
                    "first" : true,
                    "items" : [ {
                      "id" : "sequence1Item1",
                      "index" : 0
                    }, {
                      "id" : "sequence1Item2",
                      "index" : 1
                    } ],
                    "last" : false
                  }, {
                    "first" : false,
                    "items" : [ {
                      "id" : "sequence2Item1",
                      "index" : 3
                    }, {
                      "id" : "sequence2Item2",
                      "index" : 4
                    }, {
                      "id" : "sequence2Item3",
                      "index" : 5
                    } ],
                    "last" : false
                  }, {
                    "first" : false,
                    "items" : [ {
                      "id" : "sequence3Item1",
                      "index" : 7
                    } ],
                    "last" : true
                  } ]
                }""");

        assertSequenceRoundTrip(objectMapper, sequences[0], """
                {
                  "first" : true,
                  "items" : [ {
                    "id" : "sequence1Item1",
                    "index" : 0
                  }, {
                    "id" : "sequence1Item2",
                    "index" : 1
                  } ],
                  "last": false,
                  "next_break" : {
                    "first" : true,
                    "last" : false,
                    "next_sequence_start" : {
                      "id" : "sequence2Item1",
                      "index" : 3
                    },
                    "previous_sequence_end" : "sequence1Item2"
                  }
                }""");
        assertSequenceRoundTrip(objectMapper, sequences[1], """
                {
                  "first" : false,
                  "items" : [ {
                    "id" : "sequence2Item1",
                    "index" : 3
                  }, {
                    "id" : "sequence2Item2",
                    "index" : 4
                  }, {
                    "id" : "sequence2Item3",
                    "index" : 5
                  } ],
                  "last": false,
                  "next_break" : {
                    "first" : false,
                    "last" : true,
                    "next_sequence_start" : {
                      "id" : "sequence3Item1",
                      "index" : 7
                    },
                    "previous_sequence_end" : "sequence2Item3"
                  },
                  "previous_break" : {
                    "first" : true,
                    "last" : false,
                    "next_sequence_start" : "sequence2Item1",
                    "previous_sequence_end" : {
                      "id" : "sequence1Item2",
                      "index" : 1
                    }
                  }
                }""");
        assertSequenceRoundTrip(objectMapper, sequences[2], """
                {
                  "first" : false,
                  "items" : [ {
                    "id" : "sequence3Item1",
                    "index" : 7
                  } ],
                  "last": true,
                  "previous_break" : {
                    "first" : false,
                    "last" : true,
                    "next_sequence_start" : "sequence3Item1",
                    "previous_sequence_end" : {
                      "id" : "sequence2Item3",
                      "index" : 5
                    }
                  }
                }""");

        assertBreakRoundTrip(objectMapper, breaks[0], """
                {
                  "first" : true,
                  "last" : false,
                  "next_sequence_start" : {
                    "id" : "sequence2Item1",
                    "index" : 3
                  },
                  "previous_sequence_end" : {
                    "id" : "sequence1Item2",
                    "index" : 1
                  }
                }""");
        assertBreakRoundTrip(objectMapper, breaks[1], """
                {
                  "first" : false,
                  "last" : true,
                  "next_sequence_start" : {
                    "id" : "sequence3Item1",
                    "index" : 7
                  },
                  "previous_sequence_end" : {
                    "id" : "sequence2Item3",
                    "index" : 5
                  }
                }""");
    }

    private static void assertSequenceChainRoundTrip(ObjectMapper objectMapper, SequenceChain<?, ?> original,
            String expectedSerialization) throws JsonProcessingException {
        var serialized = objectMapper.writeValueAsString(original);
        assertThat(serialized).isEqualToIgnoringWhitespace(expectedSerialization);

        var deserialized = objectMapper.readValue(serialized, SequenceChain.class);
        assertSoftly(softly -> {
            softly.assertThat(deserialized.getConsecutiveSequences())
                    .hasSize(original.getConsecutiveSequences().size());
            softly.assertThatThrownBy(deserialized::getBreaks)
                    .isInstanceOf(UnsupportedOperationException.class);
        });
    }

    private static void assertSequenceRoundTrip(ObjectMapper objectMapper, Sequence<?, ?> original,
            String expectedSerialization) throws JsonProcessingException {
        var serialized = objectMapper.writeValueAsString(original);
        assertThat(serialized).isEqualToIgnoringWhitespace(expectedSerialization);

        var deserialized = objectMapper.readValue(serialized, Sequence.class);
        assertSoftly(softly -> {
            softly.assertThat(deserialized.getItems()).hasSize(original.getItems().size());
            softly.assertThat(deserialized.getCount()).isEqualTo(original.getCount());
            if (original.isFirst()) {
                softly.assertThat(deserialized.isFirst()).isEqualTo(true);
                softly.assertThat(deserialized.getPreviousBreak()).isNull();
            } else {
                softly.assertThat(deserialized.isFirst()).isEqualTo(false);
                softly.assertThat(deserialized.getPreviousBreak()).isNotNull();
            }
            if (original.isLast()) {
                softly.assertThat(deserialized.isLast()).isEqualTo(true);
                softly.assertThat(deserialized.getNextBreak()).isNull();
            } else {
                softly.assertThat(deserialized.isLast()).isEqualTo(false);
                softly.assertThat(deserialized.getNextBreak()).isNotNull();
            }
        });
    }

    private static void assertBreakRoundTrip(ObjectMapper objectMapper, Break<?, ?> original, String expectedSerialization)
            throws JsonProcessingException {
        var serialized = objectMapper.writeValueAsString(original);
        assertThat(serialized).isEqualToIgnoringWhitespace(expectedSerialization);

        var deserialized = objectMapper.readValue(serialized, Break.class);
        assertSoftly(softly -> {
            softly.assertThat(deserialized.isFirst()).isEqualTo(original.isFirst());
            softly.assertThat(deserialized.isLast()).isEqualTo(original.isLast());
            softly.assertThat(deserialized.getPreviousSequenceEnd()).isNotNull();
            softly.assertThat(deserialized.getNextSequenceStart()).isNotNull();
        });
    }

}