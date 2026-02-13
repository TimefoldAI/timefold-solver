package ai.timefold.solver.jaxb.api.score;

import java.math.BigDecimal;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ai.timefold.solver.core.api.score.SimpleBigDecimalScore;

import org.junit.jupiter.api.Test;

class SimpleBigDecimalScoreJaxbAdapterTest extends AbstractScoreJaxbAdapterTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestSimpleBigDecimalScoreWrapper(null));

        var score = SimpleBigDecimalScore.of(new BigDecimal("1234.4321"));
        assertSerializeAndDeserialize(score, new TestSimpleBigDecimalScoreWrapper(score));
    }

    @XmlRootElement
    public static class TestSimpleBigDecimalScoreWrapper extends TestScoreWrapper<SimpleBigDecimalScore> {

        @XmlJavaTypeAdapter(SimpleBigDecimalScoreJaxbAdapter.class)
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
