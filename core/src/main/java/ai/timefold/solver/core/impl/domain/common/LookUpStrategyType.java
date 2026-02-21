package ai.timefold.solver.core.impl.domain.common;

import ai.timefold.solver.core.api.domain.common.PlanningId;
import ai.timefold.solver.core.api.domain.entity.PlanningEntity;
import ai.timefold.solver.core.api.domain.solution.ProblemFactCollectionProperty;
import ai.timefold.solver.core.impl.score.director.ScoreDirector;

/**
 * Determines how {@link ScoreDirector#lookUpWorkingObject(Object)} maps
 * a {@link ProblemFactCollectionProperty problem fact} or a {@link PlanningEntity planning entity}
 * from an external copy to the internal one.
 */
enum LookUpStrategyType {

    /**
     * Map by the same {@link PlanningId} field or method.
     * If there is no such field or method,
     * there is no mapping and {@link ScoreDirector#lookUpWorkingObject(Object)} must not be used.
     * If there is such a field or method, but it returns null, it fails fast.
     * <p>
     * This is the default.
     */
    PLANNING_ID_OR_NONE,
    /**
     * Map by the same {@link PlanningId} field or method.
     * If there is no such field or method, it fails fast.
     */
    PLANNING_ID_OR_FAIL_FAST,
    /**
     * Only used in testing; do not use in production code.
     */
    NONE;

}
