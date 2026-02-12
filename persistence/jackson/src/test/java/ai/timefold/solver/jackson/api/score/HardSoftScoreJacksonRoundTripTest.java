package ai.timefold.solver.jackson.api.score;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.jackson.api.score.buildin.HardSoftScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.HardSoftScoreJacksonSerializer;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

class HardSoftScoreJacksonRoundTripTest extends AbstractScoreJacksonRoundTripTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestHardSoftLongScoreWrapper(null));
        var score = HardSoftScore.of(1200L, 34L);
        assertSerializeAndDeserialize(score, new TestHardSoftLongScoreWrapper(score));
    }

    public static class TestHardSoftLongScoreWrapper extends TestScoreWrapper<HardSoftScore> {

        @JsonSerialize(using = HardSoftScoreJacksonSerializer.class)
        @JsonDeserialize(using = HardSoftScoreJacksonDeserializer.class)
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
