package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import ai.timefold.solver.core.impl.util.ElementAwareList;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * {@link ArrayList}-backed set which allows to {@link #remove(Object)} an element
 * without knowing its position and without an expensive lookup.
 * It also allows for direct random access like a list.
 * <p>
 * It uses an {@link ElementPositionTracker} to track the insertion position of each element.
 * When an element is removed, the insertion position of later elements is not changed.
 * Instead, when the next element is removed, the search starts from its last known insertion position,
 * iterating backwards until the element is found.
 * We also keep a counter of deleted elements to avoid excessive iteration;
 * we can guarantee that the current position of an element will not be further away
 * than the number of earlier deletions.
 * <p>
 * Together with the fact that removals are relatively rare,
 * this keeps the average removal cost low while giving us all benefits of {@link ArrayList},
 * such as memory efficiency, random access, and fast iteration.
 * Random access is not required for Constraint Streams, but Neighborhoods make heavy use of it;
 * if we used the {@link ElementAwareList} implementation instead,
 * we would have to copy the elements to an array every time we need to access them randomly during move generation.
 * <p>
 * For performance reasons, this class does not check if an element was already added;
 * duplicates must be avoided by the caller and will cause undefined behavior.
 *
 * @param <T>
 */
@NullMarked
public final class IndexedSet<T> {

    private final ElementPositionTracker<T> elementPositionTracker;
    private @Nullable ArrayList<T> elementList; // Lazily initialized, so that empty indexes use no memory.
    private int removalCount = 0;

    public IndexedSet(ElementPositionTracker<T> elementPositionTracker) {
        this.elementPositionTracker = Objects.requireNonNull(elementPositionTracker);
    }

    private List<T> getElementList() {
        if (elementList == null) {
            elementList = new ArrayList<>();
        }
        return elementList;
    }

    /**
     * Appends the specified element to the end of this collection.
     * If the element is already present,
     * undefined, unexpected, and incorrect behavior should be expected.
     * <p>
     * Presence of the element can be checked using the associated {@link ElementPositionTracker}.
     * For performance reasons, this method avoids that check.
     *
     * @param element element to be appended to this collection
     */
    public void add(T element) {
        var actualElementList = getElementList();
        actualElementList.add(element);
        elementPositionTracker.setPosition(element, actualElementList.size() - 1);
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
        if (isEmpty()) {
            return false;
        }
        var insertionPosition = elementPositionTracker.clearPosition(element);
        if (insertionPosition < 0) {
            return false;
        }
        var actualElementList = getElementList();
        var upperBound = Math.min(insertionPosition, actualElementList.size() - 1);
        var lowerBound = Math.max(0, insertionPosition - removalCount);
        var actualPosition = findElement(actualElementList, element, lowerBound, upperBound);
        if (actualPosition < 0) {
            return false;
        }
        actualElementList.remove(actualPosition);
        if (isEmpty()) {
            removalCount = 0;
        } else if (actualElementList.size() > actualPosition) {
            // We only mark removals that actually affect later elements.
            // Removing the last element does not affect any other element.
            removalCount++;
        }
        return true;
    }

    /**
     * Search for the element in the given range.
     *
     * @param actualElementList the list to search in
     * @param element the element to search for
     * @param startIndex start of the range we are currently considering (inclusive)
     * @param endIndex end of the range we are currently considering (inclusive)
     * @return the index of the element if found, -1 otherwise
     */
    private static <T> int findElement(List<T> actualElementList, T element, int startIndex, int endIndex) {
        for (var i = endIndex; i >= startIndex; i--) {
            // Iterating backwards as the element is more likely to be closer to the end of the range,
            // which is where it was originally inserted.
            var maybeElement = actualElementList.get(i);
            if (maybeElement == element) {
                return i;
            }
        }
        return -1;
    }

    public int size() {
        return elementList == null ? 0 : elementList.size();
    }

    /**
     * Performs the given action for each element of the collection
     * until all elements have been processed.
     *
     * @param tupleConsumer the action to be performed for each element
     */
    public void forEach(Consumer<T> tupleConsumer) {
        if (elementList == null) {
            return;
        }
        var i = 0;
        while (i < elementList.size()) {
            var oldRemovalCount = removalCount; // The consumer may remove some elements, shifting others forward.
            tupleConsumer.accept(elementList.get(i));
            var elementDrift = removalCount - oldRemovalCount;
            // Move to the next element, adjusting for any shifts due to removals.
            // If no elements were removed by the consumer, we simply move to the next index.
            i -= elementDrift - 1;
        }
    }

    public boolean isEmpty() {
        return elementList == null || elementList.isEmpty();
    }

    /**
     * Returns a standard {@link List} view of this collection.
     * Users must not modify the returned list, as that would also change the underlying data structure.
     *
     * @return a standard list view of this element-aware list
     */
    public List<T> asList() {
        return elementList == null ? Collections.emptyList() : elementList;
    }

    public String toString() {
        return elementList == null ? "[]" : elementList.toString();
    }

}
