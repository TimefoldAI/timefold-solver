package ai.timefold.solver.core.impl.score.stream.collector;

public final class IntSumCalculator implements IntCalculator<Integer> {
    int sum = 0;

    @Override
    public void insert(int input) {
        sum += input;
    }

    @Override
    public void retract(int input) {
        sum -= input;
    }

    @Override
    public Integer result() {
        return sum;
    }
}
