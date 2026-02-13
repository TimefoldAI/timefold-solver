package ai.timefold.solver.jpa.api.score;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.jpa.impl.AbstractScoreJpaTest;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class HardSoftScoreConverterTest extends AbstractScoreJpaTest {

    @Test
    void persistAndMerge() {
        persistAndMerge(new HardSoftLongScoreConverterTestJpaEntity(HardSoftScore.ZERO), null,
                HardSoftScore.of(-10L, -2L));
    }

    @Entity
    static class HardSoftLongScoreConverterTestJpaEntity extends AbstractTestJpaEntity<HardSoftScore> {

        @Convert(converter = HardSoftScoreConverter.class)
        protected HardSoftScore score;

        HardSoftLongScoreConverterTestJpaEntity() {
        }

        public HardSoftLongScoreConverterTestJpaEntity(HardSoftScore score) {
            this.score = score;
        }

        @Override
        public HardSoftScore getScore() {
            return score;
        }

        @Override
        public void setScore(HardSoftScore score) {
            this.score = score;
        }
    }
}
