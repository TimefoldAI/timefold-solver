package ai.timefold.solver.core.impl.localsearch.decider.acceptor.reconfiguration;

import java.time.Clock;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeometricUnimprovedSolutionReconfigurationStrategy<Solution_> implements ReconfigurationStrategy<Solution_> {
    private static final double GEOMETRIC_FACTOR = 1.3;
    private static final double SCALING_FACTOR = 1.0;

    private final Logger logger = LoggerFactory.getLogger(GeometricUnimprovedSolutionReconfigurationStrategy.class);
    private final Clock clock = Clock.systemUTC();

    private long lastImprovementMillis;
    private Score<?> currentBestScore;
    private boolean reconfigurationTriggered;
    private double geometricGrowFactor;
    private long nextReconfiguration;

    @Override
    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        currentBestScore = phaseScope.getBestScore();
        geometricGrowFactor = 1;
        nextReconfiguration = (long) (1_000 * SCALING_FACTOR);
        reconfigurationTriggered = false;
        lastImprovementMillis = clock.millis();
    }

    @Override
    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        // Do nothing
    }

    @Override
    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        this.currentBestScore = stepScope.getPhaseScope().getBestScore();
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        if (((Score) stepScope.getScore()).compareTo(currentBestScore) > 0) {
            lastImprovementMillis = clock.millis();
            this.currentBestScore = stepScope.getScore();
        }
    }

    @Override
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    @Override
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        // Do nothing
    }

    @Override
    public boolean needReconfiguration(LocalSearchMoveScope<Solution_> moveScope) {
        if (!reconfigurationTriggered && lastImprovementMillis > 0
                && clock.millis() - lastImprovementMillis >= nextReconfiguration) {
            logger.debug("Reconfiguration triggered with geometric factor {} and scaling factor of {}", geometricGrowFactor,
                    SCALING_FACTOR);
            nextReconfiguration = (long) Math.ceil(SCALING_FACTOR * geometricGrowFactor * 1_000);
            geometricGrowFactor = Math.ceil(geometricGrowFactor * GEOMETRIC_FACTOR);
            lastImprovementMillis = clock.millis();
            reconfigurationTriggered = true;
        }
        return reconfigurationTriggered;
    }

    @Override
    public void reset() {
        reconfigurationTriggered = false;
        lastImprovementMillis = clock.millis();
    }
}
