package ai.timefold.solver.core.config.heuristic.selector.value;

import jakarta.xml.bind.annotation.XmlEnum;

import ai.timefold.solver.core.api.domain.variable.PlanningVariable;

/**
 * The manner of sorting values for a {@link PlanningVariable}.
 */

@XmlEnum
public enum ValueSorterManner {
    NONE(true),
    /**
     * @deprecated use {@link #ASCENDING} instead
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    INCREASING_STRENGTH(false),
    /**
     * @deprecated use {@link #ASCENDING_IF_AVAILABLE} instead
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    INCREASING_STRENGTH_IF_AVAILABLE(true),
    /**
     * @deprecated use {@link #DESCENDING} instead
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    DECREASING_STRENGTH(false),
    /**
     * @deprecated use {@link #DESCENDING_IF_AVAILABLE} instead
     */
    @Deprecated(forRemoval = true, since = "1.28.0")
    DECREASING_STRENGTH_IF_AVAILABLE(true),
    ASCENDING(false),
    ASCENDING_IF_AVAILABLE(true),
    DESCENDING(false),
    DESCENDING_IF_AVAILABLE(true);

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
