package ai.timefold.solver.jackson.api.score.buildin.hardmediumsoftlong;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.jackson.api.score.AbstractScoreJacksonRoundTripTest;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

class HardMediumSoftLongScoreJacksonRoundTripTest extends AbstractScoreJacksonRoundTripTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestHardMediumSoftLongScoreWrapper(null));
        HardMediumSoftLongScore score = HardMediumSoftLongScore.of(1200L, 30L, 4L);
        assertSerializeAndDeserialize(score, new TestHardMediumSoftLongScoreWrapper(score));
        score = HardMediumSoftLongScore.ofUninitialized(-7, 1200L, 30L, 4L);
        assertSerializeAndDeserialize(score, new TestHardMediumSoftLongScoreWrapper(score));
    }

    public static class TestHardMediumSoftLongScoreWrapper extends TestScoreWrapper<HardMediumSoftLongScore> {

        @JsonSerialize(using = HardMediumSoftLongScoreJacksonSerializer.class)
        @JsonDeserialize(using = HardMediumSoftLongScoreJacksonDeserializer.class)
        private HardMediumSoftLongScore score;

        @SuppressWarnings("unused")
        private TestHardMediumSoftLongScoreWrapper() {
        }

        public TestHardMediumSoftLongScoreWrapper(HardMediumSoftLongScore score) {
            this.score = score;
        }

        @Override
        public HardMediumSoftLongScore getScore() {
            return score;
        }

    }

}
