package ai.timefold.solver.jaxb.api.score.buildin.simple;

import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.jaxb.api.score.AbstractScoreJaxbAdapterTest;

import org.junit.jupiter.api.Test;

class SimpleScoreJaxbAdapterTest extends AbstractScoreJaxbAdapterTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestSimpleScoreWrapper(null));

        var score = SimpleScore.of(1234);
        assertSerializeAndDeserialize(score, new TestSimpleScoreWrapper(score));
    }

    @XmlRootElement
    public static class TestSimpleScoreWrapper extends TestScoreWrapper<SimpleScore> {

        @XmlJavaTypeAdapter(SimpleScoreJaxbAdapter.class)
        private SimpleScore score;

        @SuppressWarnings("unused")
        private TestSimpleScoreWrapper() {
        }

        public TestSimpleScoreWrapper(SimpleScore score) {
            this.score = score;
        }

        @Override
        public SimpleScore getScore() {
            return score;
        }

    }

}
