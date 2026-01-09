package ai.timefold.solver.core.impl.exhaustivesearch;

import java.util.Comparator;
import java.util.TreeSet;
import java.util.function.IntFunction;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.solver.event.EventProducerId;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.exhaustivesearch.node.ExhaustiveSearchNode;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchPhaseScope;
import ai.timefold.solver.core.impl.exhaustivesearch.scope.ExhaustiveSearchStepScope;
import ai.timefold.solver.core.impl.phase.AbstractPhase;
import ai.timefold.solver.core.impl.phase.PhaseType;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;

/**
 * Default implementation of {@link ExhaustiveSearchPhase}.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class DefaultExhaustiveSearchPhase<Solution_> extends AbstractPhase<Solution_>
        implements ExhaustiveSearchPhase<Solution_> {

    private final Comparator<ExhaustiveSearchNode> nodeComparator;
    private final ExhaustiveSearchStrategy<Solution_> strategy;

    private DefaultExhaustiveSearchPhase(Builder<Solution_> builder) {
        super(builder);
        nodeComparator = builder.nodeComparator;
        strategy = builder.strategy;
    }

    @Override
    public PhaseType getPhaseType() {
        return PhaseType.EXHAUSTIVE_SEARCH;
    }

    @Override
    public IntFunction<EventProducerId> getEventProducerIdSupplier() {
        return EventProducerId::exhaustiveSearch;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void solve(SolverScope<Solution_> solverScope) {
        var expandableNodeQueue = new TreeSet<>(nodeComparator);
        var phaseScope = new ExhaustiveSearchPhaseScope<>(solverScope, phaseIndex);
        phaseScope.setExpandableNodeQueue(expandableNodeQueue);
        phaseStarted(phaseScope);

        while (!expandableNodeQueue.isEmpty() && !phaseTermination.isPhaseTerminated(phaseScope)) {
            var stepScope = strategy.prepareStep(phaseScope);
            stepStarted(stepScope);
            strategy.solveStep(stepScope);
            stepEnded(stepScope);
            phaseScope.setLastCompletedStepScope(stepScope);
        }
        phaseEnded(phaseScope);
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        strategy.solvingStarted(solverScope);
    }

    private void phaseStarted(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        strategy.phaseStarted(phaseScope);
    }

    private void stepStarted(ExhaustiveSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        strategy.stepStarted(stepScope);
    }

    private void stepEnded(ExhaustiveSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        strategy.stepEnded(stepScope);
        if (logger.isDebugEnabled()) {
            var phaseScope = stepScope.getPhaseScope();
            logger.debug("{}    ES step ({}), time spent ({}), treeId ({}), {} best score ({}), selected move count ({}).",
                    logIndentation,
                    stepScope.getStepIndex(),
                    phaseScope.calculateSolverTimeMillisSpentUpToNow(),
                    stepScope.getTreeId(),
                    (stepScope.getBestScoreImproved() ? "new" : "   "),
                    phaseScope.getBestScore().raw(),
                    stepScope.getSelectedMoveCount());
        }
    }

    private void phaseEnded(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        strategy.phaseEnded(phaseScope);
        phaseScope.endingNow();
        logger.info("{}Exhaustive Search phase ({}) ended: time spent ({}), best score ({}),"
                + " move evaluation speed ({}/sec), step total ({}).",
                logIndentation,
                phaseIndex,
                phaseScope.calculateSolverTimeMillisSpentUpToNow(),
                phaseScope.getBestScore().raw(),
                phaseScope.getPhaseMoveEvaluationSpeed(),
                phaseScope.getNextStepIndex());
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        strategy.solvingEnded(solverScope);
    }

    public static class Builder<Solution_> extends AbstractPhaseBuilder<Solution_> {

        private final Comparator<ExhaustiveSearchNode> nodeComparator;
        private final ExhaustiveSearchStrategy<Solution_> strategy;

        public Builder(int phaseIndex, String logIndentation, PhaseTermination<Solution_> phaseTermination,
                Comparator<ExhaustiveSearchNode> nodeComparator, ExhaustiveSearchStrategy<Solution_> strategy) {
            super(phaseIndex, logIndentation, phaseTermination);
            this.nodeComparator = nodeComparator;
            this.strategy = strategy;
        }

        @Override
        public Builder<Solution_> enableAssertions(EnvironmentMode environmentMode) {
            super.enableAssertions(environmentMode);
            strategy.enableAssertions(environmentMode);
            return this;
        }

        @Override
        public DefaultExhaustiveSearchPhase<Solution_> build() {
            return new DefaultExhaustiveSearchPhase<>(this);
        }
    }
}
