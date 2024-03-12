package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.Objects;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.score.stream.common.SequenceChain;

public final class SequenceCalculator<Result_>
        implements ObjectCalculator<Result_, SequenceChain<Result_, Integer>> {

    private final ConsecutiveSetTree<Result_, Integer, Integer> context = new ConsecutiveSetTree<>(
            (Integer a, Integer b) -> b - a,
            Integer::sum, 1, 0);

    private final ToIntFunction<Result_> indexMap;

    public SequenceCalculator(ToIntFunction<Result_> indexMap) {
        this.indexMap = Objects.requireNonNull(indexMap);
    }

    @Override
    public void insert(Result_ result) {
        var value = indexMap.applyAsInt(result);
        context.add(result, value);
    }

    @Override
    public void retract(Result_ result) {
        context.remove(result);
    }

    @Override
    public ConsecutiveSetTree<Result_, Integer, Integer> result() {
        return context;
    }

}
