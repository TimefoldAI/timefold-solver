package ai.timefold.solver.core.impl.phase;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.exhaustivesearch.ExhaustiveSearchPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.partitionedsearch.PartitionedSearchPhaseConfig;
import ai.timefold.solver.core.config.phase.NoChangePhaseConfig;
import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.phase.custom.CustomPhaseConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.constructionheuristic.DefaultConstructionHeuristicPhaseFactory;
import ai.timefold.solver.core.impl.exhaustivesearch.DefaultExhaustiveSearchPhaseFactory;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.localsearch.DefaultLocalSearchPhaseFactory;
import ai.timefold.solver.core.impl.partitionedsearch.DefaultPartitionedSearchPhaseFactory;
import ai.timefold.solver.core.impl.phase.custom.DefaultCustomPhaseFactory;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.termination.Termination;

public interface PhaseFactory<Solution_> {

    static <Solution_> PhaseFactory<Solution_> create(PhaseConfig<?> phaseConfig) {
        if (LocalSearchPhaseConfig.class.isAssignableFrom(phaseConfig.getClass())) {
            return new DefaultLocalSearchPhaseFactory<>((LocalSearchPhaseConfig) phaseConfig);
        } else if (ConstructionHeuristicPhaseConfig.class.isAssignableFrom(phaseConfig.getClass())) {
            return new DefaultConstructionHeuristicPhaseFactory<>((ConstructionHeuristicPhaseConfig) phaseConfig);
        } else if (PartitionedSearchPhaseConfig.class.isAssignableFrom(phaseConfig.getClass())) {
            return new DefaultPartitionedSearchPhaseFactory<>((PartitionedSearchPhaseConfig) phaseConfig);
        } else if (CustomPhaseConfig.class.isAssignableFrom(phaseConfig.getClass())) {
            return new DefaultCustomPhaseFactory<>((CustomPhaseConfig) phaseConfig);
        } else if (ExhaustiveSearchPhaseConfig.class.isAssignableFrom(phaseConfig.getClass())) {
            return new DefaultExhaustiveSearchPhaseFactory<>((ExhaustiveSearchPhaseConfig) phaseConfig);
        } else if (NoChangePhaseConfig.class.isAssignableFrom(phaseConfig.getClass())) {
            return new NoChangePhaseFactory<>((NoChangePhaseConfig) phaseConfig);
        } else {
            throw new IllegalArgumentException(String.format("Unknown %s type: (%s).",
                    PhaseConfig.class.getSimpleName(), phaseConfig.getClass().getName()));
        }
    }

    static <Solution_> List<Phase<Solution_>> buildPhases(List<PhaseConfig> phaseConfigList,
            HeuristicConfigPolicy<Solution_> configPolicy, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            Termination<Solution_> termination) {
        List<Phase<Solution_>> phaseList = new ArrayList<>(phaseConfigList.size());
        boolean isPhaseSelected = false;
        for (int phaseIndex = 0; phaseIndex < phaseConfigList.size(); phaseIndex++) {
            var phaseConfig = phaseConfigList.get(phaseIndex);
            if (phaseIndex > 0) {
                PhaseConfig previousPhaseConfig = phaseConfigList.get(phaseIndex - 1);
                if (!canTerminate(previousPhaseConfig)) {
                    throw new IllegalStateException("Solver configuration contains an unreachable phase. "
                            + "Phase #" + phaseIndex + " (" + phaseConfig + ") follows a phase "
                            + "without a configured termination (" + previousPhaseConfig + ").");
                }
            }
            // The initialization phase can only be applied to construction heuristics or custom phases
            var isConstructionOrCustomPhase = ConstructionHeuristicPhaseConfig.class.isAssignableFrom(phaseConfig.getClass())
                    || CustomPhaseConfig.class.isAssignableFrom(phaseConfig.getClass());
            // The next phase must be a local search
            var isNextPhaseLocalSearch = phaseIndex + 1 < phaseConfigList.size()
                    && LocalSearchPhaseConfig.class.isAssignableFrom(phaseConfigList.get(phaseIndex + 1).getClass());
            PhaseFactory<Solution_> phaseFactory = PhaseFactory.create(phaseConfig);
            var phase = phaseFactory.buildPhase(phaseIndex,
                    !isPhaseSelected && isConstructionOrCustomPhase && isNextPhaseLocalSearch, configPolicy,
                    bestSolutionRecaller, termination);
            // Ensure only one initialization phase is set
            if (!isPhaseSelected && isConstructionOrCustomPhase && isNextPhaseLocalSearch) {
                isPhaseSelected = true;
            }
            phaseList.add(phase);
        }
        return phaseList;
    }

    static boolean canTerminate(PhaseConfig phaseConfig) {
        if (phaseConfig instanceof ConstructionHeuristicPhaseConfig
                || phaseConfig instanceof ExhaustiveSearchPhaseConfig
                || phaseConfig instanceof CustomPhaseConfig) { // Termination guaranteed.
            return true;
        }
        TerminationConfig terminationConfig = phaseConfig.getTerminationConfig();
        return (terminationConfig != null && terminationConfig.isConfigured());
    }

    Phase<Solution_> buildPhase(int phaseIndex, boolean triggerFirstInitializedSolutionEvent,
            HeuristicConfigPolicy<Solution_> solverConfigPolicy, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            Termination<Solution_> solverTermination);
}
