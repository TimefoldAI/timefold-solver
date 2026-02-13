package ai.timefold.solver.core.config.heuristic.selector.entity;

import jakarta.xml.bind.annotation.XmlEnum;

import ai.timefold.solver.core.api.domain.entity.PlanningEntity;

/**
 * The manner of sorting {@link PlanningEntity} instances.
 */

@XmlEnum
public enum EntitySorterManner {
    NONE,
    DESCENDING,
    DESCENDING_IF_AVAILABLE
}
