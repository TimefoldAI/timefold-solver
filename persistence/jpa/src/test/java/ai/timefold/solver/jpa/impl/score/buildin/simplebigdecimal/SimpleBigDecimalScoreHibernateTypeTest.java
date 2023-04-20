package ai.timefold.solver.jpa.impl.score.buildin.simplebigdecimal;

import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;

import ai.timefold.solver.core.api.score.buildin.simplebigdecimal.SimpleBigDecimalScore;
import ai.timefold.solver.jpa.impl.AbstractScoreJpaTest;

import org.hibernate.annotations.Columns;
import org.hibernate.annotations.TypeDef;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class SimpleBigDecimalScoreHibernateTypeTest extends AbstractScoreJpaTest {

    @Test
    void persistAndMerge() {
        persistAndMerge(new SimpleBigDecimalScoreHibernateTypeTestJpaEntity(SimpleBigDecimalScore.ZERO),
                SimpleBigDecimalScore.of(new BigDecimal("-10.01000")),
                SimpleBigDecimalScore.ofUninitialized(-7, new BigDecimal("-10.01000")));
    }

    @Entity
    @TypeDef(defaultForType = SimpleBigDecimalScore.class, typeClass = SimpleBigDecimalScoreHibernateType.class)
    static class SimpleBigDecimalScoreHibernateTypeTestJpaEntity extends AbstractTestJpaEntity<SimpleBigDecimalScore> {

        @Columns(columns = { @Column(name = "initScore"), @Column(name = "score", precision = 10, scale = 5) })
        protected SimpleBigDecimalScore score;

        SimpleBigDecimalScoreHibernateTypeTestJpaEntity() {
        }

        public SimpleBigDecimalScoreHibernateTypeTestJpaEntity(SimpleBigDecimalScore score) {
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
