package ai.timefold.solver.quarkus.jackson.score;

import ai.timefold.solver.core.api.score.BendableScore;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

class BendableScoreJacksonRoundTripTest extends AbstractScoreJacksonRoundTripTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestBendableScoreWrapper(null));
        var score = BendableScore.of(new long[] { 1000L, 200L }, new long[] { 34L });
        assertSerializeAndDeserialize(score, new TestBendableScoreWrapper(score));
    }

    public static class TestBendableScoreWrapper extends TestScoreWrapper<BendableScore> {

        @JsonSerialize(using = BendableScoreJacksonSerializer.class)
        @JsonDeserialize(using = BendableScoreJacksonDeserializer.class)
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
