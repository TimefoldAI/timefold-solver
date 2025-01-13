package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.ArrayList;
import java.util.List;

abstract class AbstractIndexerTest {

    record Person(String gender, int age) {

    }

    protected <T> List<T> getTuples(Indexer<T> indexer, Object... objectProperties) {
        var properties = switch (objectProperties.length) {
            case 0 -> NoneIndexProperties.INSTANCE;
            case 1 -> new SingleIndexProperties<>(objectProperties[0]);
            default -> new ManyIndexProperties(objectProperties);
        };
        var result = new ArrayList<T>();
        indexer.forEach(properties, result::add);
        return result;
    }

}
