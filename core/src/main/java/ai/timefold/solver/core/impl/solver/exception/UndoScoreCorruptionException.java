package ai.timefold.solver.core.impl.solver.exception;

/**
 * An exception that is thrown in {@link ai.timefold.solver.core.config.solver.EnvironmentMode#TRACKED_FULL_ASSERT} when
 * undo score corruption is detected. It contains the working solution before the move, after the move, and after the undo move,
 * as well as the move that caused the corruption. You can catch this exception to create a reproducer of the corruption.
 * The API for this exception is currently unstable.
 */
public class UndoScoreCorruptionException extends IllegalStateException {
    private final Object beforeMoveSolution;
    private final Object afterMoveSolution;
    private final Object afterUndoSolution;

    public UndoScoreCorruptionException(String message, Object beforeMoveSolution, Object afterMoveSolution,
            Object afterUndoSolution) {
        super(message);
        this.beforeMoveSolution = beforeMoveSolution;
        this.afterMoveSolution = afterMoveSolution;
        this.afterUndoSolution = afterUndoSolution;
    }

    /**
     * Return the state of the working solution before a move was executed.
     *
     * @return the state of the working solution before a move was executed.
     */
    @SuppressWarnings("unchecked")
    public <Solution_> Solution_ getBeforeMoveSolution() {
        return (Solution_) beforeMoveSolution;
    }

    /**
     * Return the state of the working solution after a move was executed, but prior to the undo move.
     *
     * @return the state of the working solution after a move was executed, but prior to the undo move.
     */
    @SuppressWarnings("unchecked")
    public <Solution_> Solution_ getAfterMoveSolution() {
        return (Solution_) afterMoveSolution;
    }

    /**
     * Return the state of the working solution after the undo move was executed.
     *
     * @return the state of the working solution after the undo move was executed.
     */
    @SuppressWarnings("unchecked")
    public <Solution_> Solution_ getAfterUndoSolution() {
        return (Solution_) afterUndoSolution;
    }
}
