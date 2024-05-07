package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Function;

import ai.timefold.solver.core.impl.util.ConstantLambdaUtils;
import ai.timefold.solver.core.impl.util.MutableInt;

public final class MinMaxUndoableActionable<Result_, Property_> implements UndoableActionable<Result_, Result_> {
    private final boolean isMin;
    private final NavigableMap<Property_, Map<Result_, MutableInt>> propertyToItemCountMap;
    private final Function<? super Result_, ? extends Property_> propertyFunction;

    private MinMaxUndoableActionable(boolean isMin,
            NavigableMap<Property_, Map<Result_, MutableInt>> propertyToItemCountMap,
            Function<? super Result_, ? extends Property_> propertyFunction) {
        this.isMin = isMin;
        this.propertyToItemCountMap = propertyToItemCountMap;
        this.propertyFunction = propertyFunction;
    }

    public static <Result extends Comparable<? super Result>> MinMaxUndoableActionable<Result, Result> minCalculator() {
        return new MinMaxUndoableActionable<>(true, new TreeMap<>(), ConstantLambdaUtils.identity());
    }

    public static <Result extends Comparable<? super Result>> MinMaxUndoableActionable<Result, Result> maxCalculator() {
        return new MinMaxUndoableActionable<>(false, new TreeMap<>(), ConstantLambdaUtils.identity());
    }

    public static <Result> MinMaxUndoableActionable<Result, Result> minCalculator(Comparator<? super Result> comparator) {
        return new MinMaxUndoableActionable<>(true, new TreeMap<>(comparator), ConstantLambdaUtils.identity());
    }

    public static <Result> MinMaxUndoableActionable<Result, Result> maxCalculator(Comparator<? super Result> comparator) {
        return new MinMaxUndoableActionable<>(false, new TreeMap<>(comparator), ConstantLambdaUtils.identity());
    }

    public static <Result, Property extends Comparable<? super Property>> MinMaxUndoableActionable<Result, Property>
            minCalculator(
                    Function<? super Result, ? extends Property> propertyMapper) {
        return new MinMaxUndoableActionable<>(true, new TreeMap<>(), propertyMapper);
    }

    public static <Result, Property extends Comparable<? super Property>> MinMaxUndoableActionable<Result, Property>
            maxCalculator(
                    Function<? super Result, ? extends Property> propertyMapper) {
        return new MinMaxUndoableActionable<>(false, new TreeMap<>(), propertyMapper);
    }

    @Override
    public Runnable insert(Result_ item) {
        Property_ key = propertyFunction.apply(item);
        Map<Result_, MutableInt> itemCountMap = propertyToItemCountMap.computeIfAbsent(key, ignored -> new LinkedHashMap<>());
        MutableInt count = itemCountMap.computeIfAbsent(item, ignored -> new MutableInt());
        count.increment();

        return () -> {
            if (count.decrement() == 0) {
                itemCountMap.remove(item);
                if (itemCountMap.isEmpty()) {
                    propertyToItemCountMap.remove(key);
                }
            }
        };
    }

    @Override
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
