package ai.timefold.solver.core.config.heuristic.selector.value;

import jakarta.xml.bind.annotation.XmlEnum;

import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

/**
 * The manner of sorting values for a {@link PlanningVariable}.
 */

@XmlEnum
public enum ValueSorterManner {
    NONE(true),
    INCREASING_STRENGTH(false),
    INCREASING_STRENGTH_IF_AVAILABLE(true),
    DECREASING_STRENGTH(false),
    DECREASING_STRENGTH_IF_AVAILABLE(true);

    private final boolean nonePossible;

    ValueSorterManner(boolean nonePossible) {
        this.nonePossible = nonePossible;
    }

    /**
     * @return true if {@link #NONE} is an option, such as when the other option is not available.
     */
    public boolean isNonePossible() {
        return nonePossible;
    }

}
