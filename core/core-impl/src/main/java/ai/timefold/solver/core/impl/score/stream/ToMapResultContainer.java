package ai.timefold.solver.core.impl.score.stream;

import java.util.Map;

public interface ToMapResultContainer<Key_, Value_, ResultValue_, Result_ extends Map<Key_, ResultValue_>> {

    void add(Key_ key, Value_ value);

    void remove(Key_ key, Value_ value);

    Result_ getResult();

}
