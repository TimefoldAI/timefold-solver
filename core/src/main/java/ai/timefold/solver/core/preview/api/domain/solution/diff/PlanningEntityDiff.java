package ai.timefold.solver.core.preview.api.domain.solution.diff;

import java.util.Collection;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningEntityMetaModel;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A diff between two instances of a {@link PlanningEntity},
 * where at least one variable of that entity (genuine or shadow) changed.
 * Obtain from {@link PlanningSolutionDiff}.
 * <p>
 * This interface is not intended to be implemented by users.
 * The default implementation has a default {@code toString()} method that prints a summary of the differences.
 * Do not attempt to parse that string - it is subject to change in the future.
 *
 * @param <Solution_>
 * @param <Entity_>
 */
@NullMarked
public interface PlanningEntityDiff<Solution_, Entity_> {

    /**
     * The diff between the two solutions that this is part of.
     */
    PlanningSolutionDiff<Solution_> solutionDiff();

    /**
     * The entity that this diff is of.
     */
    Entity_ entity();

    /**
     * Describes the {@link PlanningEntity} class.
     */
    PlanningEntityMetaModel<Solution_, Entity_> entityMetaModel();

    /**
     * Returns a single diff for the entity's variables.
     * It is a convenience method for {@link #variableDiff(String)} -
     * if the entity provided more than one diff, which may happen for multi-variate entities, this method will throw.
     *
     * @throws IllegalStateException if {@link #variableDiffs()} returns more than one diff.
     */
    <Value_> PlanningVariableDiff<Solution_, Entity_, Value_> variableDiff();

    /**
     * Returns the diff for the variable with the given name, or null if the variable is not present in the diff.
     * 
     * @param variableName Name of the variable to check for.
     * @return Null if the entity does not declare a variable of that name.
     * @param <Value_> Expected type of the variable.
     */
    @Nullable
    <Value_> PlanningVariableDiff<Solution_, Entity_, Value_> variableDiff(String variableName);

    /**
     * Returns the diffs of all variables of a single changed entity.
     */
    Collection<PlanningVariableDiff<Solution_, Entity_, ?>> variableDiffs();

}
