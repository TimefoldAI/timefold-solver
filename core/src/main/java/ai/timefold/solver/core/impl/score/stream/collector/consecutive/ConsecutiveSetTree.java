package ai.timefold.solver.core.impl.score.stream.collector.consecutive;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

import ai.timefold.solver.core.api.score.stream.common.Break;
import ai.timefold.solver.core.api.score.stream.common.Sequence;
import ai.timefold.solver.core.api.score.stream.common.SequenceChain;

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
    private final NavigableMap<ComparableValue<Value_, Point_>, BreakImpl<Value_, Point_, Difference_>> startItemToPreviousBreak =
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

    @Override
    public Collection<Sequence<Value_, Difference_>> getConsecutiveSequences() {
        return Collections.unmodifiableCollection(startItemToSequence.values());
    }

    @Override
    public Collection<Break<Value_, Difference_>> getBreaks() {
        return Collections.unmodifiableCollection(startItemToPreviousBreak.values());
    }

    @Override
    public Sequence<Value_, Difference_> getFirstSequence() {
        if (startItemToSequence.isEmpty()) {
            return null;
        }
        return startItemToSequence.firstEntry().getValue();
    }

    @Override
    public Sequence<Value_, Difference_> getLastSequence() {
        if (startItemToSequence.isEmpty()) {
            return null;
        }
        return startItemToSequence.lastEntry().getValue();
    }

    @Override
    public Break<Value_, Difference_> getFirstBreak() {
        if (startItemToSequence.size() <= 1) {
            return null;
        }
        return startItemToPreviousBreak.firstEntry().getValue();
    }

    @Override
    public Break<Value_, Difference_> getLastBreak() {
        if (startItemToSequence.size() <= 1) {
            return null;
        }
        return startItemToPreviousBreak.lastEntry().getValue();
    }

    public boolean add(Value_ value, Point_ valueIndex) {
        var valueCount = valueCountMap.get(value);
        if (valueCount != null) { // Item already in bag.
            var addingItem = valueCount.value;
            if (!Objects.equals(addingItem.index(), valueIndex)) {
                throw new IllegalStateException(
                        "Impossible state: the item (" + value + ") is already in the bag with a different index ("
                                + addingItem.index() + " vs " + valueIndex + ").\n" +
                                "Maybe the index map function is not deterministic?");
            }
            valueCount.count++;
            return true;
        }

        // Adding item to the bag.
        var addingItem = new ComparableValue<>(value, valueIndex);
        valueCountMap.put(value, new ValueCount<>(addingItem));
        itemMap.put(addingItem, addingItem.value());
        if (firstItem == null || addingItem.compareTo(firstItem) < 0) {
            firstItem = addingItem;
        }
        if (lastItem == null || addingItem.compareTo(lastItem) > 0) {
            lastItem = addingItem;
        }

        var firstBeforeItemEntry = startItemToSequence.floorEntry(addingItem);
        if (firstBeforeItemEntry != null) {
            var firstBeforeItem = firstBeforeItemEntry.getKey();
            var endOfBeforeSequenceItem = firstBeforeItemEntry.getValue().lastItem;
            var endOfBeforeSequenceIndex = endOfBeforeSequenceItem.index();
            if (isInNaturalOrderAndHashOrderIfEqual(valueIndex, value, endOfBeforeSequenceIndex,
                    endOfBeforeSequenceItem.value())) {
                // Item is already in the bag; do nothing
                return true;
            }
            // Item is outside the bag
            var firstAfterItem = startItemToSequence.higherKey(addingItem);
            if (firstAfterItem != null) {
                addBetweenItems(addingItem, firstBeforeItem, endOfBeforeSequenceItem, firstAfterItem);
            } else {
                var prevBag = startItemToSequence.get(firstBeforeItem);
                if (isFirstSuccessorOfSecond(addingItem, endOfBeforeSequenceItem)) {
                    // We need to extend the first bag
                    // No break since afterItem is null
                    prevBag.setEnd(addingItem);
                } else {
                    // Start a new bag of consecutive items
                    var newBag = new SequenceImpl<>(this, addingItem);
                    startItemToSequence.put(addingItem, newBag);
                    startItemToPreviousBreak.put(addingItem, new BreakImpl<>(newBag, prevBag));
                }
            }
        } else {
            // No items before it
            var firstAfterItem = startItemToSequence.higherKey(addingItem);
            if (firstAfterItem != null) {
                if (isFirstSuccessorOfSecond(firstAfterItem, addingItem)) {
                    // We need to move the after bag to use item as key
                    var afterBag = startItemToSequence.remove(firstAfterItem);
                    afterBag.setStart(addingItem);
                    // No break since this is the first sequence
                    startItemToSequence.put(addingItem, afterBag);
                } else {
                    // Start a new bag of consecutive items
                    var afterBag = startItemToSequence.get(firstAfterItem);
                    var newBag = new SequenceImpl<>(this, addingItem);
                    startItemToSequence.put(addingItem, newBag);
                    startItemToPreviousBreak.put(firstAfterItem, new BreakImpl<>(afterBag, newBag));
                }
            } else {
                // Start a new bag of consecutive items
                var newBag = new SequenceImpl<>(this, addingItem);
                startItemToSequence.put(addingItem, newBag);
                // Bag have no other items, so no break
            }
        }
        return true;
    }

    private static <T extends Comparable<T>, Value_> boolean isInNaturalOrderAndHashOrderIfEqual(T a, Value_ aItem, T b,
            Value_ bItem) {
        int difference = a.compareTo(b);
        if (difference != 0) {
            return difference < 0;
        }
        return System.identityHashCode(aItem) - System.identityHashCode(bItem) < 0;
    }

    private void addBetweenItems(ComparableValue<Value_, Point_> comparableItem,
            ComparableValue<Value_, Point_> firstBeforeItem, ComparableValue<Value_, Point_> endOfBeforeSequenceItem,
            ComparableValue<Value_, Point_> firstAfterItem) {
        if (isFirstSuccessorOfSecond(comparableItem, endOfBeforeSequenceItem)) {
            // We need to extend the first bag
            var prevBag = startItemToSequence.get(firstBeforeItem);
            if (isFirstSuccessorOfSecond(firstAfterItem, comparableItem)) {
                // We need to merge the two bags
                startItemToPreviousBreak.remove(firstAfterItem);
                var afterBag = startItemToSequence.remove(firstAfterItem);
                prevBag.merge(afterBag);
                var maybeNextBreak = startItemToPreviousBreak.higherEntry(firstAfterItem);
                if (maybeNextBreak != null) {
                    maybeNextBreak.getValue().setPreviousSequence(prevBag);
                }
            } else {
                prevBag.setEnd(comparableItem);
                startItemToPreviousBreak.get(firstAfterItem).updateLength();
            }
        } else {
            // Don't need to extend the first bag
            if (isFirstSuccessorOfSecond(firstAfterItem, comparableItem)) {
                // We need to move the after bag to use item as key
                var afterBag = startItemToSequence.remove(firstAfterItem);
                afterBag.setStart(comparableItem);
                startItemToSequence.put(comparableItem, afterBag);
                var prevBreak = startItemToPreviousBreak.remove(firstAfterItem);
                prevBreak.updateLength();
                startItemToPreviousBreak.put(comparableItem, prevBreak);
            } else {
                // Start a new bag of consecutive items
                var newBag = new SequenceImpl<>(this, comparableItem);
                startItemToSequence.put(comparableItem, newBag);
                startItemToPreviousBreak.get(firstAfterItem).setPreviousSequence(newBag);
                SequenceImpl<Value_, Point_, Difference_> previousSequence = startItemToSequence.get(firstBeforeItem);
                startItemToPreviousBreak.put(comparableItem,
                        new BreakImpl<>(newBag, previousSequence));
            }
        }
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
        boolean noMoreItems = itemMap.isEmpty();
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
            startItemToSequence.remove(firstBeforeItem);
            var removedBreak = startItemToPreviousBreak.remove(firstBeforeItem);
            var extendedBreakEntry = startItemToPreviousBreak.higherEntry(firstBeforeItem);
            if (extendedBreakEntry != null) {
                if (removedBreak != null) {
                    var extendedBreak = extendedBreakEntry.getValue();
                    extendedBreak.setPreviousSequence(removedBreak.previousSequence);
                } else {
                    startItemToPreviousBreak.remove(extendedBreakEntry.getKey());
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
            // Change start key to the item after this one
            bag.setStart(itemMap.higherKey(item));
            startItemToSequence.remove(sequenceStart);
            var extendedBreak = startItemToPreviousBreak.remove(sequenceStart);
            var bagFirstItem = bag.firstItem;
            startItemToSequence.put(bagFirstItem, bag);
            if (extendedBreak != null) {
                extendedBreak.updateLength();
                startItemToPreviousBreak.put(bagFirstItem, extendedBreak);
            }
            return;
        }
        if (item.equals(sequenceEnd)) {
            // Set end key to the item before this one
            bag.setEnd(itemMap.lowerKey(item));
            var extendedBreakEntry = startItemToPreviousBreak.higherEntry(item);
            if (extendedBreakEntry != null) {
                var extendedBreak = extendedBreakEntry.getValue();
                extendedBreak.updateLength();
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
        startItemToPreviousBreak.put(firstSplitItem, new BreakImpl<>(splitBag, bag));
        var maybeNextBreak = startItemToPreviousBreak.higherEntry(firstAfterItem);
        if (maybeNextBreak != null) {
            maybeNextBreak.getValue().setPreviousSequence(splitBag);
        }
    }

    Break<Value_, Difference_> getBreakBefore(ComparableValue<Value_, Point_> item) {
        return startItemToPreviousBreak.get(item);
    }

    Break<Value_, Difference_> getBreakAfter(ComparableValue<Value_, Point_> item) {
        var entry = startItemToPreviousBreak.higherEntry(item);
        if (entry != null) {
            return entry.getValue();
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

    private static final class ValueCount<Value_> {

        private final Value_ value;
        private int count;

        public ValueCount(Value_ value) {
            this.value = value;
            count = 1;
        }

    }

}
