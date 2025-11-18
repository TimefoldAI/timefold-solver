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
 * <p>
 * It uses an {@link ElementPositionTracker} to track the insertion position of each element.
 * When an element is removed, it is replaced by null at its insertion position;
 * therefore the insertion position of later elements is not changed.
 * The list is compacted back during iteration or when {@link #asList()} is called.
 * <p>
 * Together with the fact that removals are relatively rare,
 * this keeps the overhead low while giving us all benefits of {@link ArrayList},
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
    private @Nullable ArrayList<@Nullable T> elementList; // Lazily initialized, so that empty indexes use no memory.
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
        if (insertionPosition == actualElementList.size() - 1) {
            // The element was the last one added; we can simply remove it.
            actualElementList.remove(insertionPosition);
            removeTailGap(actualElementList);
        } else {
            // We replace the element with null, creating a gap.
            actualElementList.set(insertionPosition, null);
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
     * @param elementConsumer the action to be performed for each element
     */
    public void forEach(Consumer<T> elementConsumer) {
        if (isEmpty()) {
            return;
        }
        findFirst(element -> {
            elementConsumer.accept(element);
            return false; // Iterate until the end.
        });
    }

    public @Nullable T findFirst(Predicate<T> elementPredicate) {
        var actualElementList = getElementList();
        for (var i = 0; i < actualElementList.size(); i++) {
            var element = actualElementList.get(i);
            if (element == null) {
                var lastNonGapIndex = removeTailGap(actualElementList);
                if (lastNonGapIndex < 0 || i >= lastNonGapIndex) {
                    return null;
                }
                element = actualElementList.remove(lastNonGapIndex);
                putElementIntoGap(actualElementList, element, i);
            }
            if (elementPredicate.test(element)) {
                return element;
            }
        }
        return null;
    }

    private void putElementIntoGap(List<@Nullable T> elementList, T element, int gap) {
        elementList.set(gap, element);
        elementPositionTracker.setPosition(element, gap);
        gapCount--;
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
        defrag(elementList);
        return elementList;
    }

    private void defrag(List<@Nullable T> actualElementList) {
        if (gapCount == 0) {
            return;
        }
        var elementsAtTheBack = 0;
        for (var i = removeTailGap(actualElementList); i >= 0 && gapCount > 0; i--) {
            if (actualElementList.get(i) == null) {
                if (elementsAtTheBack > 0) {
                    var element = actualElementList.remove(actualElementList.size() - 1);
                    putElementIntoGap(actualElementList, element, i);
                } else {
                    actualElementList.remove(i);
                    gapCount--;
                }
            } else {
                elementsAtTheBack++;
            }
        }
    }

    private int removeTailGap(List<@Nullable T> actualElementList) {
        if (gapCount == actualElementList.size()) {
            actualElementList.clear();
            gapCount = 0;
            return -1;
        }
        var end = actualElementList.size() - 1;
        while (end >= 0 && actualElementList.get(end) == null) {
            actualElementList.remove(end);
            gapCount--;
            end--;
        }
        return end;
    }

}
