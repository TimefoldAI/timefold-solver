package ai.timefold.solver.core.config.heuristic.selector.value;

import jakarta.xml.bind.annotation.XmlEnum;

import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

/**
 * The manner of sorting a values for a {@link PlanningVariable}.
 */

@XmlEnum
public enum ValueSorterManner {
    NONE,
    INCREASING_STRENGTH,
    INCREASING_STRENGTH_IF_AVAILABLE,
    DECREASING_STRENGTH,
    DECREASING_STRENGTH_IF_AVAILABLE;
}
