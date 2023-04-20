package ai.timefold.solver.jackson.api.score.buildin.hardsoft;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.jackson.api.score.AbstractScoreJacksonRoundTripTest;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

class HardSoftScoreJacksonRoundTripTest extends AbstractScoreJacksonRoundTripTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestHardSoftScoreWrapper(null));
        HardSoftScore score = HardSoftScore.of(1200, 34);
        assertSerializeAndDeserialize(score, new TestHardSoftScoreWrapper(score));
        score = HardSoftScore.ofUninitialized(-7, 1200, 34);
        assertSerializeAndDeserialize(score, new TestHardSoftScoreWrapper(score));
    }

    public static class TestHardSoftScoreWrapper extends TestScoreWrapper<HardSoftScore> {

        @JsonSerialize(using = HardSoftScoreJacksonSerializer.class)
        @JsonDeserialize(using = HardSoftScoreJacksonDeserializer.class)
        private HardSoftScore score;

        @SuppressWarnings("unused")
        private TestHardSoftScoreWrapper() {
        }

        public TestHardSoftScoreWrapper(HardSoftScore score) {
            this.score = score;
        }

        @Override
        public HardSoftScore getScore() {
            return score;
        }

    }

}
