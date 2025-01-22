package ai.timefold.solver.core.impl.score.stream.collector;

public final class LongAverageCalculator implements LongCalculator<Double> {
    long count = 0;
    long sum = 0;

    @Override
    public void insert(long input) {
        count++;
        sum += input;
    }

    @Override
    public void retract(long input) {
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
