package ai.timefold.solver.core.impl.score.stream;

public interface UndoableActionable<Input_, Output_> {
    Runnable insert(Input_ input);

    Output_ result();
}
