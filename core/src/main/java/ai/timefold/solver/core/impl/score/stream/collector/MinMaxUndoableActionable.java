package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;
import ai.timefold.solver.core.impl.util.MutableInt;

public final class MinMaxUndoableActionable<Result_, Property_> implements UndoableActionable<Result_> {
    public static final class State<Result_, Property_> {
        final boolean isMin;
        final NavigableMap<Property_, Map<Result_, MutableInt>> propertyToItemCountMap;
        final Function<? super Result_, ? extends Property_> propertyFunction;

        State(boolean isMin, NavigableMap<Property_, Map<Result_, MutableInt>> propertyToItemCountMap,
                Function<? super Result_, ? extends Property_> propertyFunction) {
            this.isMin = isMin;
            this.propertyToItemCountMap = propertyToItemCountMap;
            this.propertyFunction = propertyFunction;
        }

        public Result_ result() {
            if (propertyToItemCountMap.isEmpty()) {
                return null;
            }
            return isMin ? getFirstKey(propertyToItemCountMap.firstEntry().getValue())
                    : getFirstKey(propertyToItemCountMap.lastEntry().getValue());
        }

        private static <Key_> Key_ getFirstKey(Map<Key_, ?> map) {
            return map.keySet().iterator().next();
        }
    }

    public static <Result extends Comparable<? super Result>> State<Result, Result> minState() {
        return new State<>(true, new TreeMap<>(), ConstantLambdaUtils.identity());
    }

    public static <Result extends Comparable<? super Result>> State<Result, Result> maxState() {
        return new State<>(false, new TreeMap<>(), ConstantLambdaUtils.identity());
    }

    public static <Result> State<Result, Result> minState(Comparator<? super Result> comparator) {
        return new State<>(true, new TreeMap<>(comparator), ConstantLambdaUtils.identity());
    }

    public static <Result> State<Result, Result> maxState(Comparator<? super Result> comparator) {
        return new State<>(false, new TreeMap<>(comparator), ConstantLambdaUtils.identity());
    }

    public static <Result, Property extends Comparable<? super Property>> State<Result, Property> minState(
            Function<? super Result, ? extends Property> propertyMapper) {
        return new State<>(true, new TreeMap<>(), propertyMapper);
    }

    public static <Result, Property extends Comparable<? super Property>> State<Result, Property> maxState(
            Function<? super Result, ? extends Property> propertyMapper) {
        return new State<>(false, new TreeMap<>(), propertyMapper);
    }

    private final State<Result_, Property_> state;
    private Result_ cachedItem;
    private Property_ cachedKey;
    private Map<Result_, MutableInt> cachedInnerMap;
    private MutableInt cachedCount;

    public MinMaxUndoableActionable(State<Result_, Property_> state) {
        this.state = state;
    }

    @Override
    public void insert(Result_ item) {
        cachedItem = item;
        cachedKey = state.propertyFunction.apply(item);
        cachedInnerMap = state.propertyToItemCountMap.computeIfAbsent(cachedKey, ignored -> new LinkedHashMap<>());
        cachedCount = cachedInnerMap.computeIfAbsent(item, ignored -> new MutableInt());
        cachedCount.increment();
    }

    @Override
    public void update(Result_ item) {
        var newKey = state.propertyFunction.apply(item);
        if (Objects.equals(cachedKey, newKey)) {
            return;
        }
        retract();
        cachedItem = item;
        cachedKey = newKey;
        cachedInnerMap = state.propertyToItemCountMap.computeIfAbsent(newKey, ignored -> new LinkedHashMap<>());
        cachedCount = cachedInnerMap.computeIfAbsent(item, ignored -> new MutableInt());
        cachedCount.increment();
    }

    @Override
    public void retract() {
        if (cachedCount.decrement() == 0) {
            cachedInnerMap.remove(cachedItem);
            if (cachedInnerMap.isEmpty()) {
                state.propertyToItemCountMap.remove(cachedKey);
            }
        }
    }
}
