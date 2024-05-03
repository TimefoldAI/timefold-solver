package ai.timefold.solver.core.impl.exhaustivesearch.scope;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.TreeSet;

import ai.timefold.solver.core.api.score.buildin.simple.SimpleScore;
import ai.timefold.solver.core.impl.exhaustivesearch.node.comparator.AbstractNodeComparatorTest;
import ai.timefold.solver.core.impl.exhaustivesearch.node.comparator.ScoreFirstNodeComparator;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;

import org.junit.jupiter.api.Test;

class ExhaustiveSearchPhaseScopeTest extends AbstractNodeComparatorTest {

    @Test
    void testNodePruning() {
        ExhaustiveSearchPhaseScope<TestdataSolution> phase = new ExhaustiveSearchPhaseScope<>(new SolverScope<>(), 0);
        phase.setExpandableNodeQueue(new TreeSet<>(new ScoreFirstNodeComparator(true)));
        phase.addExpandableNode(buildNode(0, "0", 0, 0));
        phase.addExpandableNode(buildNode(0, "1", 0, 0));
        phase.addExpandableNode(buildNode(0, "2", 0, 0));
        phase.setBestPessimisticBound(SimpleScore.of(Integer.MIN_VALUE));
        phase.registerPessimisticBound(SimpleScore.of(1));
        assertThat(phase.getExpandableNodeQueue()).hasSize(1);
    }

}
