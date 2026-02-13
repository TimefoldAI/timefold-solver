package ai.timefold.solver.quarkus.jackson.score;

import ai.timefold.solver.core.api.score.SimpleScore;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

class SimpleScoreJacksonRoundTripTest extends AbstractScoreJacksonRoundTripTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestSimpleScoreWrapper(null));
        var score = SimpleScore.of(1234L);
        assertSerializeAndDeserialize(score, new TestSimpleScoreWrapper(score));
    }

    public static class TestSimpleScoreWrapper
            extends AbstractScoreJacksonRoundTripTest.TestScoreWrapper<SimpleScore> {

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
