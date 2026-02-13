package ai.timefold.solver.jackson.api.score;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.SimpleBigDecimalScore;
import ai.timefold.solver.jackson.api.score.buildin.SimpleBigDecimalScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.SimpleBigDecimalScoreJacksonSerializer;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

class SimpleBigDecimalScoreJacksonRoundTripTest extends AbstractScoreJacksonRoundTripTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestSimpleBigDecimalScoreWrapper(null));
        var score = SimpleBigDecimalScore.of(new BigDecimal("1234.4321"));
        assertSerializeAndDeserialize(score, new TestSimpleBigDecimalScoreWrapper(score));
    }

    public static class TestSimpleBigDecimalScoreWrapper extends TestScoreWrapper<SimpleBigDecimalScore> {

        @JsonSerialize(using = SimpleBigDecimalScoreJacksonSerializer.class)
        @JsonDeserialize(using = SimpleBigDecimalScoreJacksonDeserializer.class)
        private SimpleBigDecimalScore score;

        @SuppressWarnings("unused")
        private TestSimpleBigDecimalScoreWrapper() {
        }

        public TestSimpleBigDecimalScoreWrapper(SimpleBigDecimalScore score) {
            this.score = score;
        }

        @Override
        public SimpleBigDecimalScore getScore() {
            return score;
        }

    }

}
