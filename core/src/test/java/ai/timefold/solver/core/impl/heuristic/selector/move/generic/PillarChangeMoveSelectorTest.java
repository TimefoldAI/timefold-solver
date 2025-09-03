package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.mockEntitySelector;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.mockIterableValueSelector;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.phaseStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.solvingStarted;
import static ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils.stepStarted;
import static ai.timefold.solver.core.testutil.PlannerAssert.assertAllCodesOfMoveSelector;

import java.util.List;

import ai.timefold.solver.core.config.heuristic.selector.entity.pillar.SubPillarConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.pillar.DefaultPillarSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testutil.PlannerTestUtils;

import org.junit.jupiter.api.Test;

class PillarChangeMoveSelectorTest {

    @Test
    void original() {
        var a = new TestdataEntity("a");
        var b = new TestdataEntity("b");
        var c = new TestdataEntity("c");
        var d = new TestdataEntity("d");
        var v1 = new TestdataValue("1");
        var v2 = new TestdataValue("2");
        var v3 = new TestdataValue("3");
        var solution = new TestdataSolution();
        solution.setEntityList(List.of(a, b, c, d));
        solution.setValueList(List.of(v1, v2, v3));

        EntitySelector<TestdataSolution> entitySelector = mockEntitySelector(TestdataEntity.class, a, b, c, d);
        var pillarSelector = new DefaultPillarSelector<>(entitySelector,
                TestdataEntity.buildEntityDescriptor().getGenuineVariableDescriptorList(), false,
                SubPillarConfigPolicy.withoutSubpillars());
        ValueSelector<TestdataSolution> valueSelector = mockIterableValueSelector(TestdataEntity.class, "value", v1, v2, v3);
        var moveSelector = new PillarChangeMoveSelector<>(pillarSelector, valueSelector, false);

        var scoreDirector = PlannerTestUtils.mockScoreDirector(TestdataSolution.buildSolutionDescriptor());
        var solverScope = solvingStarted(moveSelector, scoreDirector);
        var phaseScope = phaseStarted(moveSelector, solverScope);
        stepStarted(moveSelector, phaseScope);
        assertAllCodesOfMoveSelector(moveSelector, "[a, b, c, d]->1", "[a, b, c, d]->2", "[a, b, c, d]->3");
    }

}
