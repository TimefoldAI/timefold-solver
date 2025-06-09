package ai.timefold.solver.core.impl.solver.recaller;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.Solver;
import ai.timefold.solver.core.impl.phase.event.PhaseLifecycleListenerAdapter;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.solver.event.SolverEventSupport;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

/**
 * Remembers the {@link PlanningSolution best solution} that a {@link Solver} encounters.
 *
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class BestSolutionRecaller<Solution_> extends PhaseLifecycleListenerAdapter<Solution_> {

    protected boolean assertInitialScoreFromScratch = false;
    protected boolean assertShadowVariablesAreNotStale = false;
    protected boolean assertBestScoreIsUnmodified = false;

    protected SolverEventSupport<Solution_> solverEventSupport;

    public void setAssertInitialScoreFromScratch(boolean assertInitialScoreFromScratch) {
        this.assertInitialScoreFromScratch = assertInitialScoreFromScratch;
    }

    public void setAssertShadowVariablesAreNotStale(boolean assertShadowVariablesAreNotStale) {
        this.assertShadowVariablesAreNotStale = assertShadowVariablesAreNotStale;
    }

    public void setAssertBestScoreIsUnmodified(boolean assertBestScoreIsUnmodified) {
        this.assertBestScoreIsUnmodified = assertBestScoreIsUnmodified;
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

    public <Score_ extends Score<Score_>> void processWorkingSolutionDuringStep(AbstractStepScope<Solution_> stepScope) {
        var phaseScope = stepScope.getPhaseScope();
        var score = stepScope.<Score_> getScore();
        var solverScope = phaseScope.getSolverScope();
        var bestScoreImproved = score.compareTo(solverScope.getBestScore()) > 0;
        stepScope.setBestScoreImproved(bestScoreImproved);
        if (bestScoreImproved) {
            phaseScope.setBestSolutionStepIndex(stepScope.getStepIndex());
            var newBestSolution = stepScope.createOrGetClonedSolution();
            var innerScore = InnerScore.withUnassignedCount(
                    solverScope.getSolutionDescriptor().<Score_> getScore(newBestSolution),
                    -stepScope.getScoreDirector().getWorkingInitScore());
            updateBestSolutionAndFire(solverScope, innerScore, newBestSolution);
        } else if (assertBestScoreIsUnmodified) {
            solverScope.assertScoreFromScratch(solverScope.getBestSolution());
        }
    }

    public <Score_ extends Score<Score_>> void processWorkingSolutionDuringMove(InnerScore<Score_> moveScore,
            AbstractStepScope<Solution_> stepScope) {
        var phaseScope = stepScope.getPhaseScope();
        var solverScope = phaseScope.getSolverScope();
        var bestScoreImproved = moveScore.compareTo(solverScope.getBestScore()) > 0;
        // The method processWorkingSolutionDuringMove() is called 0..* times
        // stepScope.getBestScoreImproved() is initialized on false before the first call here
        if (bestScoreImproved) {
            stepScope.setBestScoreImproved(bestScoreImproved);
        }
        if (bestScoreImproved) {
            phaseScope.setBestSolutionStepIndex(stepScope.getStepIndex());
            var newBestSolution = solverScope.getScoreDirector().cloneWorkingSolution();
            var innerScore = new InnerScore<>(moveScore.raw(), solverScope.getScoreDirector().getWorkingInitScore());
            updateBestSolutionAndFire(solverScope, innerScore, newBestSolution);
        } else if (assertBestScoreIsUnmodified) {
            solverScope.assertScoreFromScratch(solverScope.getBestSolution());
        }
    }

    public void updateBestSolutionAndFire(SolverScope<Solution_> solverScope) {
        updateBestSolutionWithoutFiring(solverScope);
        solverEventSupport.fireBestSolutionChanged(solverScope, solverScope.getBestSolution());
    }

    public void updateBestSolutionAndFireIfInitialized(SolverScope<Solution_> solverScope) {
        updateBestSolutionWithoutFiring(solverScope);
        if (solverScope.isBestSolutionInitialized()) {
            solverEventSupport.fireBestSolutionChanged(solverScope, solverScope.getBestSolution());
        }
    }

    private void updateBestSolutionAndFire(SolverScope<Solution_> solverScope, InnerScore<?> bestScore,
            Solution_ bestSolution) {
        updateBestSolutionWithoutFiring(solverScope, bestScore, bestSolution);
        solverEventSupport.fireBestSolutionChanged(solverScope, bestSolution);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void updateBestSolutionWithoutFiring(SolverScope<Solution_> solverScope) {
        // We clone the existing working solution to set it as the best current solution
        var newBestSolution = solverScope.getScoreDirector().cloneWorkingSolution();
        var newBestScore = solverScope.getSolutionDescriptor().<Score> getScore(newBestSolution);
        var innerScore = InnerScore.withUnassignedCount(newBestScore, -solverScope.getScoreDirector().getWorkingInitScore());
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

}