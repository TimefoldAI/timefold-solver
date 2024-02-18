package ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.kopt;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.function.TriConsumer;
import ai.timefold.solver.core.impl.domain.variable.ListVariableStateSupply;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.list.LocationInList;

/**
 * A list that delegates get and set operations to multiple delegates.
 * Add and removal operations are not supported.
 *
 * @param <T>
 */
final class MultipleDelegateList<T> implements List<T>, RandomAccess {
    final Object[] delegateEntities;
    final List<T>[] delegates;
    final int[] delegateSizes;
    final int[] offsets;
    final int totalSize;

    @SafeVarargs
    public MultipleDelegateList(Object[] delegateEntities, List<T>... delegates) {
        this.delegates = delegates;
        this.delegateEntities = delegateEntities;
        this.delegateSizes = new int[delegates.length];
        this.offsets = new int[delegates.length];

        var sizeSoFar = 0;
        for (var i = 0; i < delegates.length; i++) {
            delegateSizes[i] = delegates[i].size();
            offsets[i] = sizeSoFar;
            sizeSoFar += delegateSizes[i];
        }

        this.totalSize = sizeSoFar;
    }

    @SafeVarargs
    MultipleDelegateList(List<T>... delegates) {
        this(new Object[delegates.length], delegates);
    }

    @SuppressWarnings("unchecked")
    public MultipleDelegateList<T> copy() {
        List<T>[] delegateClones = new List[delegates.length];
        for (var i = 0; i < delegates.length; i++) {
            delegateClones[i] = new ArrayList<>(delegates[i]);
        }
        return new MultipleDelegateList<>(delegateEntities, delegateClones);
    }

    @SuppressWarnings("unchecked")
    public void applyChangesFromCopy(MultipleDelegateList<?> copy) {
        for (var i = 0; i < delegates.length; i++) {
            delegates[i].clear();
            delegates[i].addAll((List<T>) copy.delegates[i]);
        }
    }

    public int getIndexOfValue(ListVariableStateSupply<?> listVariableStateSupply, Object value) {
        var elementLocation = listVariableStateSupply.getLocationInList(value);
        if (elementLocation instanceof LocationInList elementLocationInList) {
            var entity = elementLocationInList.entity();
            var listVariableDescriptor = listVariableStateSupply.getSourceVariableDescriptor();
            for (var i = 0; i < delegateEntities.length; i++) {
                if (delegateEntities[i] == entity) {
                    var firstUnpinnedIndex = listVariableDescriptor.getFirstUnpinnedIndex(delegateEntities[i]);
                    return offsets[i] + (elementLocationInList.index() - firstUnpinnedIndex);
                }
            }
        }
        // Unassigned or not found.
        throw new IllegalArgumentException("Value (" + value + ") is not contained in any entity list");
    }

    public void actOnAffectedElements(ListVariableDescriptor<?> listVariableDescriptor, Object[] originalEntities,
            TriConsumer<Object, Integer, Integer> action) {
        for (Object originalEntity : originalEntities) {
            action.accept(originalEntity,
                    listVariableDescriptor.getFirstUnpinnedIndex(originalEntity),
                    listVariableDescriptor.getListSize(originalEntity));
        }
    }

    public void moveElementsOfDelegates(int[] newDelegateEndIndices) {
        List<T>[] newDelegateData = new List[delegates.length];

        var start = 0;
        for (var i = 0; i < newDelegateData.length; i++) {
            newDelegateData[i] = List.copyOf(subList(start, newDelegateEndIndices[i] + 1));
            start = newDelegateEndIndices[i] + 1;
        }

        for (var i = 0; i < delegates.length; i++) {
            delegates[i].clear();
            delegates[i].addAll(newDelegateData[i]);
        }

        var sizeSoFar = 0;
        for (var i = 0; i < delegates.length; i++) {
            delegateSizes[i] = delegates[i].size();
            offsets[i] = sizeSoFar;
            sizeSoFar += delegateSizes[i];
        }
    }

    @Override
    public int size() {
        return totalSize;
    }

    @Override
    public boolean isEmpty() {
        return totalSize == 0;
    }

    @Override
    public boolean contains(Object o) {
        for (var delegate : delegates) {
            if (delegate.contains(o)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterator<T> iterator() {
        return Stream.of(delegates).flatMap(Collection::stream).iterator();
    }

    @Override
    public Object[] toArray() {
        return Stream.of(delegates).flatMap(Collection::stream).toArray();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T1> T1[] toArray(T1[] t1s) {
        var out = Stream.of(delegates).flatMap(Collection::stream).toArray(size -> {
            if (size <= t1s.length) {
                return t1s;
            } else {
                return (T1[]) Array.newInstance(t1s.getClass().getComponentType(), size);
            }
        });

        if (out.length > totalSize) {
            out[totalSize] = null;
        }
        return out;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return collection.stream().allMatch(this::contains);
    }

    private int getDelegateIndex(int actualIndex) {
        var delegateIndex = 0;
        while (delegateSizes[delegateIndex] <= actualIndex) {
            actualIndex -= delegateSizes[delegateIndex];
            delegateIndex++;
        }
        return delegateIndex;
    }

    @Override
    public T get(int i) {
        if (i < 0 || i >= totalSize) {
            throw new IndexOutOfBoundsException("Index (" + i + ") out of bounds for a list of size (" + totalSize + ")");
        }
        var delegateIndex = getDelegateIndex(i);
        return delegates[delegateIndex].get(i - offsets[delegateIndex]);
    }

    @Override
    public T set(int i, T t) {
        if (i < 0 || i >= totalSize) {
            throw new IndexOutOfBoundsException("Index (" + i + ") out of bounds for a list of size (" + totalSize + ")");
        }
        var delegateIndex = getDelegateIndex(i);
        return delegates[delegateIndex].set(i - offsets[delegateIndex], t);
    }

    @Override
    public int indexOf(Object o) {
        if (delegates.length == 0) {
            return -1;
        }

        var delegateIndex = 0;
        var objectIndex = -1;
        var offset = 0;

        while (delegateIndex < delegates.length && (objectIndex = delegates[delegateIndex].indexOf(o)) == -1) {
            // We do the indexOf in the while condition so offset is not updated
            // for the list that contains the element
            // (ex: [1, 2, 3].indexOf(2) should return 1, not 4)
            offset += delegateSizes[delegateIndex];
            delegateIndex++;
        }

        if (objectIndex == -1) {
            return -1;
        }

        return offset + objectIndex;
    }

    @Override
    public int lastIndexOf(Object o) {
        if (delegates.length == 0) {
            return -1;
        }

        var delegateIndex = delegates.length - 1;
        var objectIndex = -1;
        var offset = 0;

        while (delegateIndex >= 0 && objectIndex == -1) {
            // We update index here so offset is updated with the containing
            // list size (since totalSize is subtracted from it)
            // (ex: [1, 2, 3].lastIndexOf(2) should return 1, not 4)
            objectIndex = delegates[delegateIndex].lastIndexOf(o);
            offset += delegateSizes[delegateIndex];
            delegateIndex--;
        }

        if (objectIndex == -1) {
            return -1;
        }

        return (totalSize - offset) + objectIndex;
    }

    @Override
    public ListIterator<T> listIterator() {
        return new MultipleDelegateListIterator<>(this, 0);
    }

    @Override
    public ListIterator<T> listIterator(int i) {
        return new MultipleDelegateListIterator<>(this, i);
    }

    @Override
    public MultipleDelegateList<T> subList(int startInclusive, int endExclusive) {
        if (startInclusive < 0) {
            throw new IndexOutOfBoundsException("Sublist start index (" + startInclusive + ") out of range");
        }
        if (endExclusive > totalSize) {
            throw new IndexOutOfBoundsException("Sublist end index (" + endExclusive + ") out of range");
        }

        var startDelegateIndex = 0;
        var endDelegateIndex = 0;

        while (startInclusive >= delegateSizes[startDelegateIndex]) {
            startInclusive -= delegateSizes[startDelegateIndex];
            startDelegateIndex++;
        }

        while (endExclusive > delegateSizes[endDelegateIndex]) {
            endExclusive -= delegateSizes[endDelegateIndex];
            endDelegateIndex++;
        }

        @SuppressWarnings("unchecked")
        List<T>[] out = new List[endDelegateIndex - startDelegateIndex + 1];
        if (out.length == 0) {
            return new MultipleDelegateList<>(delegateEntities);
        }
        if (startDelegateIndex == endDelegateIndex) {
            out[0] = delegates[startDelegateIndex].subList(startInclusive, endExclusive);
        } else {
            out[0] = delegates[startDelegateIndex].subList(startInclusive, delegateSizes[startDelegateIndex]);
            out[out.length - 1] = delegates[endDelegateIndex].subList(0, endExclusive);
            System.arraycopy(delegates, startDelegateIndex + 1, out, 1, out.length - 2);
        }

        return new MultipleDelegateList<>(delegateEntities, out);
    }

    @Override
    public boolean add(T t) {
        throw new UnsupportedOperationException("Cannot add new elements to a multiple delegate list");
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException("Cannot remove elements from a multiple delegate list");
    }

    @Override
    public void add(int i, T t) {
        throw new UnsupportedOperationException("Cannot add new elements to a multiple delegate list");
    }

    @Override
    public T remove(int i) {
        throw new UnsupportedOperationException("Cannot remove elements from a multiple delegate list");
    }

    @Override
    public boolean addAll(Collection<? extends T> collection) {
        throw new UnsupportedOperationException("Cannot add new elements to a multiple delegate list");
    }

    @Override
    public boolean addAll(int i, Collection<? extends T> collection) {
        throw new UnsupportedOperationException("Cannot add new elements to a multiple delegate list");
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        throw new UnsupportedOperationException("Cannot remove elements from a multiple delegate list");
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        throw new UnsupportedOperationException("Cannot remove elements from a multiple delegate list");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("Cannot remove elements from a multiple delegate list");
    }

    @Override
    public String toString() {
        return Arrays.toString(delegates);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof List other) {
            if (other.size() != totalSize) {
                return false;
            }
            for (var i = 0; i < totalSize; i++) {
                if (!Objects.equals(other.get(i), get(i))) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(delegates);
    }

    private static final class MultipleDelegateListIterator<T> implements ListIterator<T> {
        final MultipleDelegateList<T> parent;
        int currentIndex;

        public MultipleDelegateListIterator(MultipleDelegateList<T> parent, int currentIndex) {
            this.parent = parent;
            this.currentIndex = currentIndex;
        }

        @Override
        public boolean hasNext() {
            return currentIndex < parent.totalSize;
        }

        @Override
        public T next() {
            var out = parent.get(currentIndex);
            currentIndex++;
            return out;
        }

        @Override
        public boolean hasPrevious() {
            return currentIndex > 0;
        }

        @Override
        public T previous() {
            currentIndex--;
            return parent.get(currentIndex);
        }

        @Override
        public int nextIndex() {
            return currentIndex + 1;
        }

        @Override
        public int previousIndex() {
            return currentIndex - 1;
        }

        @Override
        public void set(T t) {
            parent.set(currentIndex, t);
        }

        @Override
        public void add(T t) {
            throw new UnsupportedOperationException("Cannot add new elements to a multiple delegate list");
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Cannot remove elements to a multiple delegate list");
        }
    }
}
