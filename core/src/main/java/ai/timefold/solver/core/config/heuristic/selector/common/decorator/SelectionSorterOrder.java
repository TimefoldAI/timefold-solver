package ai.timefold.solver.core.config.heuristic.selector.common.decorator;

import jakarta.xml.bind.annotation.XmlEnum;

import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * @see SelectionSorter
 */

@XmlEnum
public enum SelectionSorterOrder {
    /**
     * For example: 0, 1, 2, 3.
     */
    ASCENDING,
    /**
     * For example: 3, 2, 1, 0.
     */
    DESCENDING;

    public static @NonNull SelectionSorterOrder resolve(@Nullable SelectionSorterOrder sorterOrder) {
        if (sorterOrder == null) {
            return ASCENDING;
        }
        return sorterOrder;
    }

}
