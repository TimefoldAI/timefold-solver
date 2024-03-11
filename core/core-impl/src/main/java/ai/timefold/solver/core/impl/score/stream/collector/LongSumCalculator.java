package ai.timefold.solver.core.impl.score.stream.collector;

public final class LongSumCalculator implements LongCalculator<Long> {
    long sum = 0;

    @Override
    public void insert(long input) {
        sum += input;
    }

    @Override
    public void retract(long input) {
        sum -= input;
    }

    @Override
    public Long result() {
        return sum;
    }
}
