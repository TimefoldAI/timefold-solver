package ai.timefold.solver.examples.nurserostering.app;

import ai.timefold.solver.examples.common.TestSystemProperties;
import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.UnsolvedDirSolveAllTurtleTest;
import ai.timefold.solver.examples.nurserostering.domain.NurseRoster;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = TestSystemProperties.TURTLE_TEST_SELECTION, matches = "nurserostering")
class NurseRosteringSolveAllTurtleTest extends UnsolvedDirSolveAllTurtleTest<NurseRoster> {

    @Override
    protected CommonApp<NurseRoster> createCommonApp() {
        return new NurseRosteringApp();
    }

}
