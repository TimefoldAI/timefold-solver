package ai.timefold.solver.jpa.api.score.buildin.simplelong;

import javax.persistence.Convert;
import javax.persistence.Entity;

import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
import ai.timefold.solver.jpa.impl.AbstractScoreJpaTest;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class SimpleLongScoreConverterTest extends AbstractScoreJpaTest {

    @Test
    void persistAndMerge() {
        persistAndMerge(new SimpleLongScoreConverterTestJpaEntity(SimpleLongScore.ZERO), null,
                SimpleLongScore.of(-10L),
                SimpleLongScore.ofUninitialized(-7, -10L));
    }

    @Entity
    static class SimpleLongScoreConverterTestJpaEntity extends AbstractTestJpaEntity<SimpleLongScore> {

        @Convert(converter = SimpleLongScoreConverter.class)
        protected SimpleLongScore score;

        SimpleLongScoreConverterTestJpaEntity() {
        }

        public SimpleLongScoreConverterTestJpaEntity(SimpleLongScore score) {
            this.score = score;
        }

        @Override
        public SimpleLongScore getScore() {
            return score;
        }

        @Override
        public void setScore(SimpleLongScore score) {
            this.score = score;
        }
    }
}
