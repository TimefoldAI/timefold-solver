package ai.timefold.solver.core.impl.phase;

import java.util.Collections;
import java.util.Objects;

import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.exhaustivesearch.ExhaustiveSearchPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.partitionedsearch.PartitionedSearchPhaseConfig;
import ai.timefold.solver.core.config.phase.PhaseConfig;
import ai.timefold.solver.core.config.phase.custom.CustomPhaseConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.constructionheuristic.scope.ConstructionHeuristicPhaseScope;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchPhaseScope;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.phase.custom.scope.CustomPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.solver.termination.SolverTermination;
import ai.timefold.solver.core.impl.solver.termination.TerminationFactory;
import ai.timefold.solver.core.impl.solver.termination.UniversalTermination;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPhaseFactory<Solution_, PhaseConfig_ extends PhaseConfig<PhaseConfig_>>
        implements PhaseFactory<Solution_> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final PhaseConfig_ phaseConfig;

    public AbstractPhaseFactory(PhaseConfig_ phaseConfig) {
        this.phaseConfig = phaseConfig;
    }

    protected PhaseTermination<Solution_> buildPhaseTermination(HeuristicConfigPolicy<Solution_> configPolicy,
            SolverTermination<Solution_> solverTermination) {
        var terminationConfig_ = Objects.requireNonNullElseGet(phaseConfig.getTerminationConfig(), TerminationConfig::new);
        var phaseTermination = PhaseTermination.bridge(solverTermination);
        var resultingTermination = TerminationFactory.<Solution_> create(terminationConfig_)
                .buildTermination(configPolicy, phaseTermination);
        var inapplicableTerminationList = !(this instanceof NoChangePhaseFactory<?>) &&
                resultingTermination instanceof UniversalTermination<Solution_> universalTermination
                        ? universalTermination.getPhaseTerminationsInapplicableTo(getPhaseScopeClass())
                        : Collections.emptyList();
        var phaseName = this.getClass().getSimpleName()
                .replace("PhaseFactory", "")
                .replace("Default", "");
        if (solverTermination != resultingTermination) {
            // Only fail if the user put the inapplicable termination on the phase, not on the solver.
            // On the solver level, inapplicable phase terminations are skipped.
            // Otherwise you would only be able to configure a global phase-level termination on the solver
            // if it was applicable to all phases.
            if (!inapplicableTerminationList.isEmpty()) {
                throw new IllegalStateException(
                        """
                                The phase (%s) configured with terminations (%s) includes some terminations which are not applicable to it (%s).
                                Maybe remove these terminations from the phase's configuration."""
                                .formatted(phaseName, phaseTermination, inapplicableTerminationList));
            }
        } else if (!inapplicableTerminationList.isEmpty()) {
            logger.trace("""
                    The solver-level termination ({}) includes phase-level terminations ({}) \
                    which are not applicable to the phase ({}).
                    These phase-level terminations will not take effect in this phase.""",
                    solverTermination, inapplicableTerminationList, phaseName);
        }
        return resultingTermination;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private Class<? extends AbstractPhaseScope> getPhaseScopeClass() {
        if (phaseConfig instanceof ConstructionHeuristicPhaseConfig) {
            return ConstructionHeuristicPhaseScope.class;
        } else if (phaseConfig instanceof CustomPhaseConfig) {
            return CustomPhaseScope.class;
        } else if (phaseConfig instanceof LocalSearchPhaseConfig) {
            return LocalSearchPhaseScope.class;
        } else if (phaseConfig instanceof ExhaustiveSearchPhaseConfig) {
            return ExhaustiveSearchPhaseScope.class;
        } else if (phaseConfig instanceof PartitionedSearchPhaseConfig) {
            try {
                return (Class<? extends AbstractPhaseScope>) Class
                        .forName("ai.timefold.solver.enterprise.core.partitioned.PartitionedSearchPhaseScope");
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("""
                        The class (%s) is not found.
                        Make sure Timefold Solver Enterprise Edition is on the classpath, or disable partitioned search.
                        """
                        .formatted("ai.timefold.solver.enterprise.core.partitioned.PartitionedSearchPhaseScope"));
            }
        } else {
            throw new IllegalStateException("Unsupported phaseConfig class: %s".formatted(phaseConfig.getClass()));
        }
    }

}
