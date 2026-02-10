package ai.timefold.solver.quarkus.jackson.score.buildin.bendablelong;

import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.quarkus.jackson.score.AbstractScoreJacksonRoundTripTest;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

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
