package ai.timefold.solver.core.impl.score.stream.collector;

public sealed interface UndoableActionable<Input_>
        permits CustomCollectionUndoableActionable, ListUndoableActionable, MapUndoableActionable, MinMaxUndoableActionable,
        SetUndoableActionable, SortedSetUndoableActionable {
    void insert(Input_ input);

    default void update(Input_ input) {
        retract();
        insert(input);
    }

    void retract();
}
