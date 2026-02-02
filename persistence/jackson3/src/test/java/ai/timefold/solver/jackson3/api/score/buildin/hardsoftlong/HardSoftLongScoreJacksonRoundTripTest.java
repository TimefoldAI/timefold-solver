package ai.timefold.solver.jackson3.api.score.buildin.hardsoftlong;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.jackson3.api.score.AbstractScoreJacksonRoundTripTest;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

class HardSoftLongScoreJacksonRoundTripTest extends AbstractScoreJacksonRoundTripTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestHardSoftLongScoreWrapper(null));
        var score = HardSoftLongScore.of(1200L, 34L);
        assertSerializeAndDeserialize(score, new TestHardSoftLongScoreWrapper(score));
    }

    public static class TestHardSoftLongScoreWrapper extends TestScoreWrapper<HardSoftLongScore> {

        @JsonSerialize(using = HardSoftLongScoreJacksonSerializer.class)
        @JsonDeserialize(using = HardSoftLongScoreJacksonDeserializer.class)
        private HardSoftLongScore score;

        @SuppressWarnings("unused")
        private TestHardSoftLongScoreWrapper() {
        }

        public TestHardSoftLongScoreWrapper(HardSoftLongScore score) {
            this.score = score;
        }

        @Override
        public HardSoftLongScore getScore() {
            return score;
        }

    }

}
