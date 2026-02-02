package ai.timefold.solver.jackson3.api.score.buildin.bendablelong;

import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.jackson3.api.score.AbstractScoreJacksonRoundTripTest;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

class BendableLongScoreJacksonRoundTripTest extends AbstractScoreJacksonRoundTripTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestBendableLongScoreWrapper(null));
        var score = BendableLongScore.of(new long[] { 1000L, 200L }, new long[] { 34L });
        assertSerializeAndDeserialize(score, new TestBendableLongScoreWrapper(score));
    }

    public static class TestBendableLongScoreWrapper extends TestScoreWrapper<BendableLongScore> {

        @JsonSerialize(using = BendableLongScoreJacksonSerializer.class)
        @JsonDeserialize(using = BendableLongScoreJacksonDeserializer.class)
        private BendableLongScore score;

        @SuppressWarnings("unused")
        private TestBendableLongScoreWrapper() {
        }

        public TestBendableLongScoreWrapper(BendableLongScore score) {
            this.score = score;
        }

        @Override
        public BendableLongScore getScore() {
            return score;
        }

    }

}
