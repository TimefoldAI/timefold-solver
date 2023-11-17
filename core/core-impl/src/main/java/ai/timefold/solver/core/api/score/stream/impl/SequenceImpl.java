package ai.timefold.solver.core.api.score.stream.impl;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.score.stream.api.Break;
import ai.timefold.solver.core.api.score.stream.api.Sequence;

final class SequenceImpl<Value_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        implements Sequence<Value_, Difference_> {

    private final BiFunction<Point_, Point_, Difference_> lengthFunction;
    private final ConsecutiveSetTree<Value_, Point_, Difference_> sourceTree;
    ComparableValue<Value_, Point_> firstItem;
    ComparableValue<Value_, Point_> lastItem;

    // Memorized calculations
    private Difference_ length;
    private NavigableSet<ComparableValue<Value_, Point_>> items;

    SequenceImpl(ConsecutiveSetTree<Value_, Point_, Difference_> sourceTree, ComparableValue<Value_, Point_> item,
            BiFunction<Point_, Point_, Difference_> lengthFunction) {
        this(sourceTree, item, item, lengthFunction);
    }

    SequenceImpl(ConsecutiveSetTree<Value_, Point_, Difference_> sourceTree, ComparableValue<Value_, Point_> firstItem,
            ComparableValue<Value_, Point_> lastItem, BiFunction<Point_, Point_, Difference_> lengthFunction) {
        this.lengthFunction = lengthFunction;
        this.sourceTree = sourceTree;
        this.firstItem = firstItem;
        this.lastItem = lastItem;
        length = null;
        items = null;
    }

    @Override
    public Value_ getFirstItem() {
        return firstItem.value();
    }

    @Override
    public Value_ getLastItem() {
        return lastItem.value();
    }

    @Override
    public Break<Value_, Difference_> getPreviousBreak() {
        return sourceTree.getBreakBefore(firstItem);
    }

    @Override
    public Break<Value_, Difference_> getNextBreak() {
        return sourceTree.getBreakAfter(lastItem);
    }

    @Override
    public boolean isFirst() {
        return firstItem == sourceTree.getFirstItem();
    }

    @Override
    public boolean isLast() {
        return lastItem == sourceTree.getLastItem();
    }

    @Override
    public Iterable<Value_> getItems() {
        return () -> new Iterator<>() {

            private final Iterator<ComparableValue<Value_, Point_>> iterator = getComparableItems().iterator();

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Value_ next() {
                return iterator.next().value();
            }
        };
    }

    NavigableSet<ComparableValue<Value_, Point_>> getComparableItems() {
        if (items == null) {
            return items = sourceTree.getItemSet()
                    .subSet(firstItem, true, lastItem, true);
        }
        return items;
    }

    @Override
    public int getCount() {
        return getComparableItems().size();
    }

    @Override
    public Difference_ getLength() {
        if (length == null) {
            // memoize length for later calls
            // (assignment returns the right hand side)
            return length = lengthFunction.apply(firstItem.index(), lastItem.index());
        }
        return length;
    }

    void setStart(ComparableValue<Value_, Point_> item) {
        firstItem = item;
        invalidate();
    }

    void setEnd(ComparableValue<Value_, Point_> item) {
        lastItem = item;
        invalidate();
    }

    // Called when start or end are removed; length
    // need to be invalidated
    void invalidate() {
        length = null;
        items = null;
    }

    SequenceImpl<Value_, Point_, Difference_> split(ComparableValue<Value_, Point_> fromElement) {
        var itemSet = getComparableItems();
        var newSequenceStart = itemSet.higher(fromElement);
        var newSequenceEnd = lastItem;
        setEnd(itemSet.lower(fromElement));
        return new SequenceImpl<>(sourceTree, newSequenceStart, newSequenceEnd, lengthFunction);
    }

    // This Sequence is ALWAYS before other Sequence
    void merge(SequenceImpl<Value_, Point_, Difference_> other) {
        lastItem = other.lastItem;
        invalidate();
    }

    @Override
    public String toString() {
        return getComparableItems().stream()
                .map(s -> s.value().toString())
                .collect(Collectors.joining(", ", "Sequence [", "]"));
    }
}
