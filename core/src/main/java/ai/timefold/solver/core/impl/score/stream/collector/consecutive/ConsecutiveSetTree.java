package ai.timefold.solver.core.impl.score.stream.collector.consecutive;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

import ai.timefold.solver.core.api.score.stream.common.Break;
import ai.timefold.solver.core.api.score.stream.common.Sequence;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.impl.util.MappingIterator;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A {@code ConsecutiveSetTree} determines what values are consecutive.
 * A sequence <i>x<sub>1</sub>,&nbsp;x<sub>2</sub>,&nbsp;x<sub>3</sub>,&nbsp;...,&nbsp;x<sub>n</sub></i>
 * is understood to be consecutive by <i>d</i> iff
 * <i>x<sub>2</sub> &minus; x<sub>1</sub> &le; d, x<sub>3</sub> &minus; x<sub>2</sub> &le; d, ..., x<sub>n</sub> &minus;
 * x<sub>n-1</sub> &le; d</i>.
 * This data structure can be thought as an interval tree that maps the point <i>p</i> to the interval <i>[p, p + d]</i>.
 *
 * @param <Value_> The type of value stored (examples: shifts)
 * @param <Point_> The type of the point (examples: int, LocalDateTime)
 * @param <Difference_> The type of the difference (examples: int, Duration)
 */
public final class ConsecutiveSetTree<Value_, Point_ extends Comparable<Point_>, Difference_ extends Comparable<Difference_>>
        implements SequenceChain<Value_, Difference_> {

    final BiFunction<Point_, Point_, Difference_> differenceFunction;
    final BiFunction<Point_, Point_, Difference_> sequenceLengthFunction;
    private final Difference_ maxDifference;
    private final Difference_ zeroDifference;
    private final Map<Value_, ValueCount<ComparableValue<Value_, Point_>>> valueCountMap = new HashMap<>();
    private final NavigableMap<ComparableValue<Value_, Point_>, Value_> itemMap = new TreeMap<>();
    private final NavigableMap<ComparableValue<Value_, Point_>, SequenceImpl<Value_, Point_, Difference_>> startItemToSequence =
            new TreeMap<>();

    private ComparableValue<Value_, Point_> firstItem;
    private ComparableValue<Value_, Point_> lastItem;

    public ConsecutiveSetTree(BiFunction<Point_, Point_, Difference_> differenceFunction,
            BinaryOperator<Difference_> sumFunction, Difference_ maxDifference,
            Difference_ zeroDifference) {
        this.differenceFunction = differenceFunction;
        this.sequenceLengthFunction = (first, last) -> sumFunction.apply(maxDifference, differenceFunction.apply(first, last));
        this.maxDifference = maxDifference;
        this.zeroDifference = zeroDifference;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public @NonNull Collection<Sequence<Value_, Difference_>> getConsecutiveSequences() {
        return (Collection) startItemToSequence.values();
    }

    @Override
    public @NonNull Collection<Break<Value_, Difference_>> getBreaks() {
        return new BreakCollection(); // Live view of the breaks.
    }

    @Override
    public @Nullable Sequence<Value_, Difference_> getFirstSequence() {
        if (startItemToSequence.isEmpty()) {
            return null;
        }
        return startItemToSequence.firstEntry().getValue();
    }

    @Override
    public @Nullable Sequence<Value_, Difference_> getLastSequence() {
        if (startItemToSequence.isEmpty()) {
            return null;
        }
        return startItemToSequence.lastEntry().getValue();
    }

    @Override
    public @Nullable Break<Value_, Difference_> getFirstBreak() {
        if (startItemToSequence.size() <= 1) {
            return null;
        }
        return startItemToSequence.higherEntry(startItemToSequence.firstKey()).getValue().previousBreak;
    }

    @Override
    public @Nullable Break<Value_, Difference_> getLastBreak() {
        if (startItemToSequence.size() <= 1) {
            return null;
        }
        return startItemToSequence.lastEntry().getValue().previousBreak;
    }

    public boolean add(Value_ value, Point_ valueIndex) {
        var valueCount = valueCountMap.get(value);
        if (valueCount != null) { // Item already in bag.
            addExistingItem(valueCount, valueIndex, value);
            return true;
        }

        var addingItem = addItemToBag(value, valueIndex);
        var firstBeforeItemEntry = startItemToSequence.floorEntry(addingItem);
        if (firstBeforeItemEntry != null) {
            addSubsequentItem(addingItem, firstBeforeItemEntry, valueIndex, value);
        } else { // No items before it
            addFirstItem(addingItem);
        }
        return true;
    }

    private ComparableValue<Value_, Point_> addItemToBag(Value_ value, Point_ valueIndex) {
        var addingItem = new ComparableValue<>(value, valueIndex);
        valueCountMap.put(value, new ValueCount<>(addingItem));
        itemMap.put(addingItem, addingItem.value());
        if (firstItem == null || addingItem.compareTo(firstItem) < 0) {
            firstItem = addingItem;
        }
        if (lastItem == null || addingItem.compareTo(lastItem) > 0) {
            lastItem = addingItem;
        }
        return addingItem;
    }

    private static <Value_, Point_ extends Comparable<Point_>> void
            addExistingItem(ValueCount<ComparableValue<Value_, Point_>> valueCount, Point_ valueIndex, Value_ value) {
        var addingItem = valueCount.value;
        if (!Objects.equals(addingItem.index(), valueIndex)) {
            throw new IllegalStateException("""
                    Impossible state: the item (%s) is already in the bag with a different index (%s vs %s)
                    Maybe the index map function is not deterministic?"""
                    .formatted(value, addingItem.index(), valueIndex));
        }
        valueCount.count++;
    }

    private void addSubsequentItem(ComparableValue<Value_, Point_> addingItem,
            Map.Entry<ComparableValue<Value_, Point_>, SequenceImpl<Value_, Point_, Difference_>> firstBeforeItemEntry,
            Point_ valueIndex, Value_ value) {
        var prevBag = firstBeforeItemEntry.getValue();
        var endOfBeforeSequenceItem = prevBag.lastItem;
        var endOfBeforeSequenceIndex = endOfBeforeSequenceItem.index();
        if (isInNaturalOrderAndHashOrderIfEqual(valueIndex, value, endOfBeforeSequenceIndex,
                endOfBeforeSequenceItem.value())) {
            // Item is already in the bag; do nothing
            return;
        }
        // Item is outside the bag
        var firstAfterEntry = startItemToSequence.higherEntry(addingItem);
        if (firstAfterEntry != null) {
            addBetweenItems(addingItem, prevBag, endOfBeforeSequenceItem, firstAfterEntry);
        } else {
            if (isFirstSuccessorOfSecond(addingItem, endOfBeforeSequenceItem)) {
                // We need to extend the first bag
                // No break since afterItem is null
                prevBag.setEnd(addingItem);
            } else {
                // Start a new bag of consecutive items
                var newBag = new SequenceImpl<>(this, addingItem);
                startItemToSequence.put(addingItem, newBag);
                newBag.previousBreak = new BreakImpl<>(newBag, prevBag);
            }
        }
    }

    private void addFirstItem(ComparableValue<Value_, Point_> addingItem) {
        var firstAfterEntry = startItemToSequence.higherEntry(addingItem);
        if (firstAfterEntry != null) {
            var firstAfterItem = firstAfterEntry.getKey();
            var afterBag = firstAfterEntry.getValue();
            if (isFirstSuccessorOfSecond(firstAfterItem, addingItem)) {
                // We need to move the after bag to use item as key
                startItemToSequence.remove(firstAfterItem);
                afterBag.setStart(addingItem);
                // No break since this is the first sequence
                startItemToSequence.put(addingItem, afterBag);
            } else {
                // Start a new bag of consecutive items; addingItem becomes the new first sequence
                var newBag = new SequenceImpl<>(this, addingItem);
                startItemToSequence.put(addingItem, newBag);
                afterBag.previousBreak = new BreakImpl<>(afterBag, newBag);
            }
        } else {
            // Start a new bag of consecutive items
            startItemToSequence.put(addingItem, new SequenceImpl<>(this, addingItem));
            // Bag have no other items, so no break
        }
    }

    private void addBetweenItems(ComparableValue<Value_, Point_> comparableItem,
            SequenceImpl<Value_, Point_, Difference_> prevBag,
            ComparableValue<Value_, Point_> endOfBeforeSequenceItem,
            Map.Entry<ComparableValue<Value_, Point_>, SequenceImpl<Value_, Point_, Difference_>> firstAfterEntry) {
        var firstAfterItem = firstAfterEntry.getKey();
        var afterBag = firstAfterEntry.getValue();
        if (isFirstSuccessorOfSecond(comparableItem, endOfBeforeSequenceItem)) {
            // We need to extend the first bag
            if (isFirstSuccessorOfSecond(firstAfterItem, comparableItem)) {
                // We need to merge the two bags; afterBag.previousBreak (break between prevBag and
                // afterBag) is discarded with afterBag. higherEntry is strict-greater, so it skips
                // firstAfterItem (just removed) and finds the sequence after afterBag.
                startItemToSequence.remove(firstAfterItem); // delete the afterBag mapping
                prevBag.merge(afterBag);
                var nextSeqEntry = startItemToSequence.higherEntry(firstAfterItem);
                if (nextSeqEntry != null) {
                    nextSeqEntry.getValue().previousBreak.setPreviousSequence(prevBag);
                }
            } else {
                prevBag.setEnd(comparableItem);
                afterBag.previousBreak.updateLength();
            }
        } else {
            // Don't need to extend the first bag
            if (isFirstSuccessorOfSecond(firstAfterItem, comparableItem)) {
                // We need to move the after bag to use item as key; previousBreak is a field on the
                // bag, so it follows the bag without re-keying — just update the length.
                startItemToSequence.remove(firstAfterItem); // delete the afterBag mapping; re-keyed below
                afterBag.setStart(comparableItem);
                startItemToSequence.put(comparableItem, afterBag);
                afterBag.previousBreak.updateLength();
            } else {
                // Start a new bag of consecutive items; split the existing break into two
                var newBag = new SequenceImpl<>(this, comparableItem);
                startItemToSequence.put(comparableItem, newBag);
                afterBag.previousBreak.setPreviousSequence(newBag);
                newBag.previousBreak = new BreakImpl<>(newBag, prevBag);
            }
        }
    }

    private static <T extends Comparable<T>, Value_> boolean isInNaturalOrderAndHashOrderIfEqual(T a, Value_ aItem, T b,
            Value_ bItem) {
        var difference = a.compareTo(b);
        if (difference != 0) {
            return difference < 0;
        }
        return System.identityHashCode(aItem) - System.identityHashCode(bItem) < 0;
    }

    public boolean remove(Value_ value) {
        var valueCount = valueCountMap.get(value);
        if (valueCount == null) { // Item not in bag.
            return false;
        }
        valueCount.count--;
        if (valueCount.count > 0) { // Item still in bag.
            return true;
        }

        // Item is removed from bag
        valueCountMap.remove(value);
        var removingItem = valueCount.value;
        itemMap.remove(removingItem);
        var noMoreItems = itemMap.isEmpty();
        if (removingItem.compareTo(firstItem) == 0) {
            firstItem = noMoreItems ? null : itemMap.firstEntry().getKey();
        }
        if (removingItem.compareTo(lastItem) == 0) {
            lastItem = noMoreItems ? null : itemMap.lastEntry().getKey();
        }

        var firstBeforeItemEntry = startItemToSequence.floorEntry(removingItem);
        var firstBeforeItem = firstBeforeItemEntry.getKey();
        var bag = firstBeforeItemEntry.getValue();
        if (bag.getFirstItem() == bag.getLastItem()) { // Bag is empty if first item = last item
            var removedBreak = bag.previousBreak; // null if this was the first sequence
            startItemToSequence.remove(firstBeforeItem);
            var nextSeqEntry = startItemToSequence.higherEntry(firstBeforeItem);
            if (nextSeqEntry != null) {
                var nextSeq = nextSeqEntry.getValue();
                if (removedBreak != null) {
                    // Middle sequence removed: stitch the next sequence's break to removed sequence's predecessor
                    nextSeq.previousBreak.setPreviousSequence(removedBreak.previousSequence);
                } else {
                    // First sequence removed: next sequence becomes first, so no break before it
                    nextSeq.previousBreak = null;
                }
            }
        } else { // Bag is not empty.
            removeItemFromBag(bag, removingItem, firstBeforeItem, bag.lastItem);
        }
        return true;
    }

    // Protected API
    private void removeItemFromBag(SequenceImpl<Value_, Point_, Difference_> bag, ComparableValue<Value_, Point_> item,
            ComparableValue<Value_, Point_> sequenceStart, ComparableValue<Value_, Point_> sequenceEnd) {
        if (item.equals(sequenceStart)) {
            // Change start key to the item after this one; previousBreak stays on the bag as a field —
            // no re-keying needed, just recompute the length for the new gap.
            bag.setStart(itemMap.higherKey(item));
            startItemToSequence.remove(sequenceStart);
            var bagFirstItem = bag.firstItem;
            startItemToSequence.put(bagFirstItem, bag);
            if (bag.previousBreak != null) {
                bag.previousBreak.updateLength();
            }
            return;
        }
        if (item.equals(sequenceEnd)) {
            // Set end key to the item before this one
            bag.setEnd(itemMap.lowerKey(item));
            var nextSeqEntry = startItemToSequence.higherEntry(sequenceEnd);
            if (nextSeqEntry != null) {
                nextSeqEntry.getValue().previousBreak.updateLength();
            }
            return;
        }

        var firstAfterItem = bag.getComparableItems().higherKey(item);
        var firstBeforeItem = bag.getComparableItems().lowerKey(item);
        if (isFirstSuccessorOfSecond(firstAfterItem, firstBeforeItem)) {
            // Bag is not split since the next two items are still close enough
            return;
        }

        // Need to split bag into two halves
        // Both halves are not empty as the item was not an endpoint
        // Additional, the breaks before and after the broken sequence
        // are not affected since an endpoint was not removed
        var splitBag = bag.split(item);
        var firstSplitItem = splitBag.firstItem;
        startItemToSequence.put(firstSplitItem, splitBag);
        splitBag.previousBreak = new BreakImpl<>(splitBag, bag);
        // higherEntry is strict-greater, so it skips firstSplitItem (just inserted) and finds the
        // sequence that was already after bag; its previousBreak now references splitBag, not bag.
        var nextSeqEntry = startItemToSequence.higherEntry(firstSplitItem);
        if (nextSeqEntry != null) {
            nextSeqEntry.getValue().previousBreak.setPreviousSequence(splitBag);
        }
    }

    Break<Value_, Difference_> getBreakAfter(ComparableValue<Value_, Point_> item) {
        var entry = startItemToSequence.higherEntry(item);
        if (entry != null) {
            return entry.getValue().previousBreak;
        }
        return null;
    }

    NavigableMap<ComparableValue<Value_, Point_>, Value_> getComparableItems(ComparableValue<Value_, Point_> firstKey,
            ComparableValue<Value_, Point_> lastKey) {
        return itemMap.subMap(firstKey, true, lastKey, true);
    }

    ComparableValue<Value_, Point_> getFirstItem() {
        return firstItem;
    }

    ComparableValue<Value_, Point_> getLastItem() {
        return lastItem;
    }

    private boolean isFirstSuccessorOfSecond(ComparableValue<Value_, Point_> first, ComparableValue<Value_, Point_> second) {
        var difference = differenceFunction.apply(second.index(), first.index());
        return isInNaturalOrderAndHashOrderIfEqual(zeroDifference, second.value(), difference, first.value()) &&
                difference.compareTo(maxDifference) <= 0;
    }

    @Override
    public String toString() {
        return "Sequences {" +
                "sequenceList=" + getConsecutiveSequences() +
                ", breakList=" + getBreaks() +
                '}';
    }

    @NullMarked
    private final class BreakCollection
            extends AbstractCollection<Break<Value_, Difference_>> {

        @Override
        public Iterator<Break<Value_, Difference_>> iterator() {
            var sequences = startItemToSequence.values();
            if (sequences.size() <= 1) {
                return Collections.emptyIterator();
            }
            var iterator = sequences.iterator();
            iterator.next(); // skip first sequence — it has no previousBreak
            return new MappingIterator<>(iterator, i -> i.previousBreak);
        }

        @Override
        public int size() {
            return Math.max(0, startItemToSequence.size() - 1);
        }
    }

    @NullMarked
    private static final class ValueCount<Value_> {

        private final Value_ value;
        private int count;

        public ValueCount(Value_ value) {
            this.value = value;
            count = 1;
        }

        @Override
        public String toString() {
            return "ValueCount{" +
                    "value=" + value +
                    ", count=" + count +
                    '}';
        }
    }

}
