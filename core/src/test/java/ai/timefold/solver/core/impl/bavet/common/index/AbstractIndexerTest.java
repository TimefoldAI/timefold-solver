package ai.timefold.solver.core.impl.bavet.common.index;

import java.util.ArrayList;
import java.util.List;

abstract class AbstractIndexerTest {

    protected static <T> List<T> getTuples(Indexer<T> indexer, Object... objectProperties) {
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
