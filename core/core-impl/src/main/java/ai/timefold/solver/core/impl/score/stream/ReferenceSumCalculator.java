package ai.timefold.solver.core.impl.score.stream;

import java.util.function.BinaryOperator;

public final class ReferenceSumCalculator<Result_> implements ObjectCalculator<Result_, Result_> {
    private Result_ current;
    private final BinaryOperator<Result_> adder;
    private final BinaryOperator<Result_> subtractor;

    public ReferenceSumCalculator(Result_ current, BinaryOperator<Result_> adder, BinaryOperator<Result_> subtractor) {
        this.current = current;
        this.adder = adder;
        this.subtractor = subtractor;
    }

    @Override
    public void insert(Result_ input) {
        current = adder.apply(current, input);
    }

    @Override
    public void retract(Result_ input) {
        current = subtractor.apply(current, input);
    }

    @Override
    public Result_ result() {
        return current;
    }
}
