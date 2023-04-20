package ai.timefold.solver.jackson.api.score.buildin.bendablebigdecimal;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore;
import ai.timefold.solver.jackson.api.score.AbstractScoreJacksonRoundTripTest;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

class BendableBigDecimalScoreJacksonRoundTripTest extends AbstractScoreJacksonRoundTripTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestBendableBigDecimalScoreWrapper(null));
        BendableBigDecimalScore score = BendableBigDecimalScore.of(
                new BigDecimal[] { new BigDecimal("1000.0001"), new BigDecimal("200.0020") },
                new BigDecimal[] { new BigDecimal("34.4300") });
        assertSerializeAndDeserialize(score, new TestBendableBigDecimalScoreWrapper(score));
        score = BendableBigDecimalScore.ofUninitialized(-7,
                new BigDecimal[] { new BigDecimal("1000.0001"), new BigDecimal("200.0020") },
                new BigDecimal[] { new BigDecimal("34.4300") });
        assertSerializeAndDeserialize(score, new TestBendableBigDecimalScoreWrapper(score));
    }

    public static class TestBendableBigDecimalScoreWrapper extends TestScoreWrapper<BendableBigDecimalScore> {

        @JsonSerialize(using = BendableBigDecimalScoreJacksonSerializer.class)
        @JsonDeserialize(using = BendableBigDecimalScoreJacksonDeserializer.class)
        private BendableBigDecimalScore score;

        @SuppressWarnings("unused")
        private TestBendableBigDecimalScoreWrapper() {
        }

        public TestBendableBigDecimalScoreWrapper(BendableBigDecimalScore score) {
            this.score = score;
        }

        @Override
        public BendableBigDecimalScore getScore() {
            return score;
        }

    }

}
