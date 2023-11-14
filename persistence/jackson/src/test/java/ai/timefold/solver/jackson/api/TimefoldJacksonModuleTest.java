package ai.timefold.solver.jackson.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ConstraintAnalysis;
import ai.timefold.solver.core.api.score.analysis.MatchAnalysis;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.jackson.api.score.analysis.AbstractScoreAnalysisJacksonDeserializer;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

class TimefoldJacksonModuleTest extends AbstractJacksonRoundTripTest {

    /**
     * According to official specification (see {@link Class#getDeclaredMethods()}),
     * "The elements in the returned array are not sorted and are not in any particular order."
     * Enabling {@link MapperFeature#SORT_PROPERTIES_ALPHABETICALLY} makes this test work on all JDK implementations.
     */
    @Test
    void polymorphicScore() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
        objectMapper.registerModule(TimefoldJacksonModule.createModule());

        TestTimefoldJacksonModuleWrapper input = new TestTimefoldJacksonModuleWrapper();
        input.setBendableScore(BendableScore.of(new int[] { 1000, 200 }, new int[] { 34 }));
        input.setHardSoftScore(HardSoftScore.of(-1, -20));
        input.setPolymorphicScore(HardSoftScore.of(-20, -300));
        TestTimefoldJacksonModuleWrapper output = serializeAndDeserialize(objectMapper, input);
        assertThat(output.getBendableScore()).isEqualTo(BendableScore.of(new int[] { 1000, 200 }, new int[] { 34 }));
        assertThat(output.getHardSoftScore()).isEqualTo(HardSoftScore.of(-1, -20));
        assertThat(output.getPolymorphicScore()).isEqualTo(HardSoftScore.of(-20, -300));

        input.setPolymorphicScore(BendableScore.of(new int[] { -1, -20 }, new int[] { -300, -4000, -50000 }));
        output = serializeAndDeserialize(objectMapper, input);
        assertThat(output.getBendableScore()).isEqualTo(BendableScore.of(new int[] { 1000, 200 }, new int[] { 34 }));
        assertThat(output.getHardSoftScore()).isEqualTo(HardSoftScore.of(-1, -20));
        assertThat(output.getPolymorphicScore())
                .isEqualTo(BendableScore.of(new int[] { -1, -20 }, new int[] { -300, -4000, -50000 }));
    }

    @Test
    void scoreAnalysisWithoutMatches() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
        objectMapper.registerModule(TimefoldJacksonModule.createModule());

        var constraintRef1 = ConstraintRef.of("package1", "constraint1");
        var constraintRef2 = ConstraintRef.of("package2", "constraint2");
        var constraintAnalysis1 = new ConstraintAnalysis<>(constraintRef1, HardSoftScore.ofHard(1), HardSoftScore.ofHard(1), null);
        var constraintAnalysis2 = new ConstraintAnalysis<>(constraintRef2, HardSoftScore.ofSoft(1), HardSoftScore.ofSoft(2), null);
        var originalScoreAnalysis = new ScoreAnalysis<>(HardSoftScore.of(1, 2),
                Map.of(constraintRef1, constraintAnalysis1,
                        constraintRef2, constraintAnalysis2));

        var serialized = objectMapper.writeValueAsString(originalScoreAnalysis);
        Assertions.assertThat(serialized)
                .isEqualToIgnoringWhitespace("""
                        {
                           "score" : "1hard/2soft",
                           "constraints" : [ {
                             "package" : "package1",
                             "name" : "constraint1",
                             "weight" : "1hard/0soft",
                             "score" : "1hard/0soft"
                           }, {
                             "package" : "package2",
                             "name" : "constraint2",
                             "weight" : "0hard/1soft",
                             "score" : "0hard/2soft"
                           } ]
                         }""");

        objectMapper.registerModule(new CustomJacksonModule());
        ScoreAnalysis<HardSoftScore> deserialized = objectMapper.readValue(serialized, ScoreAnalysis.class);
        Assertions.assertThat(deserialized).isEqualTo(originalScoreAnalysis);
    }

    @Test
    void scoreAnalysisWithMatches() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT);
        objectMapper.registerModule(TimefoldJacksonModule.createModule());

        var constraintRef1 = ConstraintRef.of("package1", "constraint1");
        var constraintRef2 = ConstraintRef.of("package2", "constraint2");
        var matchAnalysis1 = new MatchAnalysis<>(constraintRef1, HardSoftScore.ofHard(1),
                DefaultConstraintJustification.of(HardSoftScore.ofHard(1), "A", "B"));
        var matchAnalysis2 = new MatchAnalysis<>(constraintRef1, HardSoftScore.ofHard(1),
                DefaultConstraintJustification.of(HardSoftScore.ofHard(1), "B", "C", "D"));
        var matchAnalysis3 = new MatchAnalysis<>(constraintRef2, HardSoftScore.ofSoft(1),
                DefaultConstraintJustification.of(HardSoftScore.ofSoft(1), "D"));
        var matchAnalysis4 = new MatchAnalysis<>(constraintRef2, HardSoftScore.ofSoft(3),
                DefaultConstraintJustification.of(HardSoftScore.ofSoft(3), "A", "C"));
        var constraintAnalysis1 =
                new ConstraintAnalysis<>(constraintRef1, HardSoftScore.ofHard(1), HardSoftScore.ofHard(2), List.of(matchAnalysis1, matchAnalysis2));
        var constraintAnalysis2 =
                new ConstraintAnalysis<>(constraintRef2, HardSoftScore.ofSoft(1), HardSoftScore.ofSoft(4), List.of(matchAnalysis3, matchAnalysis4));
        var originalScoreAnalysis = new ScoreAnalysis<>(HardSoftScore.of(2, 4),
                Map.of(constraintRef1, constraintAnalysis1,
                        constraintRef2, constraintAnalysis2));

        var serialized = objectMapper.writeValueAsString(originalScoreAnalysis);
        Assertions.assertThat(serialized)
                .isEqualToIgnoringWhitespace("""
                        {
                            "score" : "2hard/4soft",
                            "constraints" : [ {
                              "package" : "package1",
                              "name" : "constraint1",
                              "weight" : "1hard/0soft",
                              "score" : "2hard/0soft",
                              "matches" : [ {
                                "score" : "1hard/0soft",
                                "justification" : [ "A", "B" ]
                              }, {
                                "score" : "1hard/0soft",
                                "justification" : [ "B", "C", "D" ]
                              } ]
                            }, {
                              "package" : "package2",
                              "name" : "constraint2",
                              "weight" : "0hard/1soft",
                              "score" : "0hard/4soft",
                              "matches" : [ {
                                "score" : "0hard/1soft",
                                "justification" : [ "D" ]
                              }, {
                                "score" : "0hard/3soft",
                                "justification" : [ "A", "C" ]
                              } ]
                            } ]
                          }""");

        objectMapper.registerModule(new CustomJacksonModule());
        ScoreAnalysis<HardSoftScore> deserialized = objectMapper.readValue(serialized, ScoreAnalysis.class);
        Assertions.assertThat(deserialized).isEqualTo(originalScoreAnalysis);
    }

    public static final class CustomJacksonModule extends SimpleModule {

        public CustomJacksonModule() {
            super("Timefold Custom");
            addDeserializer(ScoreAnalysis.class, new CustomScoreAnalysisJacksonDeserializer());
        }

    }

    public static final class CustomScoreAnalysisJacksonDeserializer
            extends AbstractScoreAnalysisJacksonDeserializer<HardSoftScore> {

        @Override
        protected HardSoftScore parseScore(String scoreString) {
            return HardSoftScore.parseScore(scoreString);
        }

        @Override
        protected <ConstraintJustification_ extends ConstraintJustification> ConstraintJustification_
                parseConstraintJustification(ConstraintRef constraintRef, String constraintJustificationString,
                        HardSoftScore score) {
            List<Object> justificationList = Arrays.stream(constraintJustificationString.split(","))
                    .map(s -> s.replace("[", "")
                            .replace("]", "")
                            .replace("\"", "")
                            .strip())
                    .collect(Collectors.toList());
            return (ConstraintJustification_) DefaultConstraintJustification.of(score, justificationList);
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
