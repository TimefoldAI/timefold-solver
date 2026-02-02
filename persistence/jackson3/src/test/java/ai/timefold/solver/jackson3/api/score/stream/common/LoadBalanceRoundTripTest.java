package ai.timefold.solver.jackson3.api.score.stream.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.api.score.stream.ConstraintCollectors;
import ai.timefold.solver.core.api.score.stream.common.LoadBalance;
import ai.timefold.solver.core.api.score.stream.uni.UniConstraintCollector;
import ai.timefold.solver.jackson3.api.TimefoldJacksonModule;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

class LoadBalanceRoundTripTest {

    @JsonIdentityInfo(scope = Item.class, generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    private record Item(String id) {

    }

    @Test
    void roundTrip() throws JacksonException {
        // Prepare the data to be serialized.
        Item a = new Item("A");
        Item b = new Item("B");
        Item c = new Item("C");
        var collector = (UniConstraintCollector) ConstraintCollectors.loadBalance(Function.identity());
        var context = collector.supplier().get();
        var accumulator = collector.accumulator();
        for (var item : List.of(a, b, c, a, a, b, a)) {
            accumulator.apply(context, item);
        }

        // Retrieve the instance to be serialized.
        var loadBalance = (LoadBalance<Item>) collector.finisher().apply(context);

        ObjectMapper objectMapper = JsonMapper.builder()
                .changeDefaultPropertyInclusion(incl -> incl.withContentInclusion(JsonInclude.Include.NON_NULL)
                        .withValueInclusion(JsonInclude.Include.NON_NULL))
                .addModule(TimefoldJacksonModule.createModule())
                .build();

        assertRoundTrip(objectMapper, loadBalance, """
                {
                    "unfairness":2.16025
                }""");
    }

    private static void assertRoundTrip(ObjectMapper objectMapper, LoadBalance<Item> original,
            String expectedSerialization) throws JacksonException {
        var serialized = objectMapper.writeValueAsString(original);
        assertThat(serialized).isEqualToIgnoringWhitespace(expectedSerialization);

        var deserialized = objectMapper.readValue(serialized, LoadBalance.class);
        assertSoftly(softly -> {
            softly.assertThat(deserialized.unfairness())
                    .isEqualTo(original.unfairness());
            softly.assertThatThrownBy(deserialized::loads)
                    .isInstanceOf(UnsupportedOperationException.class);
        });
    }

}
