package ai.timefold.solver.core.impl.bavet.common.index;

import ai.timefold.solver.core.impl.util.ElementAwareList;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

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
 * <p>
 * This class is not thread-safe.
 * It is in fact very thread-unsafe.
 *
 * @param <T>
 */
@NullMarked
public final class IndexedSet<T> {

    private final ElementPositionTracker<T> elementPositionTracker;
    private @Nullable ArrayList<T> elementList; // Lazily initialized, so that empty indexes use no memory.
    private final BitSet gaps = new BitSet(0);
    private int gapCount = 0;

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
        if (gapCount > 0) {
            var gapIndex = gaps.nextSetBit(0);
            actualElementList.set(gapIndex, element);
            elementPositionTracker.setPosition(element, gapIndex);
            gaps.clear(gapIndex);
            gapCount--;
        } else {
            actualElementList.add(element);
            elementPositionTracker.setPosition(element, actualElementList.size() - 1);
        }
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
        if (insertionPosition == actualElementList.size() - 1) {
            // The element was the last one added; we can simply remove it.
            actualElementList.remove(insertionPosition);
        } else {
            actualElementList.set(insertionPosition, null);
            gaps.set(insertionPosition);
            gapCount++;
        }
        return true;
    }

    public int size() {
        return elementList == null ? 0 : elementList.size() - gapCount;
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
        for (var i = 0; i < elementList.size(); i++) {
            var element = elementList.get(i);
            if (element != null) {
                tupleConsumer.accept(element);
            }
        }
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Returns a standard {@link List} view of this collection.
     * Users must not modify the returned list, as that would also change the underlying data structure.
     *
     * @return a standard list view of this element-aware list
     */
    public List<T> asList() {
        if (elementList == null) {
            return Collections.emptyList();
        }
        var actualElementList = getElementList();
        defrag(actualElementList);
        return actualElementList;
    }

    private void defrag(List<T> actualElementList) {
        if (gapCount == 0) {
            return;
        }
        var gap = gaps.nextSetBit(0);
        while (gap >= 0) {
            var lastNonGapIndex = findNonGapFromEnd(actualElementList);
            if (lastNonGapIndex < 0 || gap >= lastNonGapIndex) {
                break;
            }
            var lastElement = actualElementList.remove(lastNonGapIndex);
            actualElementList.set(gap, lastElement);
            elementPositionTracker.setPosition(lastElement, gap);
            gap = gaps.nextSetBit(gap + 1);
        }
        gaps.clear();
        gapCount = 0;
    }

    private int findNonGapFromEnd(List<T> actualElementList) {
        var end = actualElementList.size() - 1;
        var lastNonGap = gaps.previousClearBit(end);
        for (var i = end; i > lastNonGap; i--) {
            actualElementList.remove(i);
        }
        return lastNonGap;
    }

}
