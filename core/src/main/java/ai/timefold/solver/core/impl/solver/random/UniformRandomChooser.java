package ai.timefold.solver.core.impl.solver.random;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Chooses a random live item; every live item has the same chance.
 * All items start live.
 * With one live item, it uses no random number.
 * Removing a live item moves the last live item into its place;
 * {@link #reset()} restores the order of the item list.
 *
 * @param <Item_> the type of the items
 */
@NullMarked
public final class UniformRandomChooser<Item_> implements RandomChooser<Item_> {

    private final List<Item_> itemList;
    private final List<Item_> liveItemList;
    private final RandomGenerator random;
    private int chosenIndex = -1;

    /**
     * @param itemList all items; the chooser keeps it, so do not change it
     */
    public UniformRandomChooser(List<Item_> itemList, RandomGenerator random) {
        this.itemList = itemList;
        this.liveItemList = new ArrayList<>(itemList);
        this.random = random;
    }

    @Override
    public boolean isEmpty() {
        return liveItemList.isEmpty();
    }

    @Override
    public @Nullable Item_ choose() {
        var size = liveItemList.size();
        chosenIndex = switch (size) {
            case 0 -> -1;
            case 1 -> 0;
            default -> random.nextInt(size);
        };
        return chosenIndex < 0 ? null : liveItemList.get(chosenIndex);
    }

    @Override
    public void removeChosen() {
        if (chosenIndex < 0) {
            throw new IllegalStateException("Impossible state: no chosen item to remove.");
        }
        var lastItem = liveItemList.removeLast();
        if (chosenIndex != liveItemList.size()) {
            // The list order carries no meaning, so the last item fills the gap.
            liveItemList.set(chosenIndex, lastItem);
        }
        chosenIndex = -1;
    }

    @Override
    public void reset() {
        liveItemList.clear();
        liveItemList.addAll(itemList);
        chosenIndex = -1;
    }

}
