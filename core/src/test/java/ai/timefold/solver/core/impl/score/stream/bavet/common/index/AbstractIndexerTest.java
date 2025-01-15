package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.ArrayList;
import java.util.List;

abstract class AbstractIndexerTest {

    record Person(String gender, int age) {

    }

    protected <T> List<T> getTuples(Index<T> index, Object... objectProperties) {
        var properties = switch (objectProperties.length) {
            case 0 -> IndexKeys.none();
            case 1 -> IndexKeys.of(objectProperties[0]);
            default -> IndexKeys.ofMany(objectProperties);
        };
        var result = new ArrayList<T>();
        index.forEach(properties, result::add);
        return result;
    }

}
