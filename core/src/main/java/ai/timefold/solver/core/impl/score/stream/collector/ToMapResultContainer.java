package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.Map;

public sealed interface ToMapResultContainer<Key_, Value_, ResultValue_, Result_ extends Map<Key_, ResultValue_>>
        permits ToMultiMapResultContainer, ToSimpleMapResultContainer {

    void add(Key_ key, Value_ value);

    void remove(Key_ key, Value_ value);

    Result_ getResult();

}
