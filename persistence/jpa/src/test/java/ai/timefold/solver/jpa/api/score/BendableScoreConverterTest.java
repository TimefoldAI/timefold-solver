package ai.timefold.solver.jpa.api.score;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;

import ai.timefold.solver.core.api.score.BendableScore;
import ai.timefold.solver.jpa.impl.AbstractScoreJpaTest;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class BendableScoreConverterTest extends AbstractScoreJpaTest {

    @Test
    void persistAndMerge() {
        persistAndMerge(new BendableLongScoreConverterTestJpaEntity(BendableScore.zero(3, 2)), null,
                BendableScore.of(new long[] { 10000L, 2000L, 300L }, new long[] { 40L, 5L }));
    }

    @Entity
    static class BendableLongScoreConverterTestJpaEntity extends AbstractTestJpaEntity<BendableScore> {

        @Convert(converter = BendableScoreConverter.class)
        protected BendableScore score;

        BendableLongScoreConverterTestJpaEntity() {
        }

        public BendableLongScoreConverterTestJpaEntity(BendableScore score) {
            this.score = score;
        }

        @Override
        public BendableScore getScore() {
            return score;
        }

        @Override
        public void setScore(BendableScore score) {
            this.score = score;
        }
    }
}
