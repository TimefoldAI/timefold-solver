package ai.timefold.solver.jpa.impl.score.buildin.simple;

import javax.persistence.Column;
import javax.persistence.Entity;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.jpa.impl.AbstractScoreJpaTest;

import org.hibernate.annotations.Columns;
import org.hibernate.annotations.TypeDef;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class SimpleScoreHibernateTypeTest extends AbstractScoreJpaTest {

    @Test
    void persistAndMerge() {
        persistAndMerge(new SimpleScoreHibernateTypeTestJpaEntity(SimpleScore.ZERO),
                SimpleScore.of(-10),
                SimpleScore.ofUninitialized(-7, -10));
    }

    @Entity
    @TypeDef(defaultForType = SimpleScore.class, typeClass = SimpleScoreHibernateType.class)
    static class SimpleScoreHibernateTypeTestJpaEntity extends AbstractTestJpaEntity<SimpleScore> {

        @Columns(columns = { @Column(name = "initScore"), @Column(name = "score") })
        protected SimpleScore score;

        SimpleScoreHibernateTypeTestJpaEntity() {
        }

        public SimpleScoreHibernateTypeTestJpaEntity(SimpleScore score) {
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
