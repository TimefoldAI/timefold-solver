package ai.timefold.solver.core.impl.score.stream;

public interface IntCalculator<Output_> {
    void insert(int input);

    void retract(int input);

    Output_ result();
}
