package ai.timefold.solver.core.impl.solver.random;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Chooses a random item from a fixed list of items.
 * Only a live item can be chosen.
 * Which items are live at the start depends on the implementation; see there.
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
     * Makes the item that {@link #choose()} returned last not live, until {@link #reset()}.
     */
    void removeChosen();

    /**
     * Makes the removed items live again.
     */
    void reset();

}
