package ai.timefold.solver.core.impl.solver.random;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Chooses a random item from a fixed list of items.
 * Only a live item can be chosen.
 * Each implementation gets a predicate at construction that tells if an item is live,
 * but it tests that predicate only when stale, not at each choice.
 * Shared by move selectors and neighborhoods, so that both select the same way.
 *
 * @param <Item_> the type of the items
 */
@NullMarked
public sealed interface RandomChooser<Item_> permits UniformRandomChooser, BiasedRandomChooser {

    /**
     * @return true if no item can be chosen
     */
    boolean isEmpty();

    /**
     * @return null if no item can be chosen
     */
    @Nullable
    Item_ choose();

    /**
     * Makes the item that {@link #choose()} returned last not live.
     * The predicate must return false for that item from then on, else the next refresh makes it live again.
     */
    void removeChosen();

    /**
     * Before the next choice, the chooser tests the predicate again for each item.
     * Call this when the predicate can give another result for an item.
     */
    void markStale();

}
