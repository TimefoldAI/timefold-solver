package ai.timefold.solver.core.impl.neighborhood.selection;

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
 * After {@link #markStale()}, the live items are found again, in the order of the item list.
 * {@link #remove(Object)} of a live item moves the last live item into its place.
 * <p>
 * Copy of the selection logic of {@code UniformRandomUnionMoveIterator}; keep both in sync.
 *
 * @param <Item_> the type of the items
 */
@NullMarked
public final class UniformRandomChooser<Item_> {

    private final List<Item_> itemList;
    private final Predicate<Item_> livePredicate;
    private final List<Item_> liveItemList;
    private boolean stale = true;

    /**
     * @param itemList all items
     * @param livePredicate true if the item can be chosen; evaluated only when stale
     */
    public UniformRandomChooser(List<Item_> itemList, Predicate<Item_> livePredicate) {
        this.itemList = new ArrayList<>(itemList);
        this.livePredicate = livePredicate;
        this.liveItemList = new ArrayList<>(itemList.size());
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
        return switch (size) {
            case 0 -> null;
            case 1 -> liveItemList.getFirst();
            default -> liveItemList.get(random.nextInt(size));
        };
    }

    /**
     * Makes the item not live.
     * While stale, it has no effect; the livePredicate must then exclude the item.
     */
    public void remove(Item_ item) {
        if (stale) {
            return;
        }
        var lastIndex = liveItemList.size() - 1;
        for (var i = lastIndex; i >= 0; i--) {
            if (liveItemList.get(i) == item) {
                var lastItem = liveItemList.removeLast();
                if (i != lastIndex) {
                    // The list order carries no meaning, so the last item fills the gap.
                    liveItemList.set(i, lastItem);
                }
                return;
            }
        }
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
    }

}
