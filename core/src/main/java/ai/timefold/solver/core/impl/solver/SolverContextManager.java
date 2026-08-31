package ai.timefold.solver.core.impl.solver;

import java.util.List;
import java.util.Objects;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.phase.Phase;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactory;
import ai.timefold.solver.core.impl.score.director.ScoreDirectorFactoryFactory;
import ai.timefold.solver.core.impl.solver.change.DefaultProblemChangeDirector;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Owns the {@link InnerScoreDirector} the solver is currently working with,
 * and swaps it whenever a {@link Phase} requires an {@link EnvironmentMode} other than the one in use.
 * <p>
 * A score director is built for exactly one environment mode,
 * because the mode decides which assertions it runs and how much bookkeeping it keeps.
 * The solver config has a global mode and each phase may override it with a stricter one,
 * so a solve may need more than one score director.
 * Rather than every caller reasoning about that, all of it lives here:
 * {@link #phaseStarted(AbstractPhaseScope)} compares the phase's mode to the current one,
 * and only when they differ does it build a replacement, hand the working solution over,
 * and close the one being replaced.
 * <p>
 * Score directors are not cached; a phase that switches back to a mode used earlier gets a fresh instance.
 * Caching happens one level down, in the {@link ScoreDirectorFactory},
 * which keeps at most one factory per environment mode — that is the expensive part to build,
 * whereas a score director on top of an existing factory is cheap.
 * A consequence is that the solver ends holding the *last* phase's score director,
 * not the one it started with.
 * <p>
 * Everything a swap has to keep continuous lives in {@link #loadContext}:
 * the working solution, the score calculation count, the {@link SolverScope}'s view of both directors,
 * and the assertion level of the {@link BestSolutionRecaller}.
 * <p>
 * <b>Ownership of closing.</b> Every score director a solve opens is closed here and nowhere else,
 * so that no caller has to work out which of them is still in use:
 * <ul>
 * <li>{@link #phaseStarted(AbstractPhaseScope)} closes the one it replaces,
 * and only after {@link #loadContext} has handed everything over —
 * closing clears the working solution,
 * so closing any earlier would gut the director the hand-over reads from;</li>
 * <li>{@link #solvingEnded(SolverScope)} closes the score director used in the last phase; if real-time problem changes
 * restart solving, {@link #prepareForProblemChanges(SolverScope)} recreates a score director before applying them;</li>
 * <li>{@link #solvingError(SolverScope, Exception)} closes it when solving fails,
 * because {@code outerSolvingEnded} never runs on that path.</li>
 * </ul>
 * Note that no score director is closed at {@code phaseEnded}.
 * The phase which follows reads the outgoing director's working solution during the hand-over,
 * and after the last phase the solver still needs it for the best solution,
 * and for the problem changes which may restart solving.
 * <p>
 * Not thread-safe, and does not need to be: one instance belongs to one solver,
 * and every life-cycle method is called on that solver's own thread.
 *
 * @param <Solution_> the solution type
 * @param <Score_> the score type to go with the solution
 */
@NullMarked
public class SolverContextManager<Solution_, Score_ extends Score<Score_>> implements AutoCloseable {

    private final ScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory;
    private final BestSolutionRecaller<Solution_> bestSolutionRecaller;
    private final List<Phase<Solution_>> phaseList;

    @Nullable
    private SolverContext<Solution_, Score_> currentContext;

    public SolverContextManager(ScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory,
            BestSolutionRecaller<Solution_> bestSolutionRecaller, List<Phase<Solution_>> phaseList) {
        this.scoreDirectorFactory = scoreDirectorFactory;
        this.bestSolutionRecaller = bestSolutionRecaller;
        this.phaseList = phaseList;
    }

    // ************************************************************************
    // Life-cycle methods
    // ************************************************************************

    /**
     * Adopts the score director the solver was built with as the starting context.
     * Must run before any phase starts.
     */
    public void solvingStarted(SolverScope<Solution_> solverScope) {
        this.currentContext = SolverContext.of(solverScope);
    }

    /**
     * This closes the score director used in the last phase.
     * The score director mustn't be closed during the {@code phaseEnded} event,
     * as it's still needed internally by the solver until the next phase begins.
     * The {@link #phaseStarted(AbstractPhaseScope)} event is the appropriate place to close the score director
     * before the final phase.
     */
    public void solvingEnded(SolverScope<Solution_> solverScope) {
        close(solverScope);
    }

    /**
     * Swaps in a score director for the phase's {@link EnvironmentMode} if it differs from the one in use,
     * and closes the one being replaced.
     * Does nothing when the modes already match, which is the common case.
     * <p>
     * Runs before the phase's own listeners are notified,
     * so that anything binding to the score director at phase start — list variable selectors, for instance —
     * binds to the director the phase will actually run on.
     */
    public void phaseStarted(AbstractPhaseScope<Solution_> phaseScope) {
        var newSolverContext =
                contextFor(phaseScope.getSolverScope(), scoreDirectorFactory, phaseList.get(phaseScope.getPhaseIndex()),
                        Objects.requireNonNull(currentContext, "Impossible state: solvingStarted() has not run yet."));
        if (newSolverContext != currentContext) {
            loadContext(phaseScope.getSolverScope(), bestSolutionRecaller, currentContext, newSolverContext);
            currentContext.release();
            currentContext = newSolverContext;
        }
    }

    /**
     * Builds the score director the real-time problem changes are applied to,
     * and uses the environment from the first phase.
     * <p>
     * Called from {@code DefaultSolver.checkProblemChanges},
     * once the run which just ended has released the score director its phases used.
     * A problem change needs an open score director to be applied to,
     * and the restarted solver begins at the first phase,
     * so that phase's {@link EnvironmentMode} is the one to build for.
     */
    void prepareForProblemChanges(SolverScope<Solution_> solverScope) {
        var firstPhaseContext = contextFor(solverScope, scoreDirectorFactory, phaseList.getFirst(), null);
        loadContext(solverScope, bestSolutionRecaller, null, firstPhaseContext);
        currentContext = firstPhaseContext;
    }

    /**
     * Closes the score director in use,
     * as the solver's normal cleanup does not run on the failure path.
     * <p>
     * Solving can fail before {@link #solvingStarted(SolverScope)} has adopted a context — for instance in
     * {@code DefaultSolver.assertCorrectSolutionState()},
     * or in any listener notified earlier in solving start.
     * The score director the solver was built with still has to be closed in that case,
     * or a long-lived {@link SolverManager} would accumulate one per failed job.
     */
    public void solvingError(SolverScope<Solution_> solverScope, Exception exception) {
        try {
            close(solverScope);
        } catch (RuntimeException releaseException) {
            // The caller is on its way to rethrowing the real failure; this must not take its place.
            exception.addSuppressed(releaseException);
        }
    }

    /**
     * Closes the score director the solver ended on.
     * Called once the solver is done with it for good, which is after the problem change loop, not at
     * {@code solvingEnded}: a restart runs more phases on that same score director.
     * Idempotent, so a solve which already failed and closed through
     * {@link #solvingError(SolverScope, Exception)} is not closed twice.
     */
    @Override
    public void close() {
        close(null);
    }

    private void close(@Nullable SolverScope<Solution_> solverScope) {
        if (currentContext != null) {
            currentContext.release();
            currentContext = null;
        } else if (solverScope != null) {
            // Solving failed before solvingStarted() adopted a context, so the score director the solver was
            // built with is the only one open and the scope is the only place holding it.
            solverScope.getScoreDirector().close();
        }
    }

    // ************************************************************************
    // Utility methods
    // ************************************************************************

    /**
     * @return the given context when the phase can run on it, otherwise a new one for the phase's environment mode
     */
    private static <Solution_, Score_ extends Score<Score_>> SolverContext<Solution_, Score_> contextFor(
            SolverScope<Solution_> solverScope, ScoreDirectorFactory<Solution_, Score_> scoreDirectorFactory,
            Phase<Solution_> phase, @Nullable SolverContext<Solution_, Score_> context) {
        // The environment modes match, so the phase runs on the context already in use.
        if (context != null && phase.getEnvironmentMode() == context.environmentMode()) {
            return context;
        }
        // Either the modes differ, or there is no context in use at all,
        // so the phase needs a score director of its own.
        // Solver contexts are deliberately not cached; the score director factory caches per mode instead,
        // and building a score director on top of an existing factory is cheap.
        var newScoreDirector = scoreDirectorFactory.createScoreDirectorBuilder(phase.getEnvironmentMode())
                .withLookUpEnabled(true)
                .withConstraintMatchPolicy(
                        ScoreDirectorFactoryFactory.decideConstraintMatchPolicy(solverScope, phase.getEnvironmentMode()))
                .build();
        var newProblemChangeDirector = new DefaultProblemChangeDirector<>(newScoreDirector);
        return new SolverContext<>(phase.getEnvironmentMode(), newScoreDirector, newProblemChangeDirector);
    }

    /**
     * Handles everything a context swap must execute:
     * update the working solution,
     * update the running score calculation count,
     * and update the {@link SolverScope}'s view of both directors, and the recaller's assertion level.
     * Does not close the outgoing director because the caller does that once the hand-over is complete.
     */
    private static <Solution_, Score_ extends Score<Score_>> void loadContext(SolverScope<Solution_> solverScope,
            BestSolutionRecaller<Solution_> bestSolutionRecaller, @Nullable SolverContext<Solution_, Score_> oldSolverContext,
            SolverContext<Solution_, Score_> newSolverContext) {
        solverScope.setScoreDirector(newSolverContext.scoreDirector());
        solverScope.setProblemChangeDirector(newSolverContext.problemChangeDirector());
        if (oldSolverContext != null) {
            // We will use the same working solution set from the previous phase, as it has already been cloned
            newSolverContext.scoreDirector().setWorkingSolution(oldSolverContext.scoreDirector().getWorkingSolution());
        } else {
            // The old context is null, so we use the current best solution instead
            solverScope.setWorkingSolutionFromBestSolution();
        }
        bestSolutionRecaller.enableAssertions(newSolverContext.environmentMode());
        // Ensure that the score calculation count is consistent for the new director
        if (oldSolverContext != null) {
            newSolverContext.scoreDirector().resetCalculationCount();
            newSolverContext.scoreDirector().incrementCalculationCount(oldSolverContext.scoreDirector().getCalculationCount());
        }
    }

    /**
     * The score director the solver is working with, plus what is bound to it.
     * Immutable: a change of environment mode produces a new instance rather than mutating this one.
     */
    private record SolverContext<Solution_, Score_ extends Score<Score_>>(EnvironmentMode environmentMode,
            InnerScoreDirector<Solution_, Score_> scoreDirector,
            DefaultProblemChangeDirector<Solution_> problemChangeDirector) {

        public static <Solution_, Score_ extends Score<Score_>> SolverContext<Solution_, Score_>
                of(SolverScope<Solution_> solverScope) {
            return new SolverContext<>(solverScope.<Score_> getScoreDirector().getEnvironmentMode(),
                    solverScope.<Score_> getScoreDirector(), solverScope.getProblemChangeDirector());
        }

        void release() {
            scoreDirector.close();
        }
    }
}
