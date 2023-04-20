package ai.timefold.solver.jsonb.api;

import static org.assertj.core.api.Assertions.assertThat;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;

import org.junit.jupiter.api.Test;

class TimefoldJsonbConfigTest extends AbstractJsonbJsonAdapterTest {

    @Test
    void jsonbConfigSerializeAndDeserialize() {
        JsonbConfig config = TimefoldJsonbConfig.createConfig();
        Jsonb jsonb = JsonbBuilder.create(config);

        TestTimefoldJsonbConfigWrapper input = new TestTimefoldJsonbConfigWrapper();
        input.setBendableScore(BendableScore.of(new int[] { 1000, 200 }, new int[] { 34 }));
        input.setHardSoftScore(HardSoftScore.of(-1, -20));
        TestTimefoldJsonbConfigWrapper output = serializeAndDeserialize(jsonb, input);
        assertThat(output.getBendableScore()).isEqualTo(BendableScore.of(new int[] { 1000, 200 }, new int[] { 34 }));
        assertThat(output.getHardSoftScore()).isEqualTo(HardSoftScore.of(-1, -20));
    }

    public static class TestTimefoldJsonbConfigWrapper {

        private BendableScore bendableScore;
        private HardSoftScore hardSoftScore;

        // Empty constructor required by JSON-B
        @SuppressWarnings("unused")
        public TestTimefoldJsonbConfigWrapper() {
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
    }
}
