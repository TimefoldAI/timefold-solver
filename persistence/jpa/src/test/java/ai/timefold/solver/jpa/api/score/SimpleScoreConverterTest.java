package ai.timefold.solver.jpa.api.score;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;

import ai.timefold.solver.core.api.score.SimpleScore;
import ai.timefold.solver.jpa.impl.AbstractScoreJpaTest;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class SimpleScoreConverterTest extends AbstractScoreJpaTest {

    @Test
    void persistAndMerge() {
        persistAndMerge(new SimpleLongScoreConverterTestJpaEntity(SimpleScore.ZERO), null, SimpleScore.of(-10L));
    }

    @Entity
    static class SimpleLongScoreConverterTestJpaEntity extends AbstractTestJpaEntity<SimpleScore> {

        @Convert(converter = SimpleScoreConverter.class)
        protected SimpleScore score;

        SimpleLongScoreConverterTestJpaEntity() {
        }

        public SimpleLongScoreConverterTestJpaEntity(SimpleScore score) {
            this.score = score;
        }

        @Override
        public SimpleScore getScore() {
            return score;
        }

        @Override
        public void setScore(SimpleScore score) {
            this.score = score;
        }
    }
}
