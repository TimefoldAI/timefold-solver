package ai.timefold.solver.core.impl.localsearch.decider.acceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import ai.timefold.solver.core.api.score.buildin.hardsoft.HardSoftScore;
import ai.timefold.solver.core.config.localsearch.decider.acceptor.AcceptorType;
import ai.timefold.solver.core.config.localsearch.decider.acceptor.LocalSearchAcceptorConfig;
import ai.timefold.solver.core.config.localsearch.decider.acceptor.stepcountinghillclimbing.StepCountingHillClimbingType;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.greatdeluge.GreatDelugeAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.hillclimbing.HillClimbingAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance.DiversifiedLateAcceptanceAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.lateacceptance.LateAcceptanceAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.simulatedannealing.SimulatedAnnealingAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.stepcountinghillclimbing.StepCountingHillClimbingAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu.EntityTabuAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu.MoveTabuAcceptor;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.tabu.ValueTabuAcceptor;
import ai.timefold.solver.core.impl.score.buildin.HardSoftScoreDefinition;
import ai.timefold.solver.core.impl.score.definition.ScoreDefinition;

import org.junit.jupiter.api.Test;

class AcceptorFactoryTest {

    @Test
    <Solution_> void buildCompositeAcceptor() {
        LocalSearchAcceptorConfig localSearchAcceptorConfig = new LocalSearchAcceptorConfig()
                .withAcceptorTypeList(Arrays.asList(AcceptorType.values()))
                .withEntityTabuSize(1)
                .withFadingEntityTabuSize(1)
                .withMoveTabuSize(1)
                .withFadingMoveTabuSize(1)
                .withUndoMoveTabuSize(1) // Has no effect anymore.
                .withValueTabuSize(1)
                .withFadingValueTabuSize(1)
                .withLateAcceptanceSize(10)
                .withSimulatedAnnealingStartingTemperature("-10hard/-10soft")
                .withStepCountingHillClimbingSize(1)
                .withStepCountingHillClimbingType(StepCountingHillClimbingType.IMPROVING_STEP);

        HeuristicConfigPolicy<Solution_> heuristicConfigPolicy = mock(HeuristicConfigPolicy.class);
        ScoreDefinition<HardSoftScore> scoreDefinition = new HardSoftScoreDefinition();
        when(heuristicConfigPolicy.getEnvironmentMode()).thenReturn(EnvironmentMode.NON_INTRUSIVE_FULL_ASSERT);
        when(heuristicConfigPolicy.getScoreDefinition()).thenReturn(scoreDefinition);

        AcceptorFactory<Solution_> acceptorFactory = AcceptorFactory.create(localSearchAcceptorConfig);
        Acceptor<Solution_> acceptor = acceptorFactory.buildAcceptor(heuristicConfigPolicy);
        assertThat(acceptor).isExactlyInstanceOf(CompositeAcceptor.class);
        CompositeAcceptor<Solution_> compositeAcceptor = (CompositeAcceptor<Solution_>) acceptor;
        assertThat(compositeAcceptor.acceptorList)
                .map(a -> (Class) a.getClass())
                .containsExactly(HillClimbingAcceptor.class, StepCountingHillClimbingAcceptor.class, EntityTabuAcceptor.class,
                        ValueTabuAcceptor.class, MoveTabuAcceptor.class, SimulatedAnnealingAcceptor.class,
                        LateAcceptanceAcceptor.class, DiversifiedLateAcceptanceAcceptor.class, GreatDelugeAcceptor.class);
    }

    @Test
    <Solution_> void noAcceptorConfigured_throwsException() {
        AcceptorFactory<Solution_> acceptorFactory = AcceptorFactory.create(new LocalSearchAcceptorConfig());
        assertThatIllegalArgumentException().isThrownBy(() -> acceptorFactory.buildAcceptor(mock(HeuristicConfigPolicy.class)))
                .withMessageContaining("The acceptor does not specify any acceptorType");
    }

    @Test
    <Solution_> void lateAcceptanceAcceptor() {
        var localSearchAcceptorConfig = new LocalSearchAcceptorConfig()
                .withAcceptorTypeList(List.of(AcceptorType.LATE_ACCEPTANCE));
        HeuristicConfigPolicy<Solution_> heuristicConfigPolicy = mock(HeuristicConfigPolicy.class);
        AcceptorFactory<Solution_> acceptorFactory = AcceptorFactory.create(localSearchAcceptorConfig);
        var acceptor = acceptorFactory.buildAcceptor(heuristicConfigPolicy);
        assertThat(acceptor).isExactlyInstanceOf(LateAcceptanceAcceptor.class);

        localSearchAcceptorConfig = new LocalSearchAcceptorConfig()
                .withLateAcceptanceSize(10);
        acceptorFactory = AcceptorFactory.create(localSearchAcceptorConfig);
        acceptor = acceptorFactory.buildAcceptor(heuristicConfigPolicy);
        assertThat(acceptor).isExactlyInstanceOf(LateAcceptanceAcceptor.class);
    }

    @Test
    <Solution_> void diversifiedLateAcceptanceAcceptor() {
        var localSearchAcceptorConfig = new LocalSearchAcceptorConfig()
                .withAcceptorTypeList(List.of(AcceptorType.DIVERSIFIED_LATE_ACCEPTANCE));
        HeuristicConfigPolicy<Solution_> heuristicConfigPolicy = mock(HeuristicConfigPolicy.class);
        AcceptorFactory<Solution_> acceptorFactory = AcceptorFactory.create(localSearchAcceptorConfig);
        var acceptor = acceptorFactory.buildAcceptor(heuristicConfigPolicy);
        assertThat(acceptor).isExactlyInstanceOf(DiversifiedLateAcceptanceAcceptor.class);

        localSearchAcceptorConfig = new LocalSearchAcceptorConfig()
                .withAcceptorTypeList(List.of(AcceptorType.DIVERSIFIED_LATE_ACCEPTANCE))
                .withLateAcceptanceSize(10);
        acceptorFactory = AcceptorFactory.create(localSearchAcceptorConfig);
        acceptor = acceptorFactory.buildAcceptor(heuristicConfigPolicy);
        assertThat(acceptor).isExactlyInstanceOf(DiversifiedLateAcceptanceAcceptor.class);

        doThrow(new IllegalStateException()).when(heuristicConfigPolicy).ensurePreviewFeature(any());
        localSearchAcceptorConfig = new LocalSearchAcceptorConfig()
                .withAcceptorTypeList(List.of(AcceptorType.DIVERSIFIED_LATE_ACCEPTANCE))
                .withLateAcceptanceSize(10);
        AcceptorFactory<Solution_> badAcceptorFactory = AcceptorFactory.create(localSearchAcceptorConfig);
        assertThatIllegalStateException().isThrownBy(() -> badAcceptorFactory.buildAcceptor(heuristicConfigPolicy));
    }
}
