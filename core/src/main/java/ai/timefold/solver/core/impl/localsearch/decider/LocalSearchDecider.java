package ai.timefold.solver.core.impl.localsearch.decider;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.heuristic.move.LegacyMoveAdapter;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.Acceptor;
import ai.timefold.solver.core.impl.localsearch.decider.forager.LocalSearchForager;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchMoveScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchPhaseScope;
import ai.timefold.solver.core.impl.localsearch.scope.LocalSearchStepScope;
import ai.timefold.solver.core.impl.move.MoveRepository;
import ai.timefold.solver.core.impl.phase.scope.SolverLifecyclePoint;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.solver.termination.Termination;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @param <Solution_> the solution type, the class with the {@link PlanningSolution} annotation
 */
public class LocalSearchDecider<Solution_> {

    protected final transient Logger logger = LoggerFactory.getLogger(getClass());

    protected final String logIndentation;
    protected final PhaseTermination<Solution_> termination;
    protected final MoveRepository<Solution_> moveRepository;
    protected final Acceptor<Solution_> acceptor;
    protected final LocalSearchForager<Solution_> forager;

    protected boolean assertMoveScoreFromScratch = false;
    protected boolean assertExpectedUndoMoveScore = false;

    public LocalSearchDecider(String logIndentation, PhaseTermination<Solution_> termination,
            MoveRepository<Solution_> moveRepository, Acceptor<Solution_> acceptor, LocalSearchForager<Solution_> forager) {
        this.logIndentation = logIndentation;
        this.termination = termination;
        this.moveRepository = moveRepository;
        this.acceptor = acceptor;
        this.forager = forager;
    }

    public Termination<Solution_> getTermination() {
        return termination;
    }

    public MoveRepository<Solution_> getMoveRepository() {
        return moveRepository;
    }

    public Acceptor<Solution_> getAcceptor() {
        return acceptor;
    }

    public LocalSearchForager<Solution_> getForager() {
        return forager;
    }

    public void enableAssertions(EnvironmentMode environmentMode) {
        assertMoveScoreFromScratch = environmentMode.isFullyAsserted();
        assertExpectedUndoMoveScore = environmentMode.isIntrusivelyAsserted();
    }

    // ************************************************************************
    // Worker methods
    // ************************************************************************

    public void solvingStarted(SolverScope<Solution_> solverScope) {
        moveRepository.solvingStarted(solverScope);
        acceptor.solvingStarted(solverScope);
        forager.solvingStarted(solverScope);
    }

    public void phaseStarted(LocalSearchPhaseScope<Solution_> phaseScope) {
        moveRepository.phaseStarted(phaseScope);
        acceptor.phaseStarted(phaseScope);
        forager.phaseStarted(phaseScope);
    }

    public void stepStarted(LocalSearchStepScope<Solution_> stepScope) {
        moveRepository.stepStarted(stepScope);
        acceptor.stepStarted(stepScope);
        forager.stepStarted(stepScope);
    }

    public void decideNextStep(LocalSearchStepScope<Solution_> stepScope) {
        var scoreDirector = stepScope.getScoreDirector();
        scoreDirector.setAllChangesWillBeUndoneBeforeStepEnds(true);
        var moveIndex = 0;
        for (var move : moveRepository) {
            var moveScope = new LocalSearchMoveScope<>(stepScope, moveIndex, move);
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
        var scoreDirector = moveScope.<Score_> getScoreDirector();
        var moveDirector = moveScope.getStepScope().<Score_> getMoveDirector();
        var move = moveScope.getMove();
        if (!LegacyMoveAdapter.isDoable(moveDirector, move)) {
            throw new IllegalStateException("Impossible state: Local search move selector (" + moveRepository
                    + ") provided a non-doable move (" + moveScope.getMove() + ").");
        }
        var score = scoreDirector.executeTemporaryMove(moveScope.getMove(), assertMoveScoreFromScratch);
        moveScope.setScore(score);
        moveScope.setAccepted(acceptor.isAccepted(moveScope));
        forager.addMove(moveScope);
        if (assertExpectedUndoMoveScore) {
            scoreDirector.assertExpectedUndoMoveScore(moveScope.getMove(),
                    moveScope.getStepScope().getPhaseScope().getLastCompletedStepScope().getScore(),
                    SolverLifecyclePoint.of(moveScope));
        }
        logger.trace("{}        Move index ({}), score ({}), accepted ({}), move ({}).",
                logIndentation, moveScope.getMoveIndex(), moveScope.getScore().raw(), moveScope.getAccepted(),
                moveScope.getMove());
    }

    protected void pickMove(LocalSearchStepScope<Solution_> stepScope) {
        var pickedMoveScope = forager.pickMove(stepScope);
        if (pickedMoveScope != null) {
            var step = pickedMoveScope.getMove();
            stepScope.setStep(step);
            if (logger.isDebugEnabled()) {
                stepScope.setStepString(step.toString());
            }
            stepScope.setScore(pickedMoveScope.getScore());
        }
    }

    public void stepEnded(LocalSearchStepScope<Solution_> stepScope) {
        moveRepository.stepEnded(stepScope);
        acceptor.stepEnded(stepScope);
        forager.stepEnded(stepScope);
    }

    public void phaseEnded(LocalSearchPhaseScope<Solution_> phaseScope) {
        moveRepository.phaseEnded(phaseScope);
        acceptor.phaseEnded(phaseScope);
        forager.phaseEnded(phaseScope);
    }

    public void solvingEnded(SolverScope<Solution_> solverScope) {
        moveRepository.solvingEnded(solverScope);
        acceptor.solvingEnded(solverScope);
        forager.solvingEnded(solverScope);
    }

    public void solvingError(SolverScope<Solution_> solverScope, Exception exception) {
        // Overridable by a subclass.
    }
}
