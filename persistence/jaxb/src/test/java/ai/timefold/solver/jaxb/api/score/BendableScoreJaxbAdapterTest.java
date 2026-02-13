package ai.timefold.solver.jaxb.api.score;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ai.timefold.solver.core.api.score.BendableScore;

import org.junit.jupiter.api.Test;

class BendableScoreJaxbAdapterTest extends AbstractScoreJaxbAdapterTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestBendableLongScoreWrapper(null));

        var score = BendableScore.of(new long[] { 1000L, 200L }, new long[] { 34L });
        assertSerializeAndDeserialize(score, new TestBendableLongScoreWrapper(score));
    }

    @XmlRootElement
    public static class TestBendableLongScoreWrapper extends TestScoreWrapper<BendableScore> {

        @XmlJavaTypeAdapter(BendableScoreJaxbAdapter.class)
        private BendableScore score;

        @SuppressWarnings("unused")
        private TestBendableLongScoreWrapper() {
        }

        public TestBendableLongScoreWrapper(BendableScore score) {
            this.score = score;
        }

        @Override
        public BendableScore getScore() {
            return score;
        }

    }

}
