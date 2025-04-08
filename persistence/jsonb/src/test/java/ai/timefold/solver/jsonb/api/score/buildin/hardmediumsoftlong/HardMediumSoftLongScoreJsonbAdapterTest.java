package ai.timefold.solver.jsonb.api.score.buildin.hardmediumsoftlong;

import jakarta.json.bind.annotation.JsonbTypeAdapter;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.jsonb.api.score.AbstractScoreJsonbAdapterTest;

import org.junit.jupiter.api.Test;

class HardMediumSoftLongScoreJsonbAdapterTest extends AbstractScoreJsonbAdapterTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestHardMediumSoftLongScoreWrapper(null));
        HardMediumSoftLongScore score = HardMediumSoftLongScore.of(1200L, 30L, 4L);
        assertSerializeAndDeserialize(score, new TestHardMediumSoftLongScoreWrapper(score));
    }

    public static class TestHardMediumSoftLongScoreWrapper extends TestScoreWrapper<HardMediumSoftLongScore> {

        @JsonbTypeAdapter(HardMediumSoftLongScoreJsonbAdapter.class)
        private HardMediumSoftLongScore score;

        // Empty constructor required by JSON-B
        @SuppressWarnings("unused")
        public TestHardMediumSoftLongScoreWrapper() {
        }

        public TestHardMediumSoftLongScoreWrapper(HardMediumSoftLongScore score) {
            this.score = score;
        }

        @Override
        public HardMediumSoftLongScore getScore() {
            return score;
        }

        @Override
        public void setScore(HardMediumSoftLongScore score) {
            this.score = score;
        }

    }
}
