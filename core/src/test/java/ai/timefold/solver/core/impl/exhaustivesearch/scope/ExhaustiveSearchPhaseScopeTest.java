package ai.timefold.solver.core.impl.exhaustivesearch.scope;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.TreeSet;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.exhaustivesearch.node.comparator.AbstractNodeComparatorTest;
import ai.timefold.solver.core.impl.exhaustivesearch.node.comparator.ScoreFirstNodeComparator;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;

class ExhaustiveSearchPhaseScopeTest extends AbstractNodeComparatorTest {

    @Test
    void testNodePruning() {
        var phase = new ExhaustiveSearchPhaseScope<TestdataSolution>(new SolverScope<>(), 0);
        phase.setExpandableNodeQueue(new TreeSet<>(new ScoreFirstNodeComparator(true)));
        phase.addExpandableNode(buildNode(0, "0", 0, 0));
        phase.addExpandableNode(buildNode(0, "1", 0, 0));
        phase.addExpandableNode(buildNode(0, "2", 0, 0));
        phase.setBestPessimisticBound(InnerScore.fullyAssigned(SimpleScore.of(Integer.MIN_VALUE)));
        phase.registerPessimisticBound(InnerScore.fullyAssigned(SimpleScore.ONE));
        assertThat(phase.getExpandableNodeQueue()).hasSize(1);
    }

}
