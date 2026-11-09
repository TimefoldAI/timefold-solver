package ai.timefold.solver.core.impl.solver.random;

import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Chooses a random live item; the chance of each item is proportional to its weight.
 * Items with weight 0 are never chosen.
 * The live items are found again only after {@link #markStale()}.
 * Shared by move selectors and neighborhoods, so that both select the same way.
 *
 * @param <Item_> the type of the items
 */
@NullMarked
public final class BiasedRandomChooser<Item_> {

    private final List<Item_> itemList;
    private final double[] weightArray;
    private final Predicate<Item_> livePredicate;
    private final NavigableMap<Double, Item_> offsetToItemMap = new TreeMap<>();
    private double weightTotal = 0.0;
    private boolean stale = true;

    /**
     * @param itemList all items, in the order that maps the random offset to an item;
     *        the chooser keeps it, so do not change it
     * @param weightArray the weight of each item, at the same index as in itemList; the chooser keeps it too
     * @param livePredicate true if the item can be chosen; evaluated only when stale
     */
    public BiasedRandomChooser(List<Item_> itemList, double[] weightArray, Predicate<Item_> livePredicate) {
        if (itemList.size() != weightArray.length) {
            throw new IllegalArgumentException("The itemList size (%d) differs from the weightArray length (%d)."
                    .formatted(itemList.size(), weightArray.length));
        }
        this.itemList = itemList;
        this.weightArray = weightArray;
        this.livePredicate = livePredicate;
    }

    public void markStale() {
        stale = true;
    }

    public boolean isEmpty() {
        refresh();
        return offsetToItemMap.isEmpty();
    }

    /**
     * @return null if no item is live and has a non-zero weight
     */
    public @Nullable Item_ choose(RandomGenerator random) {
        if (isEmpty()) {
            return null;
        }
        var randomOffset = RandomUtils.nextDouble(random, weightTotal);
        // Never null, because randomOffset < weightTotal.
        return offsetToItemMap.floorEntry(randomOffset).getValue();
    }

    private void refresh() {
        if (!stale) {
            return;
        }
        offsetToItemMap.clear();
        var offset = 0.0;
        for (var i = 0; i < itemList.size(); i++) {
            var item = itemList.get(i);
            var weight = weightArray[i];
            if (weight != 0.0 && livePredicate.test(item)) {
                offsetToItemMap.put(offset, item);
                offset += weight;
            }
        }
        weightTotal = offset;
        stale = false;
    }

}
