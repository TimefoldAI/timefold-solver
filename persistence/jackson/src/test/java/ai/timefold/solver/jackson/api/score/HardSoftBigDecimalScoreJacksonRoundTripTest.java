package ai.timefold.solver.jackson.api.score;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.HardSoftBigDecimalScore;
import ai.timefold.solver.jackson.api.score.buildin.HardSoftBigDecimalScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.HardSoftBigDecimalScoreJacksonSerializer;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

class HardSoftBigDecimalScoreJacksonRoundTripTest extends AbstractScoreJacksonRoundTripTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestHardSoftBigDecimalScoreWrapper(null));
        var score = HardSoftBigDecimalScore.of(new BigDecimal("1200.0021"), new BigDecimal("34.4300"));
        assertSerializeAndDeserialize(score, new TestHardSoftBigDecimalScoreWrapper(score));
    }

    public static class TestHardSoftBigDecimalScoreWrapper extends TestScoreWrapper<HardSoftBigDecimalScore> {

        @JsonSerialize(using = HardSoftBigDecimalScoreJacksonSerializer.class)
        @JsonDeserialize(using = HardSoftBigDecimalScoreJacksonDeserializer.class)
        private HardSoftBigDecimalScore score;

        @SuppressWarnings("unused")
        private TestHardSoftBigDecimalScoreWrapper() {
        }

        public TestHardSoftBigDecimalScoreWrapper(HardSoftBigDecimalScore score) {
            this.score = score;
        }

        @Override
        public HardSoftBigDecimalScore getScore() {
            return score;
        }

    }

}
