package ai.timefold.solver.core.impl.score.stream.collector;

import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.ToIntFunction;

import ai.timefold.solver.core.api.score.stream.common.SequenceChain;
import ai.timefold.solver.core.impl.score.stream.collector.consecutive.ConsecutiveSetTree;

public final class SequenceCalculator<Result_>
        implements ObjectCalculator<Result_, SequenceChain<Result_, Integer>, Result_> {

    private final static BinaryOperator<Integer> DIFFERENCE = (a, b) -> b - a;

    private final ConsecutiveSetTree<Result_, Integer, Integer> context =
            new ConsecutiveSetTree<>(DIFFERENCE, Integer::sum, 1, 0);
    private final ToIntFunction<Result_> toIndexFunction;

    public SequenceCalculator(ToIntFunction<Result_> toIndexFunction) {
        this.toIndexFunction = Objects.requireNonNull(toIndexFunction);
    }

    @Override
    public Result_ insert(Result_ result) {
        var value = toIndexFunction.applyAsInt(result);
        context.add(result, value);
        return result;
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
