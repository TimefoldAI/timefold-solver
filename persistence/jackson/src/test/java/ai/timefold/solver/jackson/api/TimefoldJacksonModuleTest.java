package ai.timefold.solver.jackson.api;

import static org.assertj.core.api.Assertions.assertThat;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

class TimefoldJacksonModuleTest extends AbstractJacksonRoundTripTest {

    /**
     * According to official specification (see {@link Class#getDeclaredMethods()}),
     * "The elements in the returned array are not sorted and are not in any particular order."
     * Enabling {@link MapperFeature#SORT_PROPERTIES_ALPHABETICALLY} makes this test work on all JDK implementations.
     */
    @Test
    void polymorphicScore() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
        objectMapper.registerModule(TimefoldJacksonModule.createModule());

        TestTimefoldJacksonModuleWrapper input = new TestTimefoldJacksonModuleWrapper();
        input.setBendableScore(BendableScore.of(new int[] { 1000, 200 }, new int[] { 34 }));
        input.setHardSoftScore(HardSoftScore.of(-1, -20));
        input.setPolymorphicScore(HardSoftScore.of(-20, -300));
        TestTimefoldJacksonModuleWrapper output = serializeAndDeserialize(objectMapper, input);
        assertThat(output.getBendableScore()).isEqualTo(BendableScore.of(new int[] { 1000, 200 }, new int[] { 34 }));
        assertThat(output.getHardSoftScore()).isEqualTo(HardSoftScore.of(-1, -20));
        assertThat(output.getPolymorphicScore()).isEqualTo(HardSoftScore.of(-20, -300));

        input.setPolymorphicScore(BendableScore.of(new int[] { -1, -20 }, new int[] { -300, -4000, -50000 }));
        output = serializeAndDeserialize(objectMapper, input);
        assertThat(output.getBendableScore()).isEqualTo(BendableScore.of(new int[] { 1000, 200 }, new int[] { 34 }));
        assertThat(output.getHardSoftScore()).isEqualTo(HardSoftScore.of(-1, -20));
        assertThat(output.getPolymorphicScore())
                .isEqualTo(BendableScore.of(new int[] { -1, -20 }, new int[] { -300, -4000, -50000 }));
    }

    public static class TestTimefoldJacksonModuleWrapper {

        private BendableScore bendableScore;
        private HardSoftScore hardSoftScore;
        private Score polymorphicScore;

        @SuppressWarnings("unused")
        private TestTimefoldJacksonModuleWrapper() {
        }

        public BendableScore getBendableScore() {
            return bendableScore;
        }

        public void setBendableScore(BendableScore bendableScore) {
            this.bendableScore = bendableScore;
        }

        public HardSoftScore getHardSoftScore() {
            return hardSoftScore;
        }

        public void setHardSoftScore(HardSoftScore hardSoftScore) {
            this.hardSoftScore = hardSoftScore;
        }

        public Score getPolymorphicScore() {
            return polymorphicScore;
        }

        public void setPolymorphicScore(Score polymorphicScore) {
            this.polymorphicScore = polymorphicScore;
        }

    }

}
