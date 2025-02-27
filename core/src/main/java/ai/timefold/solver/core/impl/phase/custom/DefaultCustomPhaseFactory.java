package ai.timefold.solver.core.impl.phase.custom;

import java.util.ArrayList;
import java.util.Collection;

import ai.timefold.solver.core.api.solver.phase.PhaseCommand;
import ai.timefold.solver.core.config.phase.custom.CustomPhaseConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.phase.AbstractPhaseFactory;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.termination.SolverTermination;

public class DefaultCustomPhaseFactory<Solution_> extends AbstractPhaseFactory<Solution_, CustomPhaseConfig> {

    public DefaultCustomPhaseFactory(CustomPhaseConfig phaseConfig) {
        super(phaseConfig);
    }

    @Override
    public CustomPhase<Solution_> buildPhase(int phaseIndex, boolean lastInitializingPhase,
            HeuristicConfigPolicy<Solution_> solverConfigPolicy, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            SolverTermination<Solution_> solverTermination) {
        var phaseConfigPolicy = solverConfigPolicy.createPhaseConfigPolicy();
        var customPhaseCommandClassList = phaseConfig.getCustomPhaseCommandClassList();
        var customPhaseCommandList = phaseConfig.getCustomPhaseCommandList();
        if (ConfigUtils.isEmptyCollection(customPhaseCommandClassList)
                && ConfigUtils.isEmptyCollection(customPhaseCommandList)) {
            throw new IllegalArgumentException(
                    "Configure at least 1 <customPhaseCommandClass> in the <customPhase> configuration.");
        }

        var customPhaseCommandList_ = new ArrayList<PhaseCommand<Solution_>>(getCustomPhaseCommandListSize());
        if (customPhaseCommandClassList != null) {
            for (var customPhaseCommandClass : customPhaseCommandClassList) {
                if (customPhaseCommandClass == null) {
                    throw new IllegalArgumentException("""
                            The customPhaseCommandClass (%s) cannot be null in the customPhase (%s).
                            Maybe there was a typo in the class name provided in the solver config XML?"""
                            .formatted(customPhaseCommandClass, phaseConfig));
                }
                customPhaseCommandList_.add(createCustomPhaseCommand(customPhaseCommandClass));
            }
        }
        if (customPhaseCommandList != null) {
            customPhaseCommandList_.addAll((Collection) customPhaseCommandList);
        }
        return new DefaultCustomPhase.DefaultCustomPhaseBuilder<>(phaseIndex, lastInitializingPhase,
                solverConfigPolicy.getLogIndentation(), buildPhaseTermination(phaseConfigPolicy, solverTermination),
                customPhaseCommandList_)
                .enableAssertions(phaseConfigPolicy.getEnvironmentMode())
                .build();
    }

    private PhaseCommand<Solution_> createCustomPhaseCommand(Class<? extends PhaseCommand> customPhaseCommandClass) {
        PhaseCommand<Solution_> customPhaseCommand =
                ConfigUtils.newInstance(phaseConfig, "customPhaseCommandClass", customPhaseCommandClass);
        ConfigUtils.applyCustomProperties(customPhaseCommand, "customPhaseCommandClass", phaseConfig.getCustomProperties(),
                "customProperties");
        return customPhaseCommand;
    }

    private int getCustomPhaseCommandListSize() {
        var customPhaseCommandClassList = phaseConfig.getCustomPhaseCommandClassList();
        var customPhaseCommandList = phaseConfig.getCustomPhaseCommandList();
        return (customPhaseCommandClassList == null ? 0 : customPhaseCommandClassList.size())
                + (customPhaseCommandList == null ? 0 : customPhaseCommandList.size());
    }
}
