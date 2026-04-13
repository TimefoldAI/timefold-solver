package ai.timefold.solver.quarkus.jackson;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.BendableScore;
import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.quarkus.jackson.domain.solution.AbstractConstraintWeightOverridesDeserializer;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

class TimefoldJacksonModuleTest extends AbstractJacksonRoundTripTest {

    /**
     * According to official specification (see {@link Class#getDeclaredMethods()}),
     * "The elements in the returned array are not sorted and are not in any particular order."
     * Enabling {@link MapperFeature#SORT_PROPERTIES_ALPHABETICALLY} makes this test work on all JDK implementations.
     */
    @Test
    void polymorphicScore() {
        var objectMapper = JsonMapper.builder()
                .addModule(TimefoldJacksonModule.createModule())
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .build();

        var input = new TestTimefoldJacksonModuleWrapper();
        input.setBendableScore(BendableScore.of(new long[] { 1000, 200 }, new long[] { 34 }));
        input.setHardSoftScore(HardSoftScore.of(-1, -20));
        input.setPolymorphicScore(HardSoftScore.of(-20, -300));
        var output = serializeAndDeserialize(objectMapper, input);
        assertThat(output.getBendableScore()).isEqualTo(BendableScore.of(new long[] { 1000, 200 }, new long[] { 34 }));
        assertThat(output.getHardSoftScore()).isEqualTo(HardSoftScore.of(-1, -20));
        assertThat(output.getPolymorphicScore()).isEqualTo(HardSoftScore.of(-20, -300));

        input.setPolymorphicScore(BendableScore.of(new long[] { -1, -20 }, new long[] { -300, -4000, -50000 }));
        output = serializeAndDeserialize(objectMapper, input);
        assertThat(output.getBendableScore()).isEqualTo(BendableScore.of(new long[] { 1000, 200 }, new long[] { 34 }));
        assertThat(output.getHardSoftScore()).isEqualTo(HardSoftScore.of(-1, -20));
        assertThat(output.getPolymorphicScore())
                .isEqualTo(BendableScore.of(new long[] { -1, -20 }, new long[] { -300, -4000, -50000 }));
    }

    @Test
    void constraintWeightOverrides() throws JsonProcessingException {
        var objectMapper = JsonMapper.builder()
                .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
                .defaultPropertyInclusion(
                        JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL))
                .addModule(TimefoldJacksonModule.createModule())
                .build();

        var constraintWeightOverrides = ConstraintWeightOverrides.of(
                Map.of(
                        "constraint1", HardSoftScore.ofHard(1),
                        "constraint2", HardSoftScore.ofSoft(2)));

        var serialized = objectMapper.writeValueAsString(constraintWeightOverrides);
        assertThat(serialized)
                .isEqualToIgnoringWhitespace("""
                        {
                            "constraint1":"1hard/0soft",
                            "constraint2":"0hard/2soft"
                        }""");

        objectMapper.registerModule(new CustomJacksonModule());
        var deserialized = objectMapper.readValue(serialized, ConstraintWeightOverrides.class);
        assertThat(deserialized).isEqualTo(constraintWeightOverrides);
    }

    public static final class CustomJacksonModule extends SimpleModule {

        public CustomJacksonModule() {
            super("Timefold Custom");
            addDeserializer(ConstraintWeightOverrides.class, new CustomConstraintWeightOverridesDeserializer());
        }

    }

    public static final class CustomConstraintWeightOverridesDeserializer
            extends AbstractConstraintWeightOverridesDeserializer<HardSoftScore> {

        @Override
        protected HardSoftScore parseScore(String scoreString) {
            return HardSoftScore.parseScore(scoreString);
        }

    }

    public static class TestTimefoldJacksonModuleWrapper {

        private BendableScore bendableScore;
        private HardSoftScore hardSoftScore;
        private Score polymorphicScore;

        @SuppressWarnings("unused")
        private TestTimefoldJacksonModuleWrapper() {
        }

        public BendableScore getBendableScore() {
            return bendableScore;
        }

        public void setBendableScore(BendableScore bendableScore) {
            this.bendableScore = bendableScore;
        }

        public HardSoftScore getHardSoftScore() {
            return hardSoftScore;
        }

        public void setHardSoftScore(HardSoftScore hardSoftScore) {
            this.hardSoftScore = hardSoftScore;
        }

        public Score getPolymorphicScore() {
            return polymorphicScore;
        }

        public void setPolymorphicScore(Score polymorphicScore) {
            this.polymorphicScore = polymorphicScore;
        }

    }

}
