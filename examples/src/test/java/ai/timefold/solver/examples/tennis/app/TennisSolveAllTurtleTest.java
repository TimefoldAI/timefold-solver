package ai.timefold.solver.examples.tennis.app;

import ai.timefold.solver.examples.common.TestSystemProperties;
import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.UnsolvedDirSolveAllTurtleTest;
import ai.timefold.solver.examples.tennis.domain.TennisSolution;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = TestSystemProperties.TURTLE_TEST_SELECTION, matches = "tennis|all")
class TennisSolveAllTurtleTest extends UnsolvedDirSolveAllTurtleTest<TennisSolution> {

    @Override
    protected CommonApp<TennisSolution> createCommonApp() {
        return new TennisApp();
    }
}
