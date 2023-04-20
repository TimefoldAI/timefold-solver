package ai.timefold.solver.xstream.api.score.buildin.simplelong;

import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
import ai.timefold.solver.xstream.api.score.AbstractScoreXStreamConverterTest;

import org.junit.jupiter.api.Test;

import com.thoughtworks.xstream.annotations.XStreamConverter;

class SimpleLongScoreXStreamConverterTest extends AbstractScoreXStreamConverterTest {

    @Test
    void simpleLongScore() {
        assertSerializeAndDeserialize(null, new TestSimpleLongScoreWrapper(null));
        SimpleLongScore score = SimpleLongScore.of(1234L);
        assertSerializeAndDeserialize(score, new TestSimpleLongScoreWrapper(score));
        score = SimpleLongScore.ofUninitialized(-7, 1234L);
        assertSerializeAndDeserialize(score, new TestSimpleLongScoreWrapper(score));
    }

    public static class TestSimpleLongScoreWrapper extends TestScoreWrapper<SimpleLongScore> {

        @XStreamConverter(SimpleLongScoreXStreamConverter.class)
        private SimpleLongScore score;

        public TestSimpleLongScoreWrapper(SimpleLongScore score) {
            this.score = score;
        }

        @Override
        public SimpleLongScore getScore() {
            return score;
        }

    }

}
