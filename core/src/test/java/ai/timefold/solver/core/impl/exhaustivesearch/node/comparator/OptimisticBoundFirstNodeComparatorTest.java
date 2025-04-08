package ai.timefold.solver.core.impl.exhaustivesearch.node.comparator;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.score.director.InnerScore;

import org.junit.jupiter.api.Test;

class OptimisticBoundFirstNodeComparatorTest extends AbstractNodeComparatorTest {

    @Test
    void compare() {
        var comparator = new OptimisticBoundFirstNodeComparator(true);
        assertScoreCompareToOrder(comparator,
                buildNode(1, "-300", 5, 41),
                buildNode(1, "-300", 5, 40),
                buildNode(1, InnerScore.withUnassignedCount(SimpleScore.of(-200), 10), 5, 40),
                buildNode(1, "-110", 5, 40),
                buildNode(1, "-110", 7, 40),
                buildNode(2, "-110", 5, 40),
                buildNode(2, "-110", 7, 40),
                buildNode(1, "-90", 5, 40),
                buildNode(1, "-90", 7, 40),
                buildNode(2, "-90", 5, 40),
                buildNode(2, "-90", 7, 40),
                buildNode(1, "-95", 0, 5, 40),
                buildNode(2, "-95", 0, 5, 40),
                buildNode(2, "-95", 0, 7, 40),
                buildNode(1, "-11", 1, 5, 40),
                buildNode(1, InnerScore.withUnassignedCount(SimpleScore.of(-10), 1), 1, 5, 40));
    }

}
