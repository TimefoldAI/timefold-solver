package ai.timefold.solver.jackson3.api.score.buildin.simplelong;

import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
import ai.timefold.solver.jackson3.api.score.AbstractScoreJacksonRoundTripTest;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

class SimpleLongScoreJacksonRoundTripTest extends AbstractScoreJacksonRoundTripTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestSimpleLongScoreWrapper(null));
        var score = SimpleLongScore.of(1234L);
        assertSerializeAndDeserialize(score, new TestSimpleLongScoreWrapper(score));
    }

    public static class TestSimpleLongScoreWrapper
            extends AbstractScoreJacksonRoundTripTest.TestScoreWrapper<SimpleLongScore> {

        @JsonSerialize(using = SimpleLongScoreJacksonSerializer.class)
        @JsonDeserialize(using = SimpleLongScoreJacksonDeserializer.class)
        private SimpleLongScore score;

        @SuppressWarnings("unused")
        private TestSimpleLongScoreWrapper() {
        }

        public TestSimpleLongScoreWrapper(SimpleLongScore score) {
            this.score = score;
        }

        @Override
        public SimpleLongScore getScore() {
            return score;
        }

    }

}
