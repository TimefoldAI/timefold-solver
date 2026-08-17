package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;
import java.util.stream.StreamSupport;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.util.ElementAwareArrayList;

import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.ListAssert;

abstract class AbstractIndexerTest {

    private static final AtomicLong tupleId = new AtomicLong();

    static <T> ElementAwareArrayList<T> toEntries(List<T> elements) {
        var list = new ElementAwareArrayList<T>();
        list.addAll(elements);
        return list;
    }

    protected static UniTuple<String> putTuple(Indexer<Object> indexer, Object... keys) {
        var tuple = UniTuple.of("Tuple " + tupleId.getAndIncrement(), 0);
        indexer.put(CompositeKey.ofMany(keys), tuple);
        return tuple;
    }

    protected static AbstractIntegerAssert<?> assertSize(Indexer<Object> indexer, Object... keys) {
        return assertThat(indexer.size(CompositeKey.ofMany(keys)));
    }

    protected static ListAssert<Object> assertForEach(Indexer<Object> indexer, Object... keys) {
        var properties = switch (keys.length) {
            case 0 -> CompositeKey.none();
            case 1 -> CompositeKey.of(keys[0]);
            default -> CompositeKey.ofMany(keys);
        };
        var result = new ArrayList<>();
        indexer.forEach(properties, result::add);
        return assertThat(result);
    }

    protected static <T> List<T> forEachToTuples(Indexer<T> indexer, Object... objectProperties) {
        var properties = switch (objectProperties.length) {
            case 0 -> CompositeKey.none();
            case 1 -> CompositeKey.of(objectProperties[0]);
            default -> CompositeKey.ofMany(objectProperties);
        };
        var result = new ArrayList<T>();
        indexer.forEach(properties, result::add);
        return result;
    }

    static Iterable<Object> uniqueRandomIterableForQuery(Indexer<Object> indexer, String key) {
        return uniqueRandomIterableForQuery(indexer, 0, key);
    }

    private static Iterable<Object> uniqueRandomIterableForQuery(Indexer<Object> indexer, long seed, String key) {
        return () -> indexer.uniqueRandomIterator(key, new Random(seed));
    }

    static List<Object> uniqueRandomListForQuery(Indexer<Object> indexer, long seed, String key) {
        var iterable = uniqueRandomIterableForQuery(indexer, seed, key);
        return StreamSupport.stream(iterable.spliterator(), false)
                .toList();
    }

    static Iterable<Object> uniqueRandomIterableForCollectionQuery(Indexer<Object> indexer, String... keys) {
        return uniqueRandomIterableForCollectionQuery(indexer, 0, keys);
    }

    private static Iterable<Object> uniqueRandomIterableForCollectionQuery(Indexer<Object> indexer, long seed,
            String... keys) {
        return () -> indexer.uniqueRandomIterator(List.of(keys), new Random(seed));
    }

    static List<Object> uniqueRandomListForCollectionQuery(Indexer<Object> indexer, long seed, String... keys) {
        var iterable = uniqueRandomIterableForCollectionQuery(indexer, seed, keys);
        return StreamSupport.stream(iterable.spliterator(), false)
                .toList();
    }

    static UniTuple<String> putContainingIndexer(Indexer<Object> indexer, List<String> keys) {
        var tuple = UniTuple.of("Tuple " + tupleId.getAndIncrement(), 0);
        indexer.put(keys, tuple);
        return tuple;
    }

    static UniTuple<String> putContainedInIndexer(Indexer<Object> indexer, String key) {
        var tuple = UniTuple.of("Tuple " + tupleId.getAndIncrement(), 0);
        indexer.put(key, tuple);
        return tuple;
    }

    /**
     * Asserts that {@link Indexer#uniqueRandomIterator(Object, RandomGenerator)} drains exactly the same elements
     * as {@link Indexer#forEach(Object, Consumer)}, without caller cooperation, and then ends.
     * Repeats across several seeds, since bucket sampling is seed-dependent.
     */
    static <T> void assertUniqueRandomDrainMatchesForEach(Indexer<T> indexer, Object queryCompositeKey) {
        var expectedList = new ArrayList<T>();
        indexer.forEach(queryCompositeKey, expectedList::add);
        for (var seed = 0; seed < 10; seed++) {
            var iterator = indexer.uniqueRandomIterator(queryCompositeKey, new Random(seed));
            var drainedList = new ArrayList<T>();
            while (iterator.hasNext()) {
                drainedList.add(iterator.next());
            }
            assertThat(drainedList).containsExactlyInAnyOrderElementsOf(expectedList);
            assertThat(iterator).isExhausted();
            assertThatExceptionOfType(NoSuchElementException.class)
                    .isThrownBy(iterator::next);
        }
    }

    /**
     * Asserts that {@link Indexer#randomIterator(Object, RandomGenerator)} never ends,
     * every draw is a match for the query,
     * and every match is drawn at least once.
     * The last part is what catches a walk that silently drops a bucket:
     * a draw set that omits a whole matching bucket is still a subset of {@code expectedList},
     * so membership alone would not notice it.
     * Seed is fixed, so this is deterministic and cannot flake;
     * if a caller's {@code drawCount} does not reach every match,
     * raise it there rather than here.
     */
    static <T> void assertRepeatingRandomNeverEnds(Indexer<T> indexer, Object queryCompositeKey, int drawCount) {
        var expectedList = new ArrayList<T>();
        indexer.forEach(queryCompositeKey, expectedList::add);
        var iterator = indexer.randomIterator(queryCompositeKey, new Random(0));
        var drawnSet = new HashSet<T>();
        for (var i = 0; i < drawCount; i++) {
            assertThat(iterator.hasNext()).isTrue();
            var drawn = iterator.next();
            assertThat(drawn).isIn(expectedList);
            drawnSet.add(drawn);
        }
        assertThat(drawnSet).containsExactlyInAnyOrderElementsOf(expectedList);
    }
}
