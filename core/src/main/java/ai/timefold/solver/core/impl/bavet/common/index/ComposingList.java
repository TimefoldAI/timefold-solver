package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.AbstractList;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A list that is composed of multiple sublists.
 * If an element from any particular sublist is never {@link #get(int) accessed},
 * it need not be stored in memory.
 *
 * <p>
 * Note that this implementation does not support modifications of the list, or any of its sublists.
 *
 * @param <E> the type of elements in this list
 */
@NullMarked
final class ComposingList<E> extends AbstractList<E> {

    /**
     * Instead of copying all elements into a single list, we keep a map of sublists.
     * This is more memory-efficient and faster to initialize, especially when some sublists are large.
     * Key is the starting index of the sublist in the overall list.
     */
    private final NavigableMap<Integer, List<E>> listOfLists = new TreeMap<>();
    private int size = 0;

    /**
     * Adds a sublist to this composing list.
     * 
     * @param list the list to add
     */
    public void addSubList(List<E> list) {
        var listSize = list.size();
        if (listSize == 0) { // No point in adding an empty list.
            return;
        }
        listOfLists.put(size, list);
        size += list.size();
    }

    /**
     * Adds a sublist to this composing list, which is created on demand using the given supplier.
     * 
     * @param listSupplier the supplier of the sublist
     * @param listSize the size of the sublist, if we can determine it beforehand;
     *        otherwise create the list and use {@link #addSubList(List)}
     */
    public void addSubList(Supplier<? extends List<E>> listSupplier, int listSize) {
        if (listSize == 0) { // No point in adding an empty list.
            return;
        }
        listOfLists.put(size, new OnDemandList<>(listSupplier));
        size += listSize;
    }

    @Override
    public E get(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("Index: %d, Size: %d".formatted(index, size));
        }
        var headMap = listOfLists.headMap(index + 1);
        var subListStartIndex = headMap.lastKey();
        var subList = listOfLists.get(subListStartIndex);
        var indexInSubList = index - subListStartIndex;
        return subList.get(indexInSubList);
    }

    @Override
    public int size() {
        return size;
    }

    private static final class OnDemandList<E> extends AbstractList<E> {

        private final Supplier<? extends List<E>> listSupplier;
        private @Nullable List<E> list;

        public OnDemandList(Supplier<? extends List<E>> listSupplier) {
            this.listSupplier = listSupplier;
        }

        private List<E> getList() {
            if (list == null) {
                list = listSupplier.get();
            }
            return list;
        }

        @Override
        public E get(int index) {
            return getList().get(index);
        }

        @Override
        public int size() {
            return getList().size();
        }

    }

}
