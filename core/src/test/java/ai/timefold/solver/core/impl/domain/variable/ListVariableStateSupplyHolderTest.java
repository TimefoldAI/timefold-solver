package ai.timefold.solver.core.impl.domain.variable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;

import org.junit.jupiter.api.Test;

class ListVariableStateSupplyHolderTest {

    @SuppressWarnings("unchecked")
    @Test
    void demandsOnPhaseStartedAndCancelsOnPhaseEnded() {
        ListVariableDescriptor<TestdataListSolution> listVariableDescriptor = mock(ListVariableDescriptor.class);
        var stateDemand = new ListVariableStateDemand<>(listVariableDescriptor);
        doReturn(stateDemand).when(listVariableDescriptor).getStateDemand();

        ListVariableStateSupply<TestdataListSolution, Object, Object> listVariableStateSupply =
                mock(ListVariableStateSupply.class);
        SupplyManager supplyManager = mock(SupplyManager.class);
        doReturn(listVariableStateSupply).when(supplyManager).demand(stateDemand);

        InnerScoreDirector<TestdataListSolution, ?> scoreDirector = mock(InnerScoreDirector.class);
        doReturn(supplyManager).when(scoreDirector).getSupplyManager();

        AbstractPhaseScope<TestdataListSolution> phaseScope = mock(AbstractPhaseScope.class);
        doReturn(scoreDirector).when(phaseScope).getScoreDirector();

        var holder = new ListVariableStateSupplyHolder<>(listVariableDescriptor);

        // Not yet demanded: get() must fail fast rather than silently return null.
        assertThatNullPointerException().isThrownBy(holder::get)
                .withMessageContaining("not initialized yet");

        holder.phaseStarted(phaseScope);
        assertThat(holder.get()).isSameAs(listVariableStateSupply);
        verify(supplyManager).demand(stateDemand);

        holder.phaseEnded(phaseScope);
        verify(supplyManager).cancel(stateDemand);
        assertThatNullPointerException().isThrownBy(holder::get)
                .withMessageContaining("not initialized yet");
    }
}
