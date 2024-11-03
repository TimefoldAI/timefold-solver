package ai.timefold.solver.core.config.heuristic.selector.common;

import java.util.Objects;

import jakarta.xml.bind.annotation.XmlEnum;

import ai.timefold.solver.core.config.heuristic.selector.SelectorConfig;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Defines in which order the elements or a selector are selected.
 */

@XmlEnum
public enum SelectionOrder {
    /**
     * Inherit the value from the parent {@link SelectorConfig}. If the parent is cached,
     * the value is changed to {@link #ORIGINAL}.
     * <p>
     * This is the default. If there is no such parent, then it defaults to {@link #RANDOM}.
     */
    INHERIT,
    /**
     * Select the elements in original order.
     */
    ORIGINAL,
    /**
     * Select in sorted order by sorting the elements.
     * Each element will be selected exactly once (if all elements end up being selected).
     * Requires {@link SelectionCacheType#STEP} or higher.
     */
    SORTED,
    /**
     * Select in random order, without shuffling the elements.
     * Each element might be selected multiple times.
     * Scales well because it does not require caching.
     */
    RANDOM,
    /**
     * Select in random order by shuffling the elements when a selection iterator is created.
     * Each element will be selected exactly once (if all elements end up being selected).
     * Requires {@link SelectionCacheType#STEP} or higher.
     */
    SHUFFLED,
    /**
     * Select in random order, based on the selection probability of each element.
     * Elements with a higher probability have a higher chance to be selected than elements with a lower probability.
     * Each element might be selected multiple times.
     * Requires {@link SelectionCacheType#STEP} or higher.
     */
    PROBABILISTIC;

    public static @NonNull SelectionOrder resolve(@Nullable SelectionOrder selectionOrder,
            @NonNull SelectionOrder inheritedSelectionOrder) {
        if (selectionOrder == null || selectionOrder == INHERIT) {
            return Objects.requireNonNull(inheritedSelectionOrder, "The inheritedSelectionOrder cannot be null.");
        }
        return selectionOrder;
    }

    public static @NonNull SelectionOrder fromRandomSelectionBoolean(boolean randomSelection) {
        return randomSelection ? RANDOM : ORIGINAL;
    }

    public boolean toRandomSelectionBoolean() {
        if (this == RANDOM) {
            return true;
        } else if (this == ORIGINAL) {
            return false;
        } else {
            throw new IllegalStateException("The selectionOrder (" + this
                    + ") cannot be casted to a randomSelectionBoolean.");
        }
    }

}
