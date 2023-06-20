package ai.timefold.solver.examples.pas.app;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.UnsolvedDirSolveAllTurtleTest;
import ai.timefold.solver.examples.pas.domain.PatientAdmissionSchedule;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "ai.timefold.solver.examples.turtle", matches = "pas")
class PatientAdmissionScheduleSolveAllTurtleTest extends UnsolvedDirSolveAllTurtleTest<PatientAdmissionSchedule> {

    @Override
    protected CommonApp<PatientAdmissionSchedule> createCommonApp() {
        return new PatientAdmissionScheduleApp();
    }
}
