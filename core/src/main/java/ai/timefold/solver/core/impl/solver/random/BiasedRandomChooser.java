package ai.timefold.solver.core.impl.solver.random;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Chooses a random live item; the chance of each item is proportional to its weight.
 * Items with weight 0 are never chosen.
 * The live items are found again only after {@link #markStale()}.
 *
 * @param <Item_> the type of the items
 */
@NullMarked
public final class BiasedRandomChooser<Item_> implements RandomChooser<Item_> {

    private final List<Item_> itemList;
    private final double[] weightArray;
    private final Predicate<Item_> livePredicate;
    private final RandomGenerator random;
    private final int[] liveIndexArray;
    // The sum of the weights of the live items up to and including each live item.
    private final double[] liveUpperBoundArray;
    private int liveCount = 0;
    private boolean stale = true;

    /**
     * @param itemList all items, in the order that maps the random offset to an item;
     *        the chooser keeps it, so do not change it
     * @param weightArray the weight of each item, at the same index as in itemList; the chooser keeps it too
     * @param livePredicate true if the item can be chosen; evaluated only when stale
     */
    public BiasedRandomChooser(List<Item_> itemList, double[] weightArray, Predicate<Item_> livePredicate,
            RandomGenerator random) {
        if (itemList.size() != weightArray.length) {
            throw new IllegalArgumentException("The itemList size (%d) differs from the weightArray length (%d)."
                    .formatted(itemList.size(), weightArray.length));
        }
        this.itemList = itemList;
        this.weightArray = weightArray;
        this.livePredicate = livePredicate;
        this.random = random;
        this.liveIndexArray = new int[itemList.size()];
        this.liveUpperBoundArray = new double[itemList.size()];
    }

    @Override
    public void markStale() {
        stale = true;
    }

    /**
     * Marks the chooser stale, because removing one weight changes the offsets of the items after it.
     */
    @Override
    public void removeChosen() {
        markStale();
    }

    @Override
    public boolean isEmpty() {
        refresh();
        return liveCount == 0;
    }

    /**
     * @return null if no item is live and has a non-zero weight
     */
    @Override
    public @Nullable Item_ choose() {
        if (isEmpty()) {
            return null;
        }
        var randomOffset = RandomUtils.nextDouble(random, liveUpperBoundArray[liveCount - 1]);
        var index = Arrays.binarySearch(liveUpperBoundArray, 0, liveCount, randomOffset);
        // The first live item with an upper bound above the offset.
        var liveIndex = index >= 0 ? index + 1 : -index - 1;
        return itemList.get(liveIndexArray[liveIndex]);
    }

    private void refresh() {
        if (!stale) {
            return;
        }
        liveCount = 0;
        var upperBound = 0.0;
        for (var i = 0; i < itemList.size(); i++) {
            var weight = weightArray[i];
            if (weight != 0.0 && livePredicate.test(itemList.get(i))) {
                upperBound += weight;
                liveIndexArray[liveCount] = i;
                liveUpperBoundArray[liveCount] = upperBound;
                liveCount++;
            }
        }
        stale = false;
    }

}
