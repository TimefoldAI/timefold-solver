package ai.timefold.solver.core.impl.score.stream.bavet.common.index;

import java.util.ArrayList;
import java.util.List;

abstract class AbstractIndexerTest {

    static final class Person {

        public final String gender;
        public final int age;

        public Person(String gender, int age) {
            this.gender = gender;
            this.age = age;
        }

    }

    protected <T> List<T> getTuples(Indexer<T> indexer, Object... objectProperties) {
        Object properties = switch (objectProperties.length) {
            case 0 -> NoneIndexProperties.INSTANCE;
            case 1 -> objectProperties[0];
            default -> new ManyIndexProperties(objectProperties);
        };
        List<T> result = new ArrayList<>();
        indexer.forEach(properties, result::add);
        return result;
    }

}
