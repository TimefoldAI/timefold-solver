package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import org.assertj.core.api.AbstractIntegerAssert;
import org.assertj.core.api.ListAssert;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Deprecated
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

}
