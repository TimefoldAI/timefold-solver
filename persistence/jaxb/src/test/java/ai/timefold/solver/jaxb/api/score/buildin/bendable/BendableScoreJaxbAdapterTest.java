package ai.timefold.solver.jaxb.api.score.buildin.bendable;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.jaxb.api.score.AbstractScoreJaxbAdapterTest;

import org.junit.jupiter.api.Test;

class BendableScoreJaxbAdapterTest extends AbstractScoreJaxbAdapterTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestBendableScoreWrapper(null));

        var score = BendableScore.of(new int[] { 1000, 200 }, new int[] { 34 });
        assertSerializeAndDeserialize(score, new TestBendableScoreWrapper(score));
    }

    @XmlRootElement
    public static class TestBendableScoreWrapper extends TestScoreWrapper<BendableScore> {

        @XmlJavaTypeAdapter(BendableScoreJaxbAdapter.class)
        private BendableScore score;

        @SuppressWarnings("unused")
        private TestBendableScoreWrapper() {
        }

        public TestBendableScoreWrapper(BendableScore score) {
            this.score = score;
        }

        @Override
        public BendableScore getScore() {
            return score;
        }

    }

}
