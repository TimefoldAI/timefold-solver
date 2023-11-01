package ai.timefold.solver.core.impl.score.stream;

import java.util.function.BinaryOperator;

public final class ReferenceSumCalculator<Result> implements ObjectCalculator<Result, Result> {
    private Result current;
    private final BinaryOperator<Result> adder;
    private final BinaryOperator<Result> subtractor;

    public ReferenceSumCalculator(Result current, BinaryOperator<Result> adder, BinaryOperator<Result> subtractor) {
        this.current = current;
        this.adder = adder;
        this.subtractor = subtractor;
    }

    @Override
    public void insert(Result input) {
        current = adder.apply(current, input);
    }

    @Override
    public void retract(Result input) {
        current = subtractor.apply(current, input);
    }

    @Override
    public Result result() {
        return current;
    }
}
