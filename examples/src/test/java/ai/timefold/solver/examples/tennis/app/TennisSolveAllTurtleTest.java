package ai.timefold.solver.examples.tennis.app;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.UnsolvedDirSolveAllTurtleTest;
import ai.timefold.solver.examples.tennis.domain.TennisSolution;

class TennisSolveAllTurtleTest extends UnsolvedDirSolveAllTurtleTest<TennisSolution> {

    @Override
    protected CommonApp<TennisSolution> createCommonApp() {
        return new TennisApp();
    }
}
