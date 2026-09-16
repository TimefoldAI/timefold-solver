package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.NoSuchElementException;
import java.util.random.RandomGenerator;

import ai.timefold.solver.core.impl.util.ElementAwareArrayList;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Implements a lazy Fisher-Yates shuffle over the live (not yet retired) slots,
 * so that both a single draw and a full drain are exactly uniform,
 * even after retirements.
 * It accepts a list of unique items on input,
 * and does not copy or modify it.
 * <p>
 * The shuffle runs over the list's {@link ElementAwareArrayList#slotCount() physical slots},
 * not over its logical indexes,
 * because resolving a logical index would make the list compact on every single draw.
 * A slot which holds a gap is simply drawn again rather than retired,
 * which keeps the draw uniform over the elements
 * (a uniform draw over a superset, rejecting the non-members, is uniform over the members)
 * and keeps {@link SlotReservationMap} free of reservations it would otherwise spend on gaps.
 * <p>
 * The classic Fisher-Yates shuffle repeats,
 * over a shrinking range {@code [0, i]}:
 * pick a random index {@code j} in the range,
 * swap the items at {@code i} and {@code j},
 * then shrink the range ({@code i--}).
 * This class performs the same steps,
 * split across two calls,
 * over a virtual permutation layer instead of the real list:
 * <ul>
 * <li>{@link #hasNext()} draws the random {@code j}:
 * {@code nextSlot = workingRandom.nextInt(activeCount)}.
 * <li>{@link #retire()} performs the swap-and-shrink:
 * it moves the item that lives in the last live slot into the retired slot's position
 * (see {@link #slotMap} for this "reservation"),
 * then shrinks {@link #activeCount} so the last slot drops out of the live range.
 * </ul>
 * Only one side of the swap is ever written,
 * because the slot that drops out of range is never read again.
 *
 * @param <T>
 */
@NullMarked
final class DefaultRetiringRandomIterator<T extends @Nullable Object>
        implements RetiringRandomIterator<T> {

    private final ElementAwareArrayList<T> source;
    private final RandomGenerator workingRandom;
    /**
     * Maps a live Fisher-Yates slot to the physical slot (into {@link #source}) it currently holds;
     * a slot which holds no reservation holds the physical slot equal to itself
     * (the identity mapping every slot starts with).
     * {@link SlotReservationMap#resolve(int)} is therefore always a bijection
     * from the live slots {@code [0, activeCount)}
     * onto the not-yet-retired physical slots,
     * which is what makes every draw and every full drain exactly uniform:
     * a draw picks a live slot uniformly,
     * and {@link #retire()} swaps the retired slot with the last live slot
     * (updating only that one reservation)
     * instead of leaving a gap that would have to be walked around.
     * <p>
     * {@link SlotReservationMap} keeps the reservations in a small pair log while there are few of them,
     * and only upgrades to an {@code int[]} of every logical index once the log fills up,
     * because one of these iterators is built per neighborhood per step,
     * over datasets that can hold well over 100,000 tuples,
     * and most of them retire only a handful of elements.
     */
    private final SlotReservationMap slotMap;

    /**
     * [0, activeCount) is the live Fisher-Yates range, over {@link ElementAwareArrayList#slotCount() physical slots}.
     * Because gaps are never retired, this range holds the not-yet-retired elements plus every gap;
     * {@link #liveRemaining} is what says whether any element is left in it.
     */
    private int activeCount;

    /**
     * How many elements the range still holds, as opposed to gaps.
     * Needed because {@link #activeCount} counts slots, and a positive slot count may be all gaps.
     */
    private int liveRemaining;

    private int nextSlot = -1;
    private @Nullable T next = null;
    private int slotToOptionallyRetire = -1;

    DefaultRetiringRandomIterator(ElementAwareArrayList<T> source, RandomGenerator workingRandom) {
        this.source = source;
        this.workingRandom = workingRandom;
        this.activeCount = source.slotCount();
        this.liveRemaining = source.size();
        // activeCount only ever shrinks, so no slot outside [0, slotCount) is ever resolved.
        this.slotMap = new SlotReservationMap(activeCount);
    }

    @Override
    public boolean hasNext() {
        if (nextSlot != -1) {
            return true;
        }
        // The source does not change while this iterator exists, so liveRemaining > 0 guarantees
        // the live range still holds an element, and this loop always finds one.
        while (liveRemaining > 0) {
            var candidateSlot = workingRandom.nextInt(activeCount); // The Fisher-Yates random pick.
            var entry = source.entryAt(slotMap.resolve(candidateSlot));
            if (entry != null) {
                nextSlot = candidateSlot;
                next = entry.element();
                slotToOptionallyRetire = -1;
                return true;
            }
            // A gap. Just draw again: retiring it would swap-and-shrink, and that reservation
            // would eat one of SlotReservationMap's 16 sparse entries, forcing an int[slotCount] upgrade.
        }
        return false;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        slotToOptionallyRetire = nextSlot;
        var returnValue = next;
        nextSlot = -1;
        next = null;
        return returnValue;
    }

    @Override
    public void retire() { // Fisher-Yates swap-and-shrink; one-sided lazy reservation update.
        if (slotToOptionallyRetire == -1) {
            throw new IllegalStateException(
                    "The next() method has not been called yet, or the retire() method was already called after the last next() call.");
        }
        var retiredSlot = slotToOptionallyRetire;
        var lastSlot = activeCount - 1;
        if (retiredSlot != lastSlot) {
            slotMap.reserve(retiredSlot, slotMap.resolve(lastSlot));
        }
        slotMap.release(lastSlot);
        activeCount = lastSlot;
        liveRemaining--;
        slotToOptionallyRetire = -1;
    }
}
