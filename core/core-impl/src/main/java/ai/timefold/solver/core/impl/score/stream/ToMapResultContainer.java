package ai.timefold.solver.core.impl.score.stream;

import java.util.Map;

public interface ToMapResultContainer<Key, Value, ResultValue, Result_ extends Map<Key, ResultValue>> {

    void add(Key key, Value value);

    void remove(Key key, Value value);

    Result_ getResult();

}
