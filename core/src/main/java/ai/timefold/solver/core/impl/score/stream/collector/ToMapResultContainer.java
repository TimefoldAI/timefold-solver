package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.Map;

import org.jspecify.annotations.Nullable;

sealed interface ToMapResultContainer<Key_, Value_, ResultValue_, Result_ extends Map<Key_, ResultValue_>>
        permits ToMultiMapResultContainer, ToSimpleMapResultContainer {

    void add(Key_ key, Value_ value);

    void replaceWith(ToMapPerKeyCounter<Key_, Value_> counter, CountHolder<Value_> holder, Key_ newKey, Value_ newValue);

    void remove(ToMapPerKeyCounter<Key_, Value_> counter, CountHolder<Value_> holder);

    @Nullable
    ToMapPerKeyCounter<Key_, Value_> lastCounter();

    @Nullable
    CountHolder<Value_> lastHolder();

    Result_ getResult();

}
