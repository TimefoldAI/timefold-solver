package ai.timefold.solver.jackson.api.score.buildin.hardmediumsoft;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoft.HardMediumSoftScore;
import ai.timefold.solver.jackson.api.score.AbstractScoreJacksonRoundTripTest;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

class HardMediumSoftScoreJacksonRoundTripTest extends AbstractScoreJacksonRoundTripTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestHardMediumSoftScoreWrapper(null));
        HardMediumSoftScore score = HardMediumSoftScore.of(1200, 30, 4);
        assertSerializeAndDeserialize(score, new TestHardMediumSoftScoreWrapper(score));
        score = HardMediumSoftScore.ofUninitialized(-7, 1200, 30, 4);
        assertSerializeAndDeserialize(score, new TestHardMediumSoftScoreWrapper(score));
    }

    public static class TestHardMediumSoftScoreWrapper extends TestScoreWrapper<HardMediumSoftScore> {

        @JsonSerialize(using = HardMediumSoftScoreJsonSerializer.class)
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
