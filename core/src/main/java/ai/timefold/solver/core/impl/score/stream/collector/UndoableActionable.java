package ai.timefold.solver.core.impl.score.stream.collector;

public sealed interface UndoableActionable<Input_, Output_>
        permits CustomCollectionUndoableActionable, ListUndoableActionable, MapUndoableActionable, MinMaxUndoableActionable,
        SetUndoableActionable, SortedSetUndoableActionable {
    Runnable insert(Input_ input);

    Output_ result();
}
