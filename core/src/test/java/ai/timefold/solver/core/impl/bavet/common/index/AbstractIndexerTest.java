package ai.timefold.solver.core.impl.bavet.common.index;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.StreamSupport;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;

import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.ListAssert;

abstract class AbstractIndexerTest {

    private static final AtomicLong tupleId = new AtomicLong();

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

    static Iterable<Object> randomIterableForQuery(Indexer<Object> indexer, String key) {
        return randomIterableForQuery(indexer, 0, key);
    }

    private static Iterable<Object> randomIterableForQuery(Indexer<Object> indexer, long seed, String key) {
        return () -> {
            var random = new Random(seed);
            var delegate = indexer.randomIterator(key, random);
            return new Iterator<>() {

                @Override
                public boolean hasNext() {
                    return delegate.hasNext();
                }

                @Override
                public Object next() {
                    var out = delegate.next();
                    delegate.remove();
                    return out;
                }
            };
        };
    }

    static List<Object> randomListForQuery(Indexer<Object> indexer, long seed, String key) {
        var iterable = randomIterableForQuery(indexer, seed, key);
        return StreamSupport.stream(iterable.spliterator(), false)
                .toList();
    }

    static Iterable<Object> randomIterableForCollectionQuery(Indexer<Object> indexer, String... keys) {
        return randomIterableForCollectionQuery(indexer, 0, keys);
    }

    private static Iterable<Object> randomIterableForCollectionQuery(Indexer<Object> indexer, long seed, String... keys) {
        return () -> {
            var random = new Random(seed);
            var delegate = indexer.randomIterator(List.of(keys), random);
            return new Iterator<>() {

                @Override
                public boolean hasNext() {
                    return delegate.hasNext();
                }

                @Override
                public Object next() {
                    var out = delegate.next();
                    delegate.remove();
                    return out;
                }
            };
        };
    }

    static List<Object> randomListForCollectionQuery(Indexer<Object> indexer, long seed, String... keys) {
        var iterable = randomIterableForCollectionQuery(indexer, seed, keys);
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
}
