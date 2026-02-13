package ai.timefold.solver.jackson.api.score;

import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.jackson.api.score.buildin.HardMediumSoftScoreJacksonDeserializer;
import ai.timefold.solver.jackson.api.score.buildin.HardMediumSoftScoreJacksonSerializer;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

class HardMediumSoftScoreJacksonRoundTripTest extends AbstractScoreJacksonRoundTripTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestHardMediumSoftScoreWrapper(null));
        var score = HardMediumSoftScore.of(1200L, 30L, 4L);
        assertSerializeAndDeserialize(score, new TestHardMediumSoftScoreWrapper(score));
    }

    public static class TestHardMediumSoftScoreWrapper extends TestScoreWrapper<HardMediumSoftScore> {

        @JsonSerialize(using = HardMediumSoftScoreJacksonSerializer.class)
        @JsonDeserialize(using = HardMediumSoftScoreJacksonDeserializer.class)
        private HardMediumSoftScore score;

        @SuppressWarnings("unused")
        private TestHardMediumSoftScoreWrapper() {
        }

        public TestHardMediumSoftScoreWrapper(HardMediumSoftScore score) {
            this.score = score;
        }

        @Override
        public HardMediumSoftScore getScore() {
            return score;
        }

    }

}
