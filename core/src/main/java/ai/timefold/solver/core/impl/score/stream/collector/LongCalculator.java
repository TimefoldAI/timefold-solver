package ai.timefold.solver.core.impl.score.stream.collector;

public sealed interface LongCalculator permits LongAverageCalculator, LongSumCalculator {
    void insert(long input);

    void update(long input);

    void retract();
}
