package ai.timefold.solver.jpa.api.score;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;

import ai.timefold.solver.core.api.score.HardMediumSoftScore;
import ai.timefold.solver.jpa.impl.AbstractScoreJpaTest;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class HardMediumSoftScoreConverterTest extends AbstractScoreJpaTest {

    @Test
    void persistAndMerge() {
        persistAndMerge(new HardMediumSoftScoreConverterTestJpaEntity(HardMediumSoftScore.ZERO), null,
                HardMediumSoftScore.of(-100L, -20L, -3L));
    }

    @Entity
    static class HardMediumSoftScoreConverterTestJpaEntity
            extends AbstractTestJpaEntity<HardMediumSoftScore> {

        @Convert(converter = HardMediumSoftScoreConverter.class)
        protected HardMediumSoftScore score;

        HardMediumSoftScoreConverterTestJpaEntity() {
        }

        public HardMediumSoftScoreConverterTestJpaEntity(HardMediumSoftScore score) {
            this.score = score;
        }

        @Override
        public HardMediumSoftScore getScore() {
            return score;
        }

        @Override
        public void setScore(HardMediumSoftScore score) {
            this.score = score;
        }
    }
}
