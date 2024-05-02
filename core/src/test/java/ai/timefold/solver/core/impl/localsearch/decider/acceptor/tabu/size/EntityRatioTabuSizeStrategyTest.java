package ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu.size;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.junit.jupiter.api.Test;

class EntityRatioTabuSizeStrategyTest {

    @Test
    <Solution_> void tabuSize() {
        var phaseScope = new LocalSearchPhaseScope<Solution_>(mock(SolverScope.class), 0);
        when(phaseScope.getWorkingEntityCount()).thenReturn(100);
        var stepScope = new LocalSearchStepScope<>(phaseScope);
        assertThat(new EntityRatioTabuSizeStrategy<Solution_>(0.1).determineTabuSize(stepScope)).isEqualTo(10);
        assertThat(new EntityRatioTabuSizeStrategy<Solution_>(0.5).determineTabuSize(stepScope)).isEqualTo(50);
        // Rounding
        assertThat(new EntityRatioTabuSizeStrategy<Solution_>(0.1051).determineTabuSize(stepScope)).isEqualTo(11);
        assertThat(new EntityRatioTabuSizeStrategy<Solution_>(0.1049).determineTabuSize(stepScope)).isEqualTo(10);
        // Corner cases
        assertThat(new EntityRatioTabuSizeStrategy<Solution_>(0.0000001).determineTabuSize(stepScope)).isEqualTo(1);
        assertThat(new EntityRatioTabuSizeStrategy<Solution_>(0.9999999).determineTabuSize(stepScope)).isEqualTo(99);
    }

}
