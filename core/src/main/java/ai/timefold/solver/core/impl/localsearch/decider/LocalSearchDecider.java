package ai.timefold.solver.core.impl.localsearch.decider;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.Acceptor;
import ai.timefold.solver.core.impl.localsearch.decider.forager.LocalSearchForager;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.phase.scope.SolverLifecyclePoint;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.Termination;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class LocalSearchDecider<Solution_> {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    protected final String logIndentation;
    protected final Termination<Solution_> termination;
    protected final MoveSelector<Solution_> moveSelector;
    protected final Acceptor<Solution_> acceptor;
    protected final LocalSearchForager<Solution_> forager;

    protected boolean assertMoveScoreFromScratch = false;
    protected boolean assertExpectedUndoMoveScore = false;

    public LocalSearchDecider(String logIndentation, Termination<Solution_> termination,
            MoveSelector<Solution_> moveSelector, Acceptor<Solution_> acceptor, LocalSearchForager<Solution_> forager) {
        this.logIndentation = logIndentation;
        this.termination = termination;
        this.moveSelector = moveSelector;
        this.acceptor = acceptor;
        this.forager = forager;
    }

    public Termination<Solution_> getTermination() {
        return termination;
    }

    public MoveSelector<Solution_> getMoveSelector() {
        return moveSelector;
    }

    public Acceptor<Solution_> getAcceptor() {
        return acceptor;
    }

    public LocalSearchForager<Solution_> getForager() {
        return forager;
    }

    public void setAssertMoveScoreFromScratch(boolean assertMoveScoreFromScratch) {
        this.assertMoveScoreFromScratch = assertMoveScoreFromScratch;
    }

    public void setAssertExpectedUndoMoveScore(boolean assertExpectedUndoMoveScore) {
        this.assertExpectedUndoMoveScore = assertExpectedUndoMoveScore;
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public void solvingStarted(SolverScope<Solution_> solverScope) {
        moveSelector.solvingStarted(solverScope);
        acceptor.solvingStarted(solverScope);
        forager.solvingStarted(solverScope);
    }

    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        moveSelector.phaseStarted(phaseScope);
        acceptor.phaseStarted(phaseScope);
        forager.phaseStarted(phaseScope);
    }

    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        moveSelector.stepStarted(stepScope);
        acceptor.stepStarted(stepScope);
        forager.stepStarted(stepScope);
    }

    public void decideNextStep(LocalSearchStepScope<Solution_> stepScope) {
        InnerScoreDirector<Solution_, ?> scoreDirector = stepScope.getScoreDirector();
        scoreDirector.setAllChangesWillBeUndoneBeforeStepEnds(true);
        int moveIndex = 0;
        for (Move<Solution_> move : moveSelector) {
            LocalSearchMoveScope<Solution_> moveScope = new LocalSearchMoveScope<>(stepScope, moveIndex, move);
            moveIndex++;
            doMove(moveScope);
            if (forager.isQuitEarly()) {
                break;
            }
            stepScope.getPhaseScope().getSolverScope().checkYielding();
            if (termination.isPhaseTerminated(stepScope.getPhaseScope())) {
                break;
            }
        }
        scoreDirector.setAllChangesWillBeUndoneBeforeStepEnds(false);
        pickMove(stepScope);
    }

    protected <Score_ extends Score<Score_>> void doMove(LocalSearchMoveScope<Solution_> moveScope) {
        InnerScoreDirector<Solution_, Score_> scoreDirector = moveScope.getScoreDirector();
        if (!moveScope.getMove().isMoveDoable(scoreDirector)) {
            throw new IllegalStateException("Impossible state: Local search move selector (" + moveSelector
                    + ") provided a non-doable move (" + moveScope.getMove() + ").");
        }
        scoreDirector.doAndProcessMove(moveScope.getMove(), assertMoveScoreFromScratch, score -> {
            moveScope.setScore(score);
            boolean accepted = acceptor.isAccepted(moveScope);
            moveScope.setAccepted(accepted);
            forager.addMove(moveScope);
        });
        if (assertExpectedUndoMoveScore) {
            scoreDirector.assertExpectedUndoMoveScore(moveScope.getMove(),
                    (Score_) moveScope.getStepScope().getPhaseScope().getLastCompletedStepScope().getScore(),
                    SolverLifecyclePoint.of(moveScope));
        }
        logger.trace("{}        Move index ({}), score ({}), accepted ({}), move ({}).",
                logIndentation, moveScope.getMoveIndex(), moveScope.getScore(), moveScope.getAccepted(), moveScope.getMove());
    }

    protected void pickMove(LocalSearchStepScope<Solution_> stepScope) {
        LocalSearchMoveScope<Solution_> pickedMoveScope = forager.pickMove(stepScope);
        if (pickedMoveScope != null) {
            Move<Solution_> step = pickedMoveScope.getMove();
            stepScope.setStep(step);
            if (logger.isDebugEnabled()) {
                stepScope.setStepString(step.toString());
            }
            stepScope.setScore(pickedMoveScope.getScore());
        }
    }

    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        moveSelector.stepEnded(stepScope);
        acceptor.stepEnded(stepScope);
        forager.stepEnded(stepScope);
    }

    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        moveSelector.phaseEnded(phaseScope);
        acceptor.phaseEnded(phaseScope);
        forager.phaseEnded(phaseScope);
    }

    public void solvingEnded(SolverScope<Solution_> solverScope) {
        moveSelector.solvingEnded(solverScope);
        acceptor.solvingEnded(solverScope);
        forager.solvingEnded(solverScope);
    }

    public void solvingError(SolverScope<Solution_> solverScope, Exception exception) {
        // Overridable by a subclass.
    }
}
