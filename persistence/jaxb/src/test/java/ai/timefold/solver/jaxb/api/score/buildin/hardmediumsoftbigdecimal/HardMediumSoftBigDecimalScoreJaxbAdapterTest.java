package ai.timefold.solver.jaxb.api.score.buildin.hardmediumsoftbigdecimal;

import java.math.BigDecimal;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftbigdecimal.HardMediumSoftBigDecimalScore;
import ai.timefold.solver.jaxb.api.score.AbstractScoreJaxbAdapterTest;

import org.junit.jupiter.api.Test;

class HardMediumSoftBigDecimalScoreJaxbAdapterTest extends AbstractScoreJaxbAdapterTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestHardMediumSoftBigDecimalScoreWrapper(null));

        HardMediumSoftBigDecimalScore score = HardMediumSoftBigDecimalScore.of(new BigDecimal("1200.0021"),
                new BigDecimal("-3.1415"), new BigDecimal("34.4300"));
        assertSerializeAndDeserialize(score, new TestHardMediumSoftBigDecimalScoreWrapper(score));

        score = HardMediumSoftBigDecimalScore.ofUninitialized(-7, new BigDecimal("1200.0021"), new BigDecimal("-3.1415"),
                new BigDecimal("34.4300"));
        assertSerializeAndDeserialize(score, new TestHardMediumSoftBigDecimalScoreWrapper(score));
    }

    @XmlRootElement
    public static class TestHardMediumSoftBigDecimalScoreWrapper extends TestScoreWrapper<HardMediumSoftBigDecimalScore> {

        @XmlJavaTypeAdapter(HardMediumSoftBigDecimalScoreJaxbAdapter.class)
        private HardMediumSoftBigDecimalScore score;

        @SuppressWarnings("unused")
        private TestHardMediumSoftBigDecimalScoreWrapper() {
        }

        public TestHardMediumSoftBigDecimalScoreWrapper(HardMediumSoftBigDecimalScore score) {
            this.score = score;
        }

        @Override
        public HardMediumSoftBigDecimalScore getScore() {
            return score;
        }

    }

}
