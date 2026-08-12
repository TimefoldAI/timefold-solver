package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.HashMap;
import java.util.Map;
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
 *
 * @param <T>
 */
@NullMarked
final class DefaultRetiringRandomIterator<T extends @Nullable Object>
        implements RetiringRandomIterator<T> {

    private final ElementAwareArrayList<T> source;
    private final RandomGenerator workingRandom;
    /**
     * Maps a live slot to the logical index (into {@link #source}) it currently holds;
     * a slot absent from the map holds the logical index equal to itself
     * (the identity mapping every slot starts with).
     * {@link #resolveSlot(int)} is therefore always a bijection from the live slots {@code [0, activeCount)}
     * onto the live logical indexes,
     * which is what makes every draw and every full drain exactly uniform:
     * a draw picks a live slot uniformly,
     * and {@link #retire()} swaps the retired slot with the last live slot
     * (updating only that one map entry)
     * instead of leaving a gap that would have to be walked around.
     * A {@link HashMap} is used instead of an eager {@code int[]} permutation of every logical index,
     * because one of these iterators is built per neighborhood per step,
     * over datasets that can hold well over 100,000 tuples;
     * a {@link HashMap} only grows with the number of retirements, not with the size of {@link #source}.
     */
    private final Map<Integer, Integer> slotMap = new HashMap<>();

    private int activeCount;

    private int nextSlot = -1;
    private @Nullable T next = null;
    private int slotToOptionallyRetire = -1;

    DefaultRetiringRandomIterator(ElementAwareArrayList<T> source, RandomGenerator workingRandom) {
        this.source = source;
        this.workingRandom = workingRandom;
        this.activeCount = source.size();
    }

    @Override
    public boolean hasNext() {
        if (activeCount <= 0) {
            return false;
        }
        if (nextSlot != -1) {
            return true;
        }
        nextSlot = workingRandom.nextInt(activeCount);
        next = source.get(resolveSlot(nextSlot));
        slotToOptionallyRetire = -1;
        return true;
    }

    private int resolveSlot(int slot) {
        var result = slotMap.get(slot);
        if (result != null) {
            return result;
        }
        return slot;
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
    public void retire() {
        if (slotToOptionallyRetire == -1) {
            throw new IllegalStateException(
                    "The next() method has not been called yet, or the retire() method was already called after the last next() call.");
        }
        var retiredSlot = slotToOptionallyRetire;
        var lastSlot = activeCount - 1;
        if (retiredSlot != lastSlot) {
            slotMap.put(retiredSlot, resolveSlot(lastSlot));
        }
        slotMap.remove(lastSlot);
        activeCount = lastSlot;
        slotToOptionallyRetire = -1;
    }
}
