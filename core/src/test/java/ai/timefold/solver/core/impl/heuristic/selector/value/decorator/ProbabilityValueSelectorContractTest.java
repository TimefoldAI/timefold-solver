package ai.timefold.solver.core.impl.heuristic.selector.value.decorator;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.delegatingPhaseScope;
import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockSolverScope;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testutil.TestRandom;

import org.junit.jupiter.api.Test;

class ProbabilityValueSelectorContractTest {

    private ProbabilityValueSelector<Object> buildSelector() {
        var child = SelectorTestUtils.mockIterableValueSelector(TestdataEntity.class, "value",
                new TestdataValue("v1"), new TestdataValue("v2"));
        return new ProbabilityValueSelector<>(child, SelectionCacheType.PHASE, (scoreDirector, value) -> 1.0);
    }

    @Test
    void isNeverEndingMatchesIterator() {
        var selector = buildSelector();
        var solverScope = mockSolverScope();
        when(solverScope.getWorkingRandom()).thenReturn(new TestRandom(0.25, 0.75));
        selector.solvingStarted(solverScope);
        selector.phaseStarted(delegatingPhaseScope(solverScope));

        var iterator = selector.iterator();
        assertThat(iterator.hasNext()).isTrue();
        assertThat(iterator.next()).isNotNull();
        assertThat(iterator.hasNext()).isTrue();

        assertThat(selector.isNeverEnding()).isTrue();
    }

    @Test
    void disposeCacheReleasesCachedValues() {
        var selector = buildSelector();
        var solverScope = mockSolverScope();
        selector.solvingStarted(solverScope);
        var phaseScope = delegatingPhaseScope(solverScope);
        selector.phaseStarted(phaseScope);
        assertThat(selector.getSize()).isEqualTo(2L);

        selector.phaseEnded(phaseScope);

        assertThat(selector.getSize()).isZero();
    }
}
