package ai.timefold.solver.core.impl.localsearch.decider.perturbation;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.heuristic.move.LegacyMoveAdapter;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.move.director.MoveDirector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoveSelectorPerturbationStrategy<Solution_> implements PerturbationStrategy<Solution_> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private final MoveSelector<Solution_> perturbationMoveSelector;
    private final int maxLevels;
    private Score<?> currentBestScore;
    private int level;

    public MoveSelectorPerturbationStrategy(MoveSelector<Solution_> perturbationMoveSelector, int maxLevels) {
        this.perturbationMoveSelector = perturbationMoveSelector;
        this.maxLevels = maxLevels;
    }

    @Override
    public <Score_ extends Score<Score_>> Score_ apply(AbstractStepScope<Solution_> stepScope) {
        var phaseScope = stepScope.getPhaseScope();
        var solverScope = phaseScope.getSolverScope();
        logger.debug("Resetting working solution, score ({})", solverScope.getBestScore());
        solverScope.setWorkingSolutionFromBestSolution();
        var perturbationMoveIterator = perturbationMoveSelector.iterator();
        InnerScoreDirector<Solution_, Score_> scoreDirector = phaseScope.getScoreDirector();
        MoveDirector<Solution_> moveDirector = stepScope.getMoveDirector();
        for (int i = 0; i < level; i++) {
            if (perturbationMoveIterator.hasNext()) {
                var perturbation = new LegacyMoveAdapter<>(perturbationMoveIterator.next());
                if (!LegacyMoveAdapter.isDoable(moveDirector, perturbation)) {
                    continue;
                }
                perturbation.execute(moveDirector);
                logger.debug("Generating a perturbation: Move ({})", perturbation);
            }
        }
        var currentScore = scoreDirector.calculateScore();
        logger.debug("New solution generated: old score ({}), new score ({})",
                phaseScope.getLastCompletedStepScope().getScore(), currentScore);
        if (level + 1 < maxLevels) {
            level++;
        }
        return currentScore;
    }

    @Override
    public void stepStarted(AbstractStepScope<Solution_> stepScope) {
        this.perturbationMoveSelector.stepStarted(stepScope);
        this.currentBestScore = stepScope.getPhaseScope().getBestScore();
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void stepEnded(AbstractStepScope<Solution_> stepScope) {
        this.perturbationMoveSelector.stepEnded(stepScope);
        // Always cancel it at the end of the step as the perturbation is already applied
        stepScope.getPhaseScope().cancelReconfiguration();
        if (((Score) stepScope.getScore()).compareTo(currentBestScore) > 0) {
            // Reset it if the best solution is improved
            this.level = 1;
        }
    }

    @Override
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        this.level = 1;
        perturbationMoveSelector.phaseStarted(phaseScope);
    }

    @Override
    public void phaseEnded(AbstractPhaseScope<Solution_> phaseScope) {
        perturbationMoveSelector.phaseEnded(phaseScope);
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        perturbationMoveSelector.solvingStarted(solverScope);
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        perturbationMoveSelector.solvingEnded(solverScope);
    }
}
