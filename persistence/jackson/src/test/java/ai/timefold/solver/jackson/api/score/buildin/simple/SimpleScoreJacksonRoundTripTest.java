package ai.timefold.solver.jackson.api.score.buildin.simple;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.jackson.api.score.AbstractScoreJacksonRoundTripTest;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

class SimpleScoreJacksonRoundTripTest extends AbstractScoreJacksonRoundTripTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestSimpleScoreWrapper(null));
        var score = SimpleScore.of(1234);
        assertSerializeAndDeserialize(score, new TestSimpleScoreWrapper(score));
    }

    public static class TestSimpleScoreWrapper extends TestScoreWrapper<SimpleScore> {

        @JsonSerialize(using = SimpleScoreJacksonSerializer.class)
        @JsonDeserialize(using = SimpleScoreJacksonDeserializer.class)
        private SimpleScore score;

        @SuppressWarnings("unused")
        private TestSimpleScoreWrapper() {
        }

        public TestSimpleScoreWrapper(SimpleScore score) {
            this.score = score;
        }

        @Override
        public SimpleScore getScore() {
            return score;
        }

    }

}
