package ai.timefold.solver.jpa.api.score.buildin.simplebigdecimal;

import java.math.BigDecimal;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;

import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.jpa.impl.AbstractScoreJpaTest;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class SimpleBigDecimalScoreConverterTest extends AbstractScoreJpaTest {

    @Test
    void persistAndMerge() {
        persistAndMerge(new SimpleBigDecimalScoreConverterTestJpaEntity(SimpleBigDecimalScore.ZERO), null,
                SimpleBigDecimalScore.of(new BigDecimal("-10.01000")));
    }

    @Entity
    static class SimpleBigDecimalScoreConverterTestJpaEntity extends AbstractTestJpaEntity<SimpleBigDecimalScore> {

        @Convert(converter = SimpleBigDecimalScoreConverter.class)
        protected SimpleBigDecimalScore score;

        SimpleBigDecimalScoreConverterTestJpaEntity() {
        }

        public SimpleBigDecimalScoreConverterTestJpaEntity(SimpleBigDecimalScore score) {
            this.score = score;
        }

        @Override
        public SimpleBigDecimalScore getScore() {
            return score;
        }

        @Override
        public void setScore(SimpleBigDecimalScore score) {
            this.score = score;
        }
    }
}
