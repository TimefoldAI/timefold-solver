package ai.timefold.solver.core.impl.phase.custom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ai.timefold.solver.core.config.phase.custom.CustomPhaseConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.phase.AbstractPhaseFactory;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.termination.Termination;

public class DefaultCustomPhaseFactory<Solution_> extends AbstractPhaseFactory<Solution_, CustomPhaseConfig> {

    public DefaultCustomPhaseFactory(CustomPhaseConfig phaseConfig) {
        super(phaseConfig);
    }

    @Override
    public CustomPhase<Solution_> buildPhase(int phaseIndex, boolean triggerFirstInitializedSolutionEvent,
            HeuristicConfigPolicy<Solution_> solverConfigPolicy, BestSolutionRecaller<Solution_> bestSolutionRecaller,
            Termination<Solution_> solverTermination) {
        HeuristicConfigPolicy<Solution_> phaseConfigPolicy = solverConfigPolicy.createPhaseConfigPolicy();
        if (ConfigUtils.isEmptyCollection(phaseConfig.getCustomPhaseCommandClassList())
                && ConfigUtils.isEmptyCollection(phaseConfig.getCustomPhaseCommandList())) {
            throw new IllegalArgumentException(
                    "Configure at least 1 <customPhaseCommandClass> in the <customPhase> configuration.");
        }

        List<CustomPhaseCommand<Solution_>> customPhaseCommandList_ = new ArrayList<>(getCustomPhaseCommandListSize());
        if (phaseConfig.getCustomPhaseCommandClassList() != null) {
            for (Class<? extends CustomPhaseCommand> customPhaseCommandClass : phaseConfig.getCustomPhaseCommandClassList()) {
                if (customPhaseCommandClass == null) {
                    throw new IllegalArgumentException("The customPhaseCommandClass (" + customPhaseCommandClass
                            + ") cannot be null in the customPhase (" + phaseConfig + ").\n" +
                            "Maybe there was a typo in the class name provided in the solver config XML?");
                }
                customPhaseCommandList_.add(createCustomPhaseCommand(customPhaseCommandClass));
            }
        }
        if (phaseConfig.getCustomPhaseCommandList() != null) {
            customPhaseCommandList_.addAll((Collection) phaseConfig.getCustomPhaseCommandList());
        }
        DefaultCustomPhase.Builder<Solution_> builder =
                new DefaultCustomPhase.Builder<>(phaseIndex, triggerFirstInitializedSolutionEvent,
                        solverConfigPolicy.getLogIndentation(),
                        buildPhaseTermination(phaseConfigPolicy, solverTermination), customPhaseCommandList_);
        EnvironmentMode environmentMode = phaseConfigPolicy.getEnvironmentMode();
        if (environmentMode.isNonIntrusiveFullAsserted()) {
            builder.setAssertStepScoreFromScratch(true);
        }
        return builder.build();
    }

    private CustomPhaseCommand<Solution_>
            createCustomPhaseCommand(Class<? extends CustomPhaseCommand> customPhaseCommandClass) {
        CustomPhaseCommand<Solution_> customPhaseCommand = ConfigUtils.newInstance(phaseConfig,
                "customPhaseCommandClass", customPhaseCommandClass);
        ConfigUtils.applyCustomProperties(customPhaseCommand, "customPhaseCommandClass", phaseConfig.getCustomProperties(),
                "customProperties");
        return customPhaseCommand;
    }

    private int getCustomPhaseCommandListSize() {
        return (phaseConfig.getCustomPhaseCommandClassList() == null ? 0 : phaseConfig.getCustomPhaseCommandClassList().size())
                + (phaseConfig.getCustomPhaseCommandList() == null ? 0 : phaseConfig.getCustomPhaseCommandList().size());
    }
}
