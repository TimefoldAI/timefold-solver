package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

import ai.timefold.solver.core.impl.util.ElementAwareList;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * {@link ArrayList}-backed set which allows to {@link #remove(Object)} an element
 * without knowing its position and without an expensive lookup.
 * It also allows for direct random access like a list.
 * The order of iteration isn't guaranteed to be the insertion order,
 * and it changes over time as elements are added and removed.
 * <p>
 * It uses an {@link ElementPositionTracker} to track the insertion position of each element.
 * When an element is removed, it's replaced by null at its insertion position, creating a gap;
 * therefore, the insertion position of later elements isn't changed.
 * The set is compacted back during iteration or when {@link #asList()} is called,
 * by replacing these gaps with elements from the back of the set.
 * This operation also doesn't change the insertion position of any elements
 * except for those that are moved from the back to fill a gap.
 * <p>
 * Together with the fact that removals are relatively rare,
 * this keeps the overhead low while giving us all benefits of {@link ArrayList},
 * such as memory efficiency, random access, and fast iteration.
 * Random access isn't required for Constraint Streams, but Neighborhoods make heavy use of it;
 * if we used the {@link ElementAwareList} implementation instead,
 * we'd have to copy the elements to an array every time we need to access them randomly during move generation.
 * <p>
 * For performance reasons, this class doesn't check if an element was already added;
 * duplicates must be avoided by the caller and will cause undefined behavior.
 * <p>
 * This class isn't thread-safe.
 * It's in fact very thread-unsafe.
 *
 * @param <T>
 */
@NullMarked
public final class IndexedSet<T> {

    // Compaction during forEach() only makes performance sense for larger sets with a significant amount of gaps.
    static final int MINIMUM_ELEMENT_COUNT_FOR_COMPACTION = 20;
    static final double GAP_RATIO_FOR_COMPACTION = 0.1;

    private final ElementPositionTracker<T> elementPositionTracker;
    private @Nullable List<@Nullable T> elementList; // Lazily initialized, so that empty indexes use no memory.
    private int lastElementPosition = -1;
    private int gapCount = 0;

    public IndexedSet(ElementPositionTracker<T> elementPositionTracker) {
        this.elementPositionTracker = Objects.requireNonNull(elementPositionTracker);
    }

    private List<@Nullable T> getElementList() {
        if (elementList == null) {
            elementList = new ArrayList<>();
        }
        return elementList;
    }

    /**
     * Appends the specified element to the end of this collection.
     * If the element is already present,
     * an undefined, unexpected, and incorrect behavior should be expected.
     * <p>
     * Presence of the element can be checked using the associated {@link ElementPositionTracker}.
     * For performance reasons, this method avoids that check.
     *
     * @param element element to be appended to this collection
     */
    public void add(T element) {
        var actualElementList = getElementList();
        actualElementList.add(element);
        elementPositionTracker.setPosition(element, ++lastElementPosition);
    }

    /**
     * Removes the first occurrence of the specified element from this collection, if it is present.
     * Will use identity comparison to check for presence;
     * two different instances which {@link Object#equals(Object) equal} are considered different elements.
     * 
     * @param element element to be removed from this collection
     * @throws IllegalStateException if the element was not found in this collection
     */
    public void remove(T element) {
        if (!innerRemove(element)) {
            throw new IllegalStateException("Impossible state: the element (%s) was not found in the IndexedSet."
                    .formatted(element));
        }
    }

    private boolean innerRemove(T element) {
        var insertionPosition = elementPositionTracker.clearPosition(element);
        if (insertionPosition < 0) {
            return false;
        }
        if (insertionPosition == lastElementPosition) {
            // The element was the last one added; we can simply remove it.
            elementList.remove(insertionPosition);
            lastElementPosition--;
        } else {
            // We replace the element with null, creating a gap.
            elementList.set(insertionPosition, null);
            gapCount++;
        }
        clearIfPossible();
        return true;
    }

    private boolean clearIfPossible() {
        if (gapCount > 0 && lastElementPosition + 1 == gapCount) { // All positions are gaps. Clear the list entirely.
            elementList.clear();
            gapCount = 0;
            lastElementPosition = -1;
            return true;
        }
        return false;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public int size() {
        return lastElementPosition - gapCount + 1;
    }

    /**
     * Performs the given action for each element of the collection
     * until all elements have been processed.
     * The order of iteration may change as elements are added and removed.
     *
     * @param elementConsumer the action to be performed for each element;
     *        may include removing elements from the collection,
     *        but additions or swaps are not allowed;
     *        undefined behavior will occur if that is attempted.
     */
    public void forEach(Consumer<T> elementConsumer) {
        if (isEmpty()) {
            return;
        }
        forEach(element -> {
            elementConsumer.accept(element);
            return false; // Iterate until the end.
        });
    }

    private @Nullable T forEach(Predicate<T> elementPredicate) {
        return shouldCompact() ? forEachCompacting(elementPredicate) : forEachNonCompacting(elementPredicate);
    }

    private boolean shouldCompact() {
        int elementCount = lastElementPosition + 1;
        if (elementCount < MINIMUM_ELEMENT_COUNT_FOR_COMPACTION) {
            return false;
        }
        var gapPercentage = gapCount / (double) elementCount;
        return gapPercentage > GAP_RATIO_FOR_COMPACTION;
    }

    private @Nullable T forEachNonCompacting(Predicate<T> elementPredicate) {
        return forEachNonCompacting(elementPredicate, 0);
    }

    private @Nullable T forEachNonCompacting(Predicate<T> elementPredicate, int startingIndex) {
        for (var i = startingIndex; i <= lastElementPosition; i++) {
            var element = elementList.get(i);
            if (element != null && elementPredicate.test(element)) {
                return element;
            }
        }
        return null;
    }

    private @Nullable T forEachCompacting(Predicate<T> elementPredicate) {
        if (clearIfPossible()) {
            return null;
        }
        for (var i = 0; i <= lastElementPosition; i++) {
            var element = elementList.get(i);
            if (element == null) {
                element = fillGap(i);
                if (element == null) { // There was nothing to fill the gap with; we are done.
                    return null;
                }
            }
            if (elementPredicate.test(element)) {
                return element;
            }
            if (gapCount == 0) {
                // No more gaps to fill; we can continue without compacting.
                // This is an optimized loop which no longer checks for gaps.
                return forEachNonCompacting(elementPredicate, i + 1);
            }
        }
        return null;
    }

    /**
     * Fills the gap at position i by moving the last element into it.
     * 
     * @param gapPosition the position of the gap to fill
     * @return the element that now occupies position i, or null if no element further in the list can fill the gap
     */
    private @Nullable T fillGap(int gapPosition) {
        T lastRemovedElement = removeLastNonGap(gapPosition);
        if (lastRemovedElement == null) {
            return null;
        }
        elementList.set(gapPosition, lastRemovedElement);
        elementPositionTracker.setPosition(lastRemovedElement, gapPosition);
        gapCount--;
        return lastRemovedElement;
    }

    private @Nullable T removeLastNonGap(int gapPosition) {
        while (lastElementPosition >= gapPosition) {
            var lastRemovedElement = elementList.remove(lastElementPosition);
            lastElementPosition--;
            if (lastRemovedElement != null) {
                return lastRemovedElement;
            }
            gapCount--;
        }
        return null;
    }

    /**
     * As defined by {@link #forEach(Consumer)},
     * but stops when the predicate returns true for an element.
     * 
     * @param elementPredicate the predicate to be tested for each element
     * @return the first element for which the predicate returned true, or null if none
     */
    public @Nullable T findFirst(Predicate<T> elementPredicate) {
        if (isEmpty()) {
            return null;
        }
        return forEach(elementPredicate);
    }

    /**
     * Returns a standard {@link List} view of this collection.
     * Users mustn't modify the returned list, as that'd also change the underlying data structure.
     *
     * @return a standard list view of this element-aware list
     */
    public List<T> asList() {
        if (elementList == null) {
            return Collections.emptyList();
        }
        if (gapCount > 0) { // The list must not return any nulls.
            forceCompaction();
        }
        return elementList.isEmpty() ? Collections.emptyList() : elementList;
    }

    private void forceCompaction() {
        if (clearIfPossible()) {
            return;
        }
        for (var i = 0; i <= lastElementPosition; i++) {
            var element = elementList.get(i);
            if (element == null) {
                fillGap(i);
                if (gapCount == 0) { // If there are no more gaps, we can stop.
                    return;
                }
            }
        }
    }

}
