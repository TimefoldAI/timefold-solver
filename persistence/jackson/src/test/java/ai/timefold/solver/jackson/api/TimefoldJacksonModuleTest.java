package ai.timefold.solver.jackson.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.domain.solution.ConstraintWeightOverrides;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ConstraintAnalysis;
import ai.timefold.solver.core.api.score.analysis.MatchAnalysis;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.constraint.ConstraintRef;
import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.core.api.score.stream.DefaultConstraintJustification;
import ai.timefold.solver.core.api.solver.RecommendedAssignment;
import ai.timefold.solver.core.api.solver.RecommendedFit;
import ai.timefold.solver.core.impl.solver.DefaultRecommendedAssignment;
import ai.timefold.solver.core.impl.solver.DefaultRecommendedFit;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.jackson.api.domain.solution.AbstractConstraintWeightOverridesDeserializer;
import ai.timefold.solver.jackson.api.score.analysis.AbstractScoreAnalysisJacksonDeserializer;
import ai.timefold.solver.jackson.api.solver.AbstractRecommendedAssignmentJacksonDeserializer;
import ai.timefold.solver.jackson.api.solver.AbstractRecommendedFitJacksonDeserializer;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.type.TypeFactory;

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
                .disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                .build();

        var input = new TestTimefoldJacksonModuleWrapper();
        input.setBendableScore(BendableScore.of(new int[] { 1000, 200 }, new int[] { 34 }));
        input.setHardSoftScore(HardSoftScore.of(-1, -20));
        input.setPolymorphicScore(HardSoftScore.of(-20, -300));
        var output = serializeAndDeserialize(objectMapper, input);
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
    void scoreAnalysisWithoutMatches() throws JacksonException {
        var objectMapper = JsonMapper.builder()
                .addModule(TimefoldJacksonModule.createModule())
                .addModule(new CustomJacksonModule())
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
                .build();

        var constraintRef1 = ConstraintRef.of("packageB", "constraint1");
        var constraintRef2 = ConstraintRef.of("packageA", "constraint2");
        var constraintAnalysis1 =
                new ConstraintAnalysis<>(constraintRef1, HardSoftScore.ofSoft(1), HardSoftScore.ofSoft(2), null);
        var constraintAnalysis2 =
                new ConstraintAnalysis<>(constraintRef2, HardSoftScore.ofHard(1), HardSoftScore.ofHard(1), null, 2);
        var originalScoreAnalysis = new ScoreAnalysis<>(HardSoftScore.of(1, 2),
                Map.of(constraintRef1, constraintAnalysis1,
                        constraintRef2, constraintAnalysis2));

        // Hardest constraints first, package name second.
        var serialized = objectMapper.writeValueAsString(originalScoreAnalysis);
        assertThat(serialized)
                .isEqualToIgnoringWhitespace("""
                        {
                           "score" : "1hard/2soft",
                           "initialized" : true,
                           "constraints" : [ {
                             "package" : "packageA",
                             "name" : "constraint2",
                             "weight" : "1hard/0soft",
                             "score" : "1hard/0soft",
                             "matchCount" : 2
                           }, {
                             "package" : "packageB",
                             "name" : "constraint1",
                             "weight" : "0hard/1soft",
                             "score" : "0hard/2soft"
                           } ]
                         }""");

        ScoreAnalysis<HardSoftScore> deserialized = objectMapper.readValue(serialized, ScoreAnalysis.class);
        assertThat(deserialized).isEqualTo(originalScoreAnalysis);
    }

    @Test
    void scoreAnalysisWithMatches() throws JacksonException {
        var objectMapper = JsonMapper.builder()
                .addModule(TimefoldJacksonModule.createModule())
                .addModule(new CustomJacksonModule())
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
                .build();

        var originalScoreAnalysis = getScoreAnalysis();
        var serialized = objectMapper.writeValueAsString(originalScoreAnalysis);
        assertThat(serialized)
                .isEqualToIgnoringWhitespace(getSerializedScoreAnalysis());

        ScoreAnalysis<HardSoftScore> deserialized = objectMapper.readValue(serialized, ScoreAnalysis.class);
        assertThat(deserialized).isEqualTo(originalScoreAnalysis);
    }

    private static ScoreAnalysis<HardSoftScore> getScoreAnalysis() {
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
                new ConstraintAnalysis<>(constraintRef1, HardSoftScore.ofHard(1), HardSoftScore.ofHard(2),
                        List.of(matchAnalysis1, matchAnalysis2));
        var constraintAnalysis2 =
                new ConstraintAnalysis<>(constraintRef2, HardSoftScore.ofSoft(1), HardSoftScore.ofSoft(4),
                        List.of(matchAnalysis3, matchAnalysis4));
        return new ScoreAnalysis<>(HardSoftScore.of(2, 4),
                Map.of(constraintRef1, constraintAnalysis1,
                        constraintRef2, constraintAnalysis2));
    }

    private static String getSerializedScoreAnalysis() {
        return """
                {
                  "score" : "2hard/4soft",
                  "initialized" : true,
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
                        }
                     ],
                    "matchCount" : 2
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
                    } ],
                    "matchCount" : 2
                  } ]
                }""";
    }

    @Test
    void recommendedFit() throws JacksonException {
        var objectMapper = JsonMapper.builder()
                .addModule(TimefoldJacksonModule.createModule())
                .addModule(new CustomJacksonModule())
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
                .build();

        var proposition = new Pair<>("A", "1");
        var originalScoreAnalysis = getScoreAnalysis();
        var originalRecommendedFit = new DefaultRecommendedFit<>(0, proposition, originalScoreAnalysis);
        var fitList = List.of(originalRecommendedFit);

        var serialized = objectMapper.writeValueAsString(fitList);
        assertThat(serialized)
                .isEqualToIgnoringWhitespace("""
                        [ {
                             "proposition" : {
                               "key" : "A",
                               "value" : "1"
                             },
                             "scoreDiff" : %s
                           } ]""".formatted(getSerializedScoreAnalysis()));

        List<RecommendedFit<Pair<String, String>, HardSoftScore>> deserialized =
                objectMapper.readValue(serialized,
                        TypeFactory.createDefaultInstance().constructCollectionType(List.class, RecommendedFit.class));
        assertThat(deserialized)
                .hasSize(1)
                .first()
                .isEqualTo(originalRecommendedFit);
    }

    @Test
    void recommendedAssignment() throws JacksonException {
        var objectMapper = JsonMapper.builder()
                .addModule(TimefoldJacksonModule.createModule())
                .addModule(new CustomJacksonModule())
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
                .build();

        var proposition = new Pair<>("A", "1");
        var originalScoreAnalysis = getScoreAnalysis();
        var originalRecommendedAssignment = new DefaultRecommendedAssignment<>(0, proposition, originalScoreAnalysis);
        var fitList = List.of(originalRecommendedAssignment);

        var serialized = objectMapper.writeValueAsString(fitList);
        assertThat(serialized)
                .isEqualToIgnoringWhitespace("""
                        [ {
                             "proposition" : {
                               "key" : "A",
                               "value" : "1"
                             },
                             "scoreDiff" : %s
                           } ]""".formatted(getSerializedScoreAnalysis()));

        List<RecommendedAssignment<Pair<String, String>, HardSoftScore>> deserialized =
                objectMapper.readValue(serialized,
                        TypeFactory.createDefaultInstance().constructCollectionType(List.class, RecommendedAssignment.class));
        assertThat(deserialized)
                .hasSize(1)
                .first()
                .isEqualTo(originalRecommendedAssignment);
    }

    @Test
    void constraintWeightOverrides() throws JacksonException {
        var objectMapper = JsonMapper.builder()
                .addModule(TimefoldJacksonModule.createModule())
                .addModule(new CustomJacksonModule())
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_DEFAULT))
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

        var deserialized = objectMapper.readValue(serialized, ConstraintWeightOverrides.class);
        assertThat(deserialized).isEqualTo(constraintWeightOverrides);
    }

    public static final class CustomJacksonModule extends SimpleModule {

        public CustomJacksonModule() {
            super("Timefold Custom");
            addDeserializer(ScoreAnalysis.class, new CustomScoreAnalysisJacksonDeserializer());
            addDeserializer(RecommendedFit.class, new CustomRecommendedFitJacksonDeserializer());
            addDeserializer(RecommendedAssignment.class, new CustomRecommendedAssignmentJacksonDeserializer());
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

    public static final class CustomRecommendedFitJacksonDeserializer
            extends AbstractRecommendedFitJacksonDeserializer<Pair<String, String>, HardSoftScore> {

        @Override
        protected Class<Pair<String, String>> getPropositionClass() {
            return (Class) Pair.class;
        }
    }

    public static final class CustomRecommendedAssignmentJacksonDeserializer
            extends AbstractRecommendedAssignmentJacksonDeserializer<Pair<String, String>, HardSoftScore> {

        @Override
        protected Class<Pair<String, String>> getPropositionClass() {
            return (Class) Pair.class;
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
