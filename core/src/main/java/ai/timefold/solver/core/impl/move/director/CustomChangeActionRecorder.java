package ai.timefold.solver.core.impl.move.director;

interface CustomChangeActionRecorder<Solution_> {

    void recordCustomAction(CustomChangeAction<Solution_> customChangeAction);
}
