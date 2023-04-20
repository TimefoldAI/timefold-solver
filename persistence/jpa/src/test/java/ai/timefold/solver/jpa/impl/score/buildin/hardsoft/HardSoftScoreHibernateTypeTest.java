package ai.timefold.solver.jpa.impl.score.buildin.hardsoft;

import javax.persistence.Column;
import javax.persistence.Entity;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.jpa.impl.AbstractScoreJpaTest;

import org.hibernate.annotations.Columns;
import org.hibernate.annotations.TypeDef;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class HardSoftScoreHibernateTypeTest extends AbstractScoreJpaTest {

    @Test
    void persistAndMerge() {
        persistAndMerge(new HardSoftScoreHibernateTypeTestJpaEntity(HardSoftScore.ZERO),
                HardSoftScore.of(-10, -2),
                HardSoftScore.ofUninitialized(-7, -10, -2));
    }

    @Entity
    @TypeDef(defaultForType = HardSoftScore.class, typeClass = HardSoftScoreHibernateType.class)
    static class HardSoftScoreHibernateTypeTestJpaEntity extends AbstractTestJpaEntity<HardSoftScore> {

        @Columns(columns = { @Column(name = "initScore"), @Column(name = "hardScore"), @Column(name = "softScore") })
        protected HardSoftScore score;

        HardSoftScoreHibernateTypeTestJpaEntity() {
        }

        public HardSoftScoreHibernateTypeTestJpaEntity(HardSoftScore score) {
            this.score = score;
        }

        @Override
        public HardSoftScore getScore() {
            return score;
        }

        @Override
        public void setScore(HardSoftScore score) {
            this.score = score;
        }

    }

}
