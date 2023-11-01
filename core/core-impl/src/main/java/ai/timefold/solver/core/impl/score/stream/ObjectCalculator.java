package ai.timefold.solver.core.impl.score.stream;

public interface ObjectCalculator<Input_, Output_> {
    void insert(Input_ input);

    void retract(Input_ input);

    Output_ result();
}
