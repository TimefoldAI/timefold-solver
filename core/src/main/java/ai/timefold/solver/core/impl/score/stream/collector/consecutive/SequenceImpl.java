package ai.timefold.solver.core.impl.score.stream.collector.consecutive;

import java.util.Collection;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.stream.Collectors;

import ai.timefold.solver.core.api.score.stream.common.Break;
import ai.timefold.solver.core.api.score.stream.common.Sequence;

/**
 * When adding fields, remember to add them to the JSON serialization code as well, if you want them exposed.
 *
 * @param <Value_>
 * @param <Point_>
 * @param <Difference_>
 */
final class SequenceImpl<Value_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        implements Sequence<Value_, Difference_> {

    private final ConsecutiveSetTree<Value_, Point_, Difference_> sourceTree;
    ComparableValue<Value_, Point_> firstItem;
    ComparableValue<Value_, Point_> lastItem;

    // Memorized calculations
    private Difference_ length;
    private NavigableMap<ComparableValue<Value_, Point_>, Value_> comparableItems;
    private Collection<Value_> items;

    SequenceImpl(ConsecutiveSetTree<Value_, Point_, Difference_> sourceTree, ComparableValue<Value_, Point_> item) {
        this(sourceTree, item, item);
    }

    SequenceImpl(ConsecutiveSetTree<Value_, Point_, Difference_> sourceTree, ComparableValue<Value_, Point_> firstItem,
            ComparableValue<Value_, Point_> lastItem) {
        this.sourceTree = sourceTree;
        this.firstItem = firstItem;
        this.lastItem = lastItem;
        length = null;
        comparableItems = null;
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
    public Collection<Value_> getItems() {
        if (items == null) {
            return items = getComparableItems().values();
        }
        return Collections.unmodifiableCollection(items);

    }

    NavigableMap<ComparableValue<Value_, Point_>, Value_> getComparableItems() {
        if (comparableItems == null) {
            return comparableItems = sourceTree.getComparableItems(firstItem, lastItem);
        }
        return comparableItems;
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
            return length = sourceTree.sequenceLengthFunction.apply(firstItem.index(), lastItem.index());
        }
        return length;
    }

    Difference_ computeDifference(SequenceImpl<Value_, Point_, Difference_> to) {
        return sourceTree.differenceFunction.apply(this.lastItem.index(), to.firstItem.index());
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
        comparableItems = null;
        items = null;
    }

    SequenceImpl<Value_, Point_, Difference_> split(ComparableValue<Value_, Point_> fromElement) {
        var itemSet = getComparableItems();
        var newSequenceStart = itemSet.higherKey(fromElement);
        var newSequenceEnd = lastItem;
        setEnd(itemSet.lowerKey(fromElement));
        return new SequenceImpl<>(sourceTree, newSequenceStart, newSequenceEnd);
    }

    // This Sequence is ALWAYS before other Sequence
    void merge(SequenceImpl<Value_, Point_, Difference_> other) {
        lastItem = other.lastItem;
        invalidate();
    }

    @Override
    public String toString() {
        return getItems().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", ", "Sequence [", "]"));
    }
}
