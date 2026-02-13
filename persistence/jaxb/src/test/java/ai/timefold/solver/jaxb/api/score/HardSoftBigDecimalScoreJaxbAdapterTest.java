package ai.timefold.solver.jaxb.api.score;

import java.math.BigDecimal;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ai.timefold.solver.core.api.score.HardSoftBigDecimalScore;

import org.junit.jupiter.api.Test;

class HardSoftBigDecimalScoreJaxbAdapterTest extends AbstractScoreJaxbAdapterTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestHardSoftBigDecimalScoreWrapper(null));

        var score = HardSoftBigDecimalScore.of(new BigDecimal("1200.0021"), new BigDecimal("34.4300"));
        assertSerializeAndDeserialize(score, new TestHardSoftBigDecimalScoreWrapper(score));
    }

    @XmlRootElement
    public static class TestHardSoftBigDecimalScoreWrapper extends TestScoreWrapper<HardSoftBigDecimalScore> {

        @XmlJavaTypeAdapter(HardSoftBigDecimalScoreJaxbAdapter.class)
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
