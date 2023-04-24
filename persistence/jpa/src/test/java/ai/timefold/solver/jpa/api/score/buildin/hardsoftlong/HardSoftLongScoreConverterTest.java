package ai.timefold.solver.jpa.api.score.buildin.hardsoftlong;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;

import ai.timefold.solver.core.api.score.buildin.hardsoftlong.HardSoftLongScore;
import ai.timefold.solver.jpa.impl.AbstractScoreJpaTest;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class HardSoftLongScoreConverterTest extends AbstractScoreJpaTest {

    @Test
    void persistAndMerge() {
        persistAndMerge(new HardSoftLongScoreConverterTestJpaEntity(HardSoftLongScore.ZERO), null,
                HardSoftLongScore.of(-10L, -2L),
                HardSoftLongScore.ofUninitialized(-7, -10L, -2L));
    }

    @Entity
    static class HardSoftLongScoreConverterTestJpaEntity extends AbstractTestJpaEntity<HardSoftLongScore> {

        @Convert(converter = HardSoftLongScoreConverter.class)
        protected HardSoftLongScore score;

        HardSoftLongScoreConverterTestJpaEntity() {
        }

        public HardSoftLongScoreConverterTestJpaEntity(HardSoftLongScore score) {
            this.score = score;
        }

        @Override
        public HardSoftLongScore getScore() {
            return score;
        }

        @Override
        public void setScore(HardSoftLongScore score) {
            this.score = score;
        }
    }
}
