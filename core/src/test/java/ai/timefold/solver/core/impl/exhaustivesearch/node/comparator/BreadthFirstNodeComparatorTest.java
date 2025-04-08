package ai.timefold.solver.core.impl.exhaustivesearch.node.comparator;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.score.director.InnerScore;

import org.junit.jupiter.api.Test;

class BreadthFirstNodeComparatorTest extends AbstractNodeComparatorTest {

    @Test
    void compare() {
        var comparator = new BreadthFirstNodeComparator(true);
        assertScoreCompareToOrder(comparator,
                buildNode(2, "-110", 5, 51),
                buildNode(2, "-110", 5, 50),
                buildNode(2, "-90", 7, 41),
                buildNode(2, "-90", 5, 40),
                buildNode(1, "-110", 7, 61),
                buildNode(1, "-110", 5, 60),
                buildNode(1, "-90", 7, 71),
                buildNode(1, "-90", 5, 70),
                buildNode(1, "-85", 5, 60),
                buildNode(1, InnerScore.withUnassignedCount(SimpleScore.of(-80), 1), 5, 60));
    }

}
