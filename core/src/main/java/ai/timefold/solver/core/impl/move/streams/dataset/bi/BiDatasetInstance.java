package ai.timefold.solver.core.impl.move.streams.dataset.bi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.move.streams.dataset.common.AbstractDataset;
import ai.timefold.solver.core.impl.move.streams.dataset.common.AbstractDatasetInstance;
import ai.timefold.solver.core.impl.util.CollectionUtils;
import ai.timefold.solver.core.impl.util.ElementAwareList;
import ai.timefold.solver.core.impl.util.ElementAwareListEntry;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public final class BiDatasetInstance<Solution_, A, B>
        extends AbstractDatasetInstance<Solution_, BiTuple<A, B>> {

    private final Map<A, ElementAwareList<BiTuple<A, B>>> tupleListMap = new LinkedHashMap<>();

    public BiDatasetInstance(AbstractDataset<Solution_, BiTuple<A, B>> parent, int inputStoreIndex) {
        super(parent, inputStoreIndex);
    }

    @Override
    public void insert(BiTuple<A, B> tuple) {
        var tupleList = tupleListMap.computeIfAbsent(tuple.factA, key -> new ElementAwareList<>());
        var entry = tupleList.add(tuple);
        tuple.setStore(inputStoreIndex, entry);
    }

    @Override
    public void retract(BiTuple<A, B> tuple) {
        ElementAwareListEntry<BiTuple<A, B>> entry = tuple.removeStore(inputStoreIndex);
        // No fail fast if null because we don't track which tuples made it through the filter predicate(s)
        if (entry != null) {
            var tupleList = entry.getList();
            entry.remove();
            if (tupleList.size() == 0) {
                tupleListMap.remove(tuple.factA);
            }
        }
    }

    public Iterator<BiTuple<A, B>> iterator() {
        return new OriginalTupleMapIterator<>(tupleListMap);
    }

    public Iterator<BiTuple<A, B>> iterator(Random workingRandom) {
        return new RandomTupleMapIterator<>(tupleListMap, workingRandom);
    }

    @NullMarked
    private static final class OriginalTupleMapIterator<A, B> implements Iterator<BiTuple<A, B>> {

        private final Iterator<ElementAwareList<BiTuple<A, B>>> listIterator;
        private @Nullable Iterator<BiTuple<A, B>> currentIterator = null;

        public OriginalTupleMapIterator(Map<A, ElementAwareList<BiTuple<A, B>>> tupleListMap) {
            this.listIterator = tupleListMap.values().iterator();
        }

        @Override
        public boolean hasNext() {
            while ((currentIterator == null || !currentIterator.hasNext()) && listIterator.hasNext()) {
                currentIterator = listIterator.next().iterator();
            }
            return currentIterator != null && currentIterator.hasNext();
        }

        @Override
        public BiTuple<A, B> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return currentIterator.next();
        }
    }

    @NullMarked
    private static final class RandomTupleMapIterator<A, B> implements Iterator<BiTuple<A, B>> {

        private final Random workingRandom;
        private final Map<A, ElementAwareList<BiTuple<A, B>>> allTuplesMap;
        private final List<A> keyList;
        private final Map<A, List<BiTuple<A, B>>> unvisitedTuplesMap;
        private @Nullable BiTuple<A, B> selection;

        public RandomTupleMapIterator(Map<A, ElementAwareList<BiTuple<A, B>>> allTuplesMap, Random workingRandom) {
            this.workingRandom = workingRandom;
            this.allTuplesMap = allTuplesMap;
            this.keyList = new ArrayList<>(allTuplesMap.keySet());
            this.unvisitedTuplesMap = CollectionUtils.newHashMap(allTuplesMap.size());
        }

        @Override
        public boolean hasNext() {
            if (selection != null) {
                // If we already have a selection, return true.
                return true;
            } else if (keyList.isEmpty()) {
                // All keys were removed. This means all tuples from all lists were removed.
                return false;
            }
            // At least one key is available, meaning at least one list contains at least one unvisited tuple.
            var randomKeyIndex = workingRandom.nextInt(keyList.size());
            var randomKey = keyList.get(randomKeyIndex);
            var randomAccessList = unvisitedTuplesMap.get(randomKey);
            if (randomAccessList == null) {
                // The key exists, but the random access list is empty.
                // This means that the list needs to be filled from the original tuple list,
                // as all its items are now available for random access.
                // This is done on-demand to avoid unnecessary computation and memory use.
                var tupleList = allTuplesMap.get(randomKey);
                randomAccessList = new ArrayList<>(tupleList.size());
                tupleList.forEach(randomAccessList::add);
                unvisitedTuplesMap.put(randomKey, randomAccessList);
            }
            selection = randomAccessList.remove(workingRandom.nextInt(randomAccessList.size()));
            if (randomAccessList.isEmpty()) {
                // The random access list is now empty, so we remove the key from the unvisited map.
                // This will make the key unavailable for future iterations.
                unvisitedTuplesMap.remove(randomKey);
                keyList.remove(randomKeyIndex);
            }
            return true;
        }

        @Override
        public BiTuple<A, B> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            var result = selection;
            selection = null;
            return result;
        }
    }

}
