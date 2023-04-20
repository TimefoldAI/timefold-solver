package ai.timefold.solver.xstream.api.score.buildin.bendablebigdecimal;

import java.math.BigDecimal;

import ai.timefold.solver.core.api.score.buildin.bendablebigdecimal.BendableBigDecimalScore;
import ai.timefold.solver.xstream.api.score.AbstractScoreXStreamConverterTest;

import org.junit.jupiter.api.Test;

import com.thoughtworks.xstream.annotations.XStreamConverter;

class BendableBigDecimalScoreXStreamConverterTest extends AbstractScoreXStreamConverterTest {

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

        @XStreamConverter(BendableBigDecimalScoreXStreamConverter.class)
        private BendableBigDecimalScore score;

        public TestBendableBigDecimalScoreWrapper(BendableBigDecimalScore score) {
            this.score = score;
        }

        @Override
        public BendableBigDecimalScore getScore() {
            return score;
        }

    }

}
