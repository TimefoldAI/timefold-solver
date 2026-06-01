package ai.timefold.solver.core.impl.solver.recaller;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.api.solver.event.EventProducerId;
import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.solver.event.SolverEventSupport;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.preview.api.move.Move;

import org.jspecify.annotations.Nullable;

/**
 * Remembers the {@link PlanningSolution best solution} that a {@link Solver} encounters.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class BestSolutionRecaller<Solution_> extends PhaseLifecycleListenerAdapter<Solution_> {

    protected boolean assertInitialScoreFromScratch = false;
    protected boolean assertShadowVariablesAreNotStale = false;
    protected boolean assertBestScoreIsUnmodified = false;
    protected boolean reuseBestSolution = false;
    protected SolverEventSupport<Solution_> solverEventSupport;
    protected ReusingBestSolutionUpdater<Solution_> reusingBestSolutionUpdater;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    public void setAssertInitialScoreFromScratch(boolean assertInitialScoreFromScratch) {
        this.assertInitialScoreFromScratch = assertInitialScoreFromScratch;
    }

    public void setAssertShadowVariablesAreNotStale(boolean assertShadowVariablesAreNotStale) {
        this.assertShadowVariablesAreNotStale = assertShadowVariablesAreNotStale;
    }

    public void setAssertBestScoreIsUnmodified(boolean assertBestScoreIsUnmodified) {
        this.assertBestScoreIsUnmodified = assertBestScoreIsUnmodified;
    }

    public void setReuseBestSolution(boolean reuseBestSolution) {
        this.reuseBestSolution = reuseBestSolution;
        if (reuseBestSolution) {
            reusingBestSolutionUpdater = TimefoldSolverEnterpriseService
                    .loadOrFail(TimefoldSolverEnterpriseService.Feature.REUSE_BEST_SOLUTION)
                    .buildReusingBestSolutionUpdater(readWriteLock);
        }
    }

    public boolean isReuseBestSolution() {
        return reuseBestSolution;
    }

    public void setSolverEventSupport(SolverEventSupport<Solution_> solverEventSupport) {
        this.solverEventSupport = solverEventSupport;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        // Starting bestSolution is already set by Solver.solve(Solution)
        var scoreDirector = solverScope.getScoreDirector();
        InnerScore innerScore = scoreDirector.calculateScore();
        var score = innerScore.raw();
        solverScope.setBestScore(innerScore);
        solverScope.setBestSolutionTimeMillis(solverScope.getClock().millis());
        // The original bestSolution might be the final bestSolution and should have an accurate Score
        solverScope.getSolutionDescriptor().setScore(solverScope.getBestSolution(), score);
        if (innerScore.isFullyAssigned()) {
            solverScope.setStartingInitializedScore(innerScore.raw());
        } else {
            solverScope.setStartingInitializedScore(null);
        }
        if (assertInitialScoreFromScratch) {
            scoreDirector.assertWorkingScoreFromScratch(innerScore, "Initial score calculated");
        }
        if (assertShadowVariablesAreNotStale) {
            scoreDirector.assertShadowVariablesAreNotStale(innerScore, "Initial score calculated");
        }
    }

    public void processWorkingSolutionDuringConstructionHeuristicsStep(AbstractStepScope<Solution_> stepScope) {
        AbstractPhaseScope<Solution_> phaseScope = stepScope.getPhaseScope();
        SolverScope<Solution_> solverScope = phaseScope.getSolverScope();
        stepScope.setBestScoreImproved(true);
        phaseScope.setBestSolutionStepIndex(stepScope.getStepIndex());
        Solution_ newBestSolution = stepScope.getWorkingSolution();
        // Construction heuristics don't fire intermediate best solution changed events.
        // But the best solution and score are updated, so that unimproved* terminations work correctly.
        updateBestSolutionWithoutFiring(solverScope, stepScope.getScore(), newBestSolution);
    }

    public void processWorkingSolutionDuringStep(AbstractStepScope<Solution_> stepScope) {
        processWorkingSolutionDuringStep(stepScope, null);
    }

    /**
     * Return true if best solution was updated
     */
    public <Score_ extends Score<Score_>> void processWorkingSolutionDuringStep(AbstractStepScope<Solution_> stepScope,
            @Nullable List<Move<Solution_>> acceptedMoveList) {
        var phaseScope = stepScope.getPhaseScope();
        var score = stepScope.<Score_> getScore();
        var solverScope = phaseScope.getSolverScope();
        var bestScoreImproved = score.compareTo(solverScope.getBestScore()) > 0;
        stepScope.setBestScoreImproved(bestScoreImproved);
        if (bestScoreImproved) {
            phaseScope.setBestSolutionStepIndex(stepScope.getStepIndex());
            if (reuseBestSolution && acceptedMoveList != null) {
                updateBestSolutionAndFire(solverScope, phaseScope, score, acceptedMoveList);
            } else {
                var newBestSolution = stepScope.cloneWorkingSolution();
                // Can this be removed? Seems to be the same as score?
                var innerScore =
                        buildInnerScore(solverScope.getSolutionDescriptor().<Score_> getScore(newBestSolution),
                                stepScope.getScoreDirector().getWorkingInitScore(), true);
                updateBestSolutionAndFire(solverScope, phaseScope, innerScore, newBestSolution);
            }
        } else if (assertBestScoreIsUnmodified) {
            solverScope.assertScoreFromScratch(solverScope.getBestSolution());
        }
    }

    public <Score_ extends Score<Score_>> void processWorkingSolutionDuringMove(InnerScore<Score_> moveScore,
            AbstractStepScope<Solution_> stepScope) {
        processWorkingSolutionDuringMove(moveScore, stepScope, null);
    }

    public <Score_ extends Score<Score_>> void processWorkingSolutionDuringMove(InnerScore<Score_> moveScore,
            AbstractStepScope<Solution_> stepScope, @Nullable List<Move<Solution_>> acceptedMoveList) {
        var phaseScope = stepScope.getPhaseScope();
        var solverScope = phaseScope.getSolverScope();
        var bestScoreImproved = moveScore.compareTo(solverScope.getBestScore()) > 0;
        // The method processWorkingSolutionDuringMove() is called 0..* times
        // stepScope.getBestScoreImproved() is initialized on false before the first call here
        if (bestScoreImproved) {
            stepScope.setBestScoreImproved(bestScoreImproved);
            phaseScope.setBestSolutionStepIndex(stepScope.getStepIndex());
            // Can this be removed? Seems to be the same as moveScore?
            var innerScore = buildInnerScore(moveScore.raw(), solverScope.getScoreDirector().getWorkingInitScore(),
                    solverScope.getScoreDirector().getSolutionDescriptor().hasBothBasicAndListVariables());
            if (reuseBestSolution && acceptedMoveList != null) {
                updateBestSolutionAndFire(solverScope, phaseScope, innerScore, acceptedMoveList);
            } else {
                var newBestSolution = solverScope.getScoreDirector().cloneWorkingSolution();
                // The solution for mixed models can generate a partially solved solution,
                // as the complete solution will only be achieved when all variable types are assigned.
                updateBestSolutionAndFire(solverScope, phaseScope, innerScore, newBestSolution);
            }
        } else if (assertBestScoreIsUnmodified) {
            solverScope.assertScoreFromScratch(solverScope.getBestSolution());
        }
    }

    public void updateBestSolutionAndFire(SolverScope<Solution_> solverScope, AbstractPhaseScope<Solution_> phaseScope) {
        updateBestSolutionWithoutFiring(solverScope);
        solverEventSupport.fireBestSolutionChanged(solverScope, phaseScope.getPhaseId(), solverScope.getBestSolution(),
                readWriteLock.readLock());
    }

    public void updateBestSolutionAndFireIfInitialized(SolverScope<Solution_> solverScope,
            EventProducerId eventProducerId) {
        updateBestSolutionWithoutFiring(solverScope);
        if (solverScope.isBestSolutionInitialized()) {
            solverEventSupport.fireBestSolutionChanged(solverScope, eventProducerId, solverScope.getBestSolution(),
                    readWriteLock.readLock());
        }
    }

    private void updateBestSolutionAndFire(SolverScope<Solution_> solverScope, AbstractPhaseScope<Solution_> phaseScope,
            InnerScore<?> bestScore,
            Solution_ bestSolution) {
        updateBestSolutionWithoutFiring(solverScope, bestScore, bestSolution);
        solverEventSupport.fireBestSolutionChanged(solverScope, phaseScope.getPhaseId(), bestSolution,
                readWriteLock.readLock());
    }

    private <Score_ extends Score<Score_>> void updateBestSolutionAndFire(SolverScope<Solution_> solverScope,
            AbstractPhaseScope<Solution_> phaseScope,
            InnerScore<Score_> score, List<Move<Solution_>> acceptedMoveList) {
        reusingBestSolutionUpdater.updateReusingBestSolution(solverScope, score, acceptedMoveList);
        updateBestSolutionWithoutFiring(solverScope, score, reusingBestSolutionUpdater.getBestSolution());
        solverEventSupport.fireBestSolutionChanged(solverScope, phaseScope.getPhaseId(),
                reusingBestSolutionUpdater.getBestSolution(), readWriteLock.readLock());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void updateBestSolutionWithoutFiring(SolverScope<Solution_> solverScope) {
        // We clone the existing working solution to set it as the best current solution
        var newBestSolution = solverScope.getScoreDirector().cloneWorkingSolution();
        var newBestScore = solverScope.getSolutionDescriptor().<Score> getScore(newBestSolution);
        var innerScore = buildInnerScore(newBestScore, solverScope.getScoreDirector().getWorkingInitScore(), true);
        updateBestSolutionWithoutFiring(solverScope, innerScore, newBestSolution);
    }

    private void updateBestSolutionWithoutFiring(SolverScope<Solution_> solverScope, InnerScore<?> bestScore,
            Solution_ bestSolution) {
        if (bestScore.isFullyAssigned() && !solverScope.isBestSolutionInitialized()) {
            solverScope.setStartingInitializedScore(bestScore.raw());
        }

        solverScope.setBestSolution(bestSolution);
        solverScope.setBestScore(bestScore);
        solverScope.setBestSolutionTimeMillis(solverScope.getClock().millis());
    }

    private static <Score_ extends Score<Score_>> InnerScore<Score_> buildInnerScore(Score_ moveScore, int uninitializedScore,
            boolean acceptUnassigned) {
        if (acceptUnassigned) {
            var adjustedUninitializedScore = uninitializedScore < 0 ? -(uninitializedScore) : uninitializedScore;
            return InnerScore.withUnassignedCount(moveScore, adjustedUninitializedScore);
        } else {
            return new InnerScore<>(moveScore, uninitializedScore);
        }
    }

}
