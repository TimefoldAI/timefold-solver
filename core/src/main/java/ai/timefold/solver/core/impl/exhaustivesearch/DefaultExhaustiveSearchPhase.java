package ai.timefold.solver.core.impl.exhaustivesearch;

import java.util.Comparator;
import java.util.TreeSet;
import java.util.function.IntFunction;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.event.EventProducerId;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.exhaustivesearch.decider.AbstractExhaustiveSearchDecider;
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

    protected final Comparator<ExhaustiveSearchNode<Solution_>> nodeComparator;
    protected final AbstractExhaustiveSearchDecider<Solution_, ? extends Score<?>> decider;

    protected final boolean assertWorkingSolutionScoreFromScratch;
    protected final boolean assertExpectedWorkingSolutionScore;

    private DefaultExhaustiveSearchPhase(Builder<Solution_> builder) {
        super(builder);
        nodeComparator = builder.nodeComparator;
        decider = builder.decider;

        assertWorkingSolutionScoreFromScratch = builder.assertWorkingSolutionScoreFromScratch;
        assertExpectedWorkingSolutionScore = builder.assertExpectedWorkingSolutionScore;
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
            var stepScope = new ExhaustiveSearchStepScope<>(phaseScope);
            var node = expandableNodeQueue.last();
            expandableNodeQueue.remove(node);
            stepScope.setExpandingNode(node);
            stepStarted(stepScope);
            decider.restoreWorkingSolution(stepScope, assertWorkingSolutionScoreFromScratch,
                    assertExpectedWorkingSolutionScore);
            decider.expandNode(stepScope);
            stepEnded(stepScope);
            phaseScope.setLastCompletedStepScope(stepScope);
        }
        phaseEnded(phaseScope);
    }

    // ************************************************************************
    // Lifecycle methods
    // ************************************************************************

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        super.solvingStarted(solverScope);
        decider.solvingStarted(solverScope);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        super.solvingEnded(solverScope);
        decider.solvingEnded(solverScope);
    }

    private void phaseStarted(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        decider.phaseStarted(phaseScope);
    }

    private void phaseEnded(ExhaustiveSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        decider.phaseEnded(phaseScope);
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

    private void stepStarted(ExhaustiveSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        decider.stepStarted(stepScope);
    }

    private void stepEnded(ExhaustiveSearchStepScope<Solution_> stepScope) {
        super.stepEnded(stepScope);
        // Skip entitySelector.stepEnded(stepScope)
        decider.stepEnded(stepScope);
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

    public static class Builder<Solution_> extends AbstractPhaseBuilder<Solution_> {

        private final Comparator<ExhaustiveSearchNode<Solution_>> nodeComparator;
        private final AbstractExhaustiveSearchDecider<Solution_, ? extends Score<?>> decider;

        private boolean assertWorkingSolutionScoreFromScratch = false;
        private boolean assertExpectedWorkingSolutionScore = false;

        public Builder(int phaseIndex, String logIndentation, PhaseTermination<Solution_> phaseTermination,
                Comparator<ExhaustiveSearchNode<Solution_>> nodeComparator,
                AbstractExhaustiveSearchDecider<Solution_, ? extends Score<?>> decider) {
            super(phaseIndex, logIndentation, phaseTermination);
            this.nodeComparator = nodeComparator;
            this.decider = decider;
        }

        @Override
        public Builder<Solution_> enableAssertions(EnvironmentMode environmentMode) {
            super.enableAssertions(environmentMode);
            assertWorkingSolutionScoreFromScratch = environmentMode.isFullyAsserted();
            assertExpectedWorkingSolutionScore = environmentMode.isIntrusivelyAsserted();
            return this;
        }

        @Override
        public DefaultExhaustiveSearchPhase<Solution_> build() {
            return new DefaultExhaustiveSearchPhase<>(this);
        }
    }
}
