package ai.timefold.solver.benchmark.impl.ranking;

import static ai.timefold.solver.core.impl.testdata.util.PlannerAssert.assertCompareToOrder;

import java.util.Comparator;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.score.buildin.SimpleScoreDefinition;

import org.junit.jupiter.api.Test;

class ResilientScoreComparatorTest {

    @Test
    void compareTo() {
        Comparator<Score> comparator = new ResilientScoreComparator(new SimpleScoreDefinition());

        assertCompareToOrder(comparator,
                SimpleScore.of(-20),
                SimpleScore.of(-1));
        assertCompareToOrder(comparator,
                HardSoftScore.of(-20, -300),
                HardSoftScore.of(-1, -4000));
        assertCompareToOrder(comparator,
                SimpleScore.of(-4000),
                HardSoftScore.of(-300, -300),
                HardSoftScore.of(-20, -4000),
                SimpleScore.of(-20),
                HardSoftScore.of(-20, 4000),
                SimpleScore.of(-1));
    }

}
