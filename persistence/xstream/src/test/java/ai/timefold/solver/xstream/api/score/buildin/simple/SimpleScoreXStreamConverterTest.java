package ai.timefold.solver.xstream.api.score.buildin.simple;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.xstream.api.score.AbstractScoreXStreamConverterTest;

import org.junit.jupiter.api.Test;

import com.thoughtworks.xstream.annotations.XStreamConverter;

class SimpleScoreXStreamConverterTest extends AbstractScoreXStreamConverterTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestSimpleScoreWrapper(null));
        SimpleScore score = SimpleScore.of(1234);
        assertSerializeAndDeserialize(score, new TestSimpleScoreWrapper(score));
        score = SimpleScore.ofUninitialized(-7, 1234);
        assertSerializeAndDeserialize(score, new TestSimpleScoreWrapper(score));
    }

    public static class TestSimpleScoreWrapper extends TestScoreWrapper<SimpleScore> {

        @XStreamConverter(SimpleScoreXStreamConverter.class)
        private SimpleScore score;

        public TestSimpleScoreWrapper(SimpleScore score) {
            this.score = score;
        }

        @Override
        public SimpleScore getScore() {
            return score;
        }

    }

}
