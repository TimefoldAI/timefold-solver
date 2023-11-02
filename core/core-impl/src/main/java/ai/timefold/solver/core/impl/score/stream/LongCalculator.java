package ai.timefold.solver.core.impl.score.stream;

public sealed interface LongCalculator<Output_> permits LongAverageCalculator, LongSumCalculator {
    void insert(long input);

    void retract(long input);

    Output_ result();
}
