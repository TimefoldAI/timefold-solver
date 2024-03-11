package ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu.size;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

import org.junit.jupiter.api.Test;

class FixedTabuSizeStrategyTest {

    @Test
    void tabuSize() {
        LocalSearchStepScope stepScope = mock(LocalSearchStepScope.class);
        assertThat(new FixedTabuSizeStrategy(5).determineTabuSize(stepScope)).isEqualTo(5);
        assertThat(new FixedTabuSizeStrategy(17).determineTabuSize(stepScope)).isEqualTo(17);
    }

}
