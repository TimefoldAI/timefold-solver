package ai.timefold.solver.jackson.api.score;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.jackson.api.score.buildin.SimpleScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.SimpleScoreJacksonSerializer;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

class SimpleScoreJacksonRoundTripTest extends AbstractScoreJacksonRoundTripTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestSimpleLongScoreWrapper(null));
        var score = SimpleScore.of(1234L);
        assertSerializeAndDeserialize(score, new TestSimpleLongScoreWrapper(score));
    }

    public static class TestSimpleLongScoreWrapper
            extends AbstractScoreJacksonRoundTripTest.TestScoreWrapper<SimpleScore> {

        @JsonSerialize(using = SimpleScoreJacksonSerializer.class)
        @JsonDeserialize(using = SimpleScoreJacksonDeserializer.class)
        private SimpleScore score;

        @SuppressWarnings("unused")
        private TestSimpleLongScoreWrapper() {
        }

        public TestSimpleLongScoreWrapper(SimpleScore score) {
            this.score = score;
        }

        @Override
        public SimpleScore getScore() {
            return score;
        }

    }

}
