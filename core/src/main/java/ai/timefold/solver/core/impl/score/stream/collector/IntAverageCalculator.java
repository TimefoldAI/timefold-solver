package ai.timefold.solver.core.impl.score.stream.collector;

public final class IntAverageCalculator implements IntCalculator<Double> {
    int count = 0;
    int sum = 0;

    @Override
    public void insert(int input) {
        count++;
        sum += input;
    }

    @Override
    public void retract(int input) {
        count--;
        sum -= input;
    }

    @Override
    public Double result() {
        if (count == 0) {
            return null;
        }
        return sum / (double) count;
    }
}
