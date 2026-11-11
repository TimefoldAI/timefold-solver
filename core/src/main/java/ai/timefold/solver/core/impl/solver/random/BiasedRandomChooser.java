package ai.timefold.solver.core.impl.solver.random;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.random.RandomGenerator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Chooses a random live item; the chance of each item is proportional to its weight.
 * Items with weight 0 are never live, and neither are items that the optional live predicate rejects.
 *
 * @param <Item_> the type of the items
 */
@NullMarked
public final class BiasedRandomChooser<Item_> implements RandomChooser<Item_> {

    private final List<Item_> itemList;
    private final double[] weightArray;
    // Tested once, at the first choice; then null.
    private @Nullable Predicate<Item_> livePredicate;
    private final boolean[] excludedArray;
    private final RandomGenerator random;
    private final boolean[] removedArray;
    private final int[] liveIndexArray;
    // The sum of the weights of the live items up to and including each live item.
    private final double[] liveUpperBoundArray;
    private int liveCount = 0;
    private int chosenItemIndex = -1;
    private boolean stale = true;

    /**
     * @param itemList all items, in the order that maps the random offset to an item;
     *        the chooser keeps it, so do not change it
     * @param weightArray the weight of each item, at the same index as in itemList; the chooser keeps it too
     */
    public BiasedRandomChooser(List<Item_> itemList, double[] weightArray, RandomGenerator random) {
        this(itemList, weightArray, null, random);
    }

    /**
     * @param livePredicate tested once per item with a weight other than 0, at the first choice; a rejected item is never live,
     *        also after
     *        {@link #reset()}
     */
    public BiasedRandomChooser(List<Item_> itemList, double[] weightArray, @Nullable Predicate<Item_> livePredicate,
            RandomGenerator random) {
        if (itemList.size() != weightArray.length) {
            throw new IllegalArgumentException("The itemList size (%d) differs from the weightArray length (%d)."
                    .formatted(itemList.size(), weightArray.length));
        }
        this.itemList = itemList;
        this.weightArray = weightArray;
        this.livePredicate = livePredicate;
        this.random = random;
        this.removedArray = new boolean[itemList.size()];
        this.excludedArray = new boolean[itemList.size()];
        this.liveIndexArray = new int[itemList.size()];
        this.liveUpperBoundArray = new double[itemList.size()];
    }

    /**
     * Removing one weight changes the offsets of the items after it, so they are computed again.
     */
    @Override
    public void removeChosen() {
        if (chosenItemIndex < 0) {
            throw new IllegalStateException("Impossible state: no chosen item to remove.");
        }
        removedArray[chosenItemIndex] = true;
        chosenItemIndex = -1;
        stale = true;
    }

    @Override
    public void reset() {
        Arrays.fill(removedArray, false);
        chosenItemIndex = -1;
        stale = true;
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
            chosenItemIndex = -1;
            return null;
        }
        var randomOffset = RandomUtils.nextDouble(random, liveUpperBoundArray[liveCount - 1]);
        var index = Arrays.binarySearch(liveUpperBoundArray, 0, liveCount, randomOffset);
        // The first live item with an upper bound above the offset.
        var liveIndex = index >= 0 ? index + 1 : -index - 1;
        // Rounding can make the offset equal to the total; that offset belongs to the last live item.
        chosenItemIndex = liveIndexArray[Math.min(liveIndex, liveCount - 1)];
        return itemList.get(chosenItemIndex);
    }

    private void refresh() {
        if (!stale) {
            return;
        }
        if (livePredicate != null) {
            for (var i = 0; i < itemList.size(); i++) {
                // Items with weight 0 are never live, so their predicate is not tested.
                excludedArray[i] = weightArray[i] != 0.0 && !livePredicate.test(itemList.get(i));
            }
            livePredicate = null;
        }
        liveCount = 0;
        var upperBound = 0.0;
        for (var i = 0; i < itemList.size(); i++) {
            var weight = weightArray[i];
            if (weight != 0.0 && !removedArray[i] && !excludedArray[i]) {
                upperBound += weight;
                liveIndexArray[liveCount] = i;
                liveUpperBoundArray[liveCount] = upperBound;
                liveCount++;
            }
        }
        stale = false;
    }

}
