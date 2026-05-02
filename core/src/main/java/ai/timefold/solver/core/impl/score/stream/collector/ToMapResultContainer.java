package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.Map;

sealed interface ToMapResultContainer<Key_, Value_, ResultValue_, Result_ extends Map<Key_, ResultValue_>>
        permits ToMultiMapResultContainer, ToSimpleMapResultContainer {

    void add(Key_ key, Value_ value);

    void update(Key_ oldKey, Value_ oldValue, Key_ newKey, Value_ newValue);

    void remove(Key_ key, Value_ value);

    Result_ getResult();

}
