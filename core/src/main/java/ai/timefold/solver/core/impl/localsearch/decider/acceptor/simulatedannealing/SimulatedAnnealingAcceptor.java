package ai.timefold.solver.core.impl.localsearch.decider.acceptor.simulatedannealing;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.AbstractAcceptor;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;

/**
 * The time gradient implementation of simulated annealing.
 */
public class SimulatedAnnealingAcceptor<Solution_> extends AbstractAcceptor<Solution_> {

    protected Score startingTemperature;

    protected int levelsLength = -1;
    protected double[] startingTemperatureLevels;
    // No protected Score temperature do avoid rounding errors when using Score.multiply(double)
    protected double[] temperatureLevels;

    protected double temperatureMinimum = 1.0E-100; // Double.MIN_NORMAL is E-308

    public void setStartingTemperature(Score startingTemperature) {
        this.startingTemperature = startingTemperature;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseStarted(phaseScope);
        for (var startingTemperatureLevel : startingTemperature.toLevelDoubles()) {
            if (startingTemperatureLevel < 0.0) {
                throw new IllegalArgumentException("The startingTemperature (" + startingTemperature
                        + ") cannot have negative level (" + startingTemperatureLevel + ").");
            }
        }
        startingTemperatureLevels = startingTemperature.toLevelDoubles();
        temperatureLevels = startingTemperatureLevels;
        levelsLength = startingTemperatureLevels.length;
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        super.phaseEnded(phaseScope);
        startingTemperatureLevels = null;
        temperatureLevels = null;
        levelsLength = -1;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public boolean isAccepted(LocalSearchMoveScope<Solution_> moveScope) {
        var phaseScope = moveScope.getStepScope().getPhaseScope();
        // Guaranteed local search; no need for InnerScore.
        Score lastStepScore = phaseScope.getLastCompletedStepScope().getScore().raw();
        Score moveScore = moveScope.getScore().raw();
        if (moveScore.compareTo(lastStepScore) >= 0) {
            return true;
        }
        var moveScoreDifference = lastStepScore.subtract(moveScore);
        var moveScoreDifferenceLevels = moveScoreDifference.toLevelDoubles();
        var acceptChance = 1.0;
        for (var i = 0; i < levelsLength; i++) {
            var moveScoreDifferenceLevel = moveScoreDifferenceLevels[i];
            var temperatureLevel = temperatureLevels[i];
            double acceptChanceLevel;
            if (moveScoreDifferenceLevel <= 0.0) {
                // In this level, moveScore is better than the lastStepScore, so do not disrupt the acceptChance
                acceptChanceLevel = 1.0;
            } else {
                acceptChanceLevel = Math.exp(-moveScoreDifferenceLevel / temperatureLevel);
            }
            acceptChance *= acceptChanceLevel;
        }
        return moveScope.getWorkingRandom().nextDouble() < acceptChance;
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        super.stepStarted(stepScope);
        // TimeGradient only refreshes at the beginning of a step, so this code is in stepStarted instead of stepEnded
        var timeGradient = stepScope.getTimeGradient();
        var reverseTimeGradient = 1.0 - timeGradient;
        temperatureLevels = new double[levelsLength];
        for (var i = 0; i < levelsLength; i++) {
            temperatureLevels[i] = startingTemperatureLevels[i] * reverseTimeGradient;
            if (temperatureLevels[i] < temperatureMinimum) {
                temperatureLevels[i] = temperatureMinimum;
            }
        }
        // TODO implement reheating
    }

}
