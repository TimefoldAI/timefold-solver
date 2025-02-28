package ai.timefold.solver.core.impl.localsearch.decider.restart;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance.LateAcceptanceAcceptor;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

import org.junit.jupiter.api.Test;

class AcceptorRestartStrategyTest {

    @Test
    void restart() {
        // Restore the best solution
        var phaseScope = mock(LocalSearchPhaseScope.class);
        var stepScope = mock(LocalSearchStepScope.class);
        when(stepScope.getPhaseScope()).thenReturn(phaseScope);
        var acceptor = mock(LateAcceptanceAcceptor.class);
        var strategy = new AcceptorRestartStrategy<>(acceptor);

        // Call acceptor restart logic
        strategy.applyRestart(stepScope);
        verify(acceptor, times(1)).restart(any());
    }
}
