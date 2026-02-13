package ai.timefold.solver.jackson.api.score;

import ai.timefold.solver.core.api.score.BendableScore;
import ai.timefold.solver.jackson.api.score.buildin.BendableScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.BendableScoreJacksonSerializer;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

class BendableScoreJacksonRoundTripTest extends AbstractScoreJacksonRoundTripTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestBendableLongScoreWrapper(null));
        var score = BendableScore.of(new long[] { 1000L, 200L }, new long[] { 34L });
        assertSerializeAndDeserialize(score, new TestBendableLongScoreWrapper(score));
    }

    public static class TestBendableLongScoreWrapper extends TestScoreWrapper<BendableScore> {

        @JsonSerialize(using = BendableScoreJacksonSerializer.class)
        @JsonDeserialize(using = BendableScoreJacksonDeserializer.class)
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
