package ai.timefold.solver.jpa.impl.score.buildin.simplelong;

import javax.persistence.Column;
import javax.persistence.Entity;

import ai.timefold.solver.core.api.score.buildin.simplelong.SimpleLongScore;
import ai.timefold.solver.jpa.impl.AbstractScoreJpaTest;

import org.hibernate.annotations.Columns;
import org.hibernate.annotations.TypeDef;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class SimpleLongScoreHibernateTypeTest extends AbstractScoreJpaTest {

    @Test
    void persistAndMerge() {
        persistAndMerge(new SimpleLongScoreHibernateTypeTestJpaEntity(SimpleLongScore.ZERO),
                SimpleLongScore.of(-10L),
                SimpleLongScore.ofUninitialized(-7, -10L));
    }

    @Entity
    @TypeDef(defaultForType = SimpleLongScore.class, typeClass = SimpleLongScoreHibernateType.class)
    static class SimpleLongScoreHibernateTypeTestJpaEntity extends AbstractTestJpaEntity<SimpleLongScore> {

        @Columns(columns = { @Column(name = "initScore"), @Column(name = "score") })
        protected SimpleLongScore score;

        SimpleLongScoreHibernateTypeTestJpaEntity() {
        }

        public SimpleLongScoreHibernateTypeTestJpaEntity(SimpleLongScore score) {
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
