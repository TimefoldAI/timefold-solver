package ai.timefold.solver.core.api.score.analysis;

import java.util.Collection;

import org.jspecify.annotations.NullMarked;

/**
 * Represents a breakdown of the structural flaws of a solution.
 */
@NullMarked
public interface StructuralFlawAnalysis {
    /**
     * Return a collection of {@link ai.timefold.solver.core.api.domain.entity.PlanningEntity}
     * that have inconsistent shadow variables.
     */
    Collection<Object> getInconsistentEntities();
}
