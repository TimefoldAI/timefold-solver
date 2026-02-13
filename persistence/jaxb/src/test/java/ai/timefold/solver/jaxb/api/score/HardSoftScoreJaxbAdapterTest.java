package ai.timefold.solver.jaxb.api.score;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ai.timefold.solver.core.api.score.HardSoftScore;

import org.junit.jupiter.api.Test;

class HardSoftScoreJaxbAdapterTest extends AbstractScoreJaxbAdapterTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestHardSoftLongScoreWrapper(null));

        var score = HardSoftScore.of(1200L, 34L);
        assertSerializeAndDeserialize(score, new TestHardSoftLongScoreWrapper(score));
    }

    @XmlRootElement
    public static class TestHardSoftLongScoreWrapper extends TestScoreWrapper<HardSoftScore> {

        @XmlJavaTypeAdapter(HardSoftScoreJaxbAdapter.class)
        private HardSoftScore score;

        @SuppressWarnings("unused")
        private TestHardSoftLongScoreWrapper() {
        }

        public TestHardSoftLongScoreWrapper(HardSoftScore score) {
            this.score = score;
        }

        @Override
        public HardSoftScore getScore() {
            return score;
        }

    }

}
