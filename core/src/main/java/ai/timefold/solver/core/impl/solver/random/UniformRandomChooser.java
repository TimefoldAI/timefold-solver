package ai.timefold.solver.core.impl.solver.random;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Chooses a random live item; every live item has the same chance.
 * With one live item, it uses no random number.
 * <p>
 * All items start live.
 * After {@link #markStale()}, the live items are found again with the livePredicate, in the order of the item list.
 * Removing a live item moves the last live item into its place.
 * Shared by move selectors and neighborhoods, so that both select the same way.
 *
 * @param <Item_> the type of the items
 */
@NullMarked
public final class UniformRandomChooser<Item_> {

    private final List<Item_> itemList;
    private final Predicate<Item_> livePredicate;
    private final List<Item_> liveItemList;
    private boolean stale = false;
    private int chosenIndex = -1;

    /**
     * @param itemList all items, all live at the start; the chooser keeps it, so do not change it
     * @param livePredicate true if the item can be chosen; evaluated only after {@link #markStale()}
     */
    public UniformRandomChooser(List<Item_> itemList, Predicate<Item_> livePredicate) {
        this.itemList = itemList;
        this.livePredicate = livePredicate;
        this.liveItemList = new ArrayList<>(itemList);
    }

    public void markStale() {
        stale = true;
    }

    public boolean isEmpty() {
        refresh();
        return liveItemList.isEmpty();
    }

    /**
     * @return null if no item is live
     */
    public @Nullable Item_ choose(RandomGenerator random) {
        refresh();
        var size = liveItemList.size();
        chosenIndex = switch (size) {
            case 0 -> -1;
            case 1 -> 0;
            default -> random.nextInt(size);
        };
        return chosenIndex < 0 ? null : liveItemList.get(chosenIndex);
    }

    /**
     * Makes the item that {@link #choose(RandomGenerator)} returned last not live.
     */
    public void removeChosen() {
        if (chosenIndex < 0) {
            throw new IllegalStateException("Impossible state: no chosen item to remove.");
        }
        removeAt(chosenIndex);
    }

    /**
     * Makes the item not live.
     * While stale, it has no effect; the livePredicate must then exclude the item.
     */
    public void remove(Item_ item) {
        if (stale) {
            return;
        }
        for (var i = liveItemList.size() - 1; i >= 0; i--) {
            if (liveItemList.get(i) == item) {
                removeAt(i);
                return;
            }
        }
    }

    private void removeAt(int index) {
        var lastItem = liveItemList.removeLast();
        if (index != liveItemList.size()) {
            // The list order carries no meaning, so the last item fills the gap.
            liveItemList.set(index, lastItem);
        }
        chosenIndex = -1;
    }

    private void refresh() {
        if (!stale) {
            return;
        }
        liveItemList.clear();
        for (var item : itemList) {
            if (livePredicate.test(item)) {
                liveItemList.add(item);
            }
        }
        stale = false;
        chosenIndex = -1;
    }

}
