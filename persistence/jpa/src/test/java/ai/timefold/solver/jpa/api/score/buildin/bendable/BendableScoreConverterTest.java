package ai.timefold.solver.jpa.api.score.buildin.bendable;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;

import ai.timefold.solver.core.api.score.buildin.bendable.BendableScore;
import ai.timefold.solver.jpa.impl.AbstractScoreJpaTest;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class BendableScoreConverterTest extends AbstractScoreJpaTest {

    @Test
    void persistAndMerge() {
        persistAndMerge(new BendableScoreConverterTestJpaEntity(BendableScore.zero(3, 2)), null,
                BendableScore.of(new int[] { 10000, 2000, 300 }, new int[] { 40, 5 }));
    }

    @Entity
    static class BendableScoreConverterTestJpaEntity extends AbstractTestJpaEntity<BendableScore> {

        @Convert(converter = BendableScoreConverter.class)
        protected BendableScore score;

        BendableScoreConverterTestJpaEntity() {
        }

        public BendableScoreConverterTestJpaEntity(BendableScore score) {
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
