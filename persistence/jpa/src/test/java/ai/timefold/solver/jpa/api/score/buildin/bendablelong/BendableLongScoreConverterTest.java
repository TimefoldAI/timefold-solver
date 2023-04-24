package ai.timefold.solver.jpa.api.score.buildin.bendablelong;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;

import ai.timefold.solver.core.api.score.buildin.bendablelong.BendableLongScore;
import ai.timefold.solver.jpa.impl.AbstractScoreJpaTest;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class BendableLongScoreConverterTest extends AbstractScoreJpaTest {

    @Test
    void persistAndMerge() {
        persistAndMerge(new BendaleLongScoreConverterTestJpaEntity(BendableLongScore.zero(3, 2)), null,
                BendableLongScore.of(new long[] { 10000L, 2000L, 300L }, new long[] { 40L, 5L }),
                BendableLongScore.ofUninitialized(-7, new long[] { 10000L, 2000L, 300L }, new long[] { 40L, 5L }));
    }

    @Entity
    static class BendaleLongScoreConverterTestJpaEntity extends AbstractTestJpaEntity<BendableLongScore> {

        @Convert(converter = BendableLongScoreConverter.class)
        protected BendableLongScore score;

        BendaleLongScoreConverterTestJpaEntity() {
        }

        public BendaleLongScoreConverterTestJpaEntity(BendableLongScore score) {
            this.score = score;
        }

        @Override
        public BendableLongScore getScore() {
            return score;
        }

        @Override
        public void setScore(BendableLongScore score) {
            this.score = score;
        }
    }
}
