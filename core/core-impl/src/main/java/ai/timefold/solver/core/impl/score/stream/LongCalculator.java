package ai.timefold.solver.core.impl.score.stream;

public interface LongCalculator<Output_> {
    void insert(long input);

    void retract(long input);

    Output_ result();
}
