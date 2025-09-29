package ai.timefold.solver.core.preview.api.domain.metamodel;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;

import org.jspecify.annotations.NullMarked;

/**
 * Identifies that a given value was not found in any {@link PlanningEntity}'s list variables.
 * Singleton instance can be accessed by {@link ElementPosition#unassigned()}.
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
 * <a href="https://github.com/TimefoldAI/timefold-solver/discussions">Timefold Solver Github</a>
 * or to <a href="https://discord.com/channels/1413420192213631086/1414521616955605003">Timefold Discord</a>.
 */
@NullMarked
public sealed interface UnassignedElement
        extends ElementPosition
        permits DefaultUnassignedElement {

}
