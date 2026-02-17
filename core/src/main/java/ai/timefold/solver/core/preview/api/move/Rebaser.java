package ai.timefold.solver.core.preview.api.move;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.api.solver.change.ProblemChange;

import org.jspecify.annotations.Nullable;

/**
 * Allows to transfer an entity or fact instance (often from another {@link Thread})
 * to another {@link ScoreDirector}'s internal working instance.
 * <p>
 * <strong>This package and all of its contents are part of the Neighborhoods API,
 * which is under development and is only offered as a preview feature.</strong>
 * There are no guarantees for backward compatibility;
 * any class, method, or field may change or be removed without prior notice,
 * although we will strive to avoid this as much as possible.
 * <p>
 * We encourage you to try the API and give us feedback on your experience with it,
 * before we finalize the API.
 * Please direct your feedback to
 * <a href="https://github.com/TimefoldAI/timefold-solver/discussions">Timefold Solver GitHub</a>
 * or to <a href="https://discord.com/channels/1413420192213631086/1414521616955605003">Timefold Discord</a>.
 */
public interface Rebaser {

    /**
     * Translates an entity or fact instance (often from another {@link Thread})
     * to another {@link ScoreDirector}'s internal working instance.
     * Useful for move rebasing and in a {@link ProblemChange} and for multi-threaded solving.
     * <p>
     * Matching uses {@link PlanningId}.
     *
     * @param problemFactOrPlanningEntity The fact or entity to rebase.
     * @return null if problemFactOrPlanningEntity is null
     * @throws IllegalArgumentException if there is no working object for the fact or entity,
     *         if it cannot be looked up,
     *         or if its class is not supported.
     * @throws IllegalStateException if it cannot be looked up
     * @param <T>
     */
    <T> @Nullable T rebase(@Nullable T problemFactOrPlanningEntity);

}
