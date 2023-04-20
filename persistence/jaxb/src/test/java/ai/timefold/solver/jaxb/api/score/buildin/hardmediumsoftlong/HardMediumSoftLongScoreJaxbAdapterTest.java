package ai.timefold.solver.jaxb.api.score.buildin.hardmediumsoftlong;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import ai.timefold.solver.core.api.score.buildin.hardmediumsoftlong.HardMediumSoftLongScore;
import ai.timefold.solver.jaxb.api.score.AbstractScoreJaxbAdapterTest;

import org.junit.jupiter.api.Test;

class HardMediumSoftLongScoreJaxbAdapterTest extends AbstractScoreJaxbAdapterTest {

    @Test
    void serializeAndDeserialize() {
        assertSerializeAndDeserialize(null, new TestHardMediumSoftLongScoreWrapper(null));

        HardMediumSoftLongScore score = HardMediumSoftLongScore.of(1200L, 30L, 4L);
        assertSerializeAndDeserialize(score, new TestHardMediumSoftLongScoreWrapper(score));

        score = HardMediumSoftLongScore.ofUninitialized(-7, 1200L, 30L, 4L);
        assertSerializeAndDeserialize(score, new TestHardMediumSoftLongScoreWrapper(score));
    }

    @XmlRootElement
    public static class TestHardMediumSoftLongScoreWrapper extends TestScoreWrapper<HardMediumSoftLongScore> {

        @XmlJavaTypeAdapter(HardMediumSoftLongScoreJaxbAdapter.class)
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
