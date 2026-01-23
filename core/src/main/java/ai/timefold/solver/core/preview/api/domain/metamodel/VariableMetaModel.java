package ai.timefold.solver.core.preview.api.domain.metamodel;

import org.jspecify.annotations.NullMarked;

/**
 * Describes a variable in the domain model.
 * See extending interfaces for more specific types of variables.
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
 * 
 * @param <Solution_>
 * @param <Entity_>
 * @param <Value_>
 */
@NullMarked
public sealed interface VariableMetaModel<Solution_, Entity_, Value_>
        permits GenuineVariableMetaModel, ShadowVariableMetaModel {

    /**
     * Describes the entity that owns this variable.
     *
     * @return never null
     */
    PlanningEntityMetaModel<Solution_, Entity_> entity();

    /**
     * Describes the type of the value that this variable can hold.
     *
     * @return never null
     */
    Class<Value_> type();

    /**
     * Describes the name of this variable, which is typically a field name in the entity.
     *
     * @return never null
     */
    String name();

    /**
     * Returns true if this variable is a genuine (i.e. not shadow) variable.
     * Effectively checks if this variable is an instance of {@link GenuineVariableMetaModel}.
     *
     * @return true if this variable is genuine
     */
    default boolean isGenuine() {
        return this instanceof GenuineVariableMetaModel;
    }

}
