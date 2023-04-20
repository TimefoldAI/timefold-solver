package ai.timefold.solver.examples.pas.persistence;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.persistence.OpenDataFilesTest;
import ai.timefold.solver.examples.pas.app.PatientAdmissionScheduleApp;
import ai.timefold.solver.examples.pas.domain.PatientAdmissionSchedule;

class PatientAdmissionScheduleOpenDataFilesTest extends OpenDataFilesTest<PatientAdmissionSchedule> {

    @Override
    protected CommonApp<PatientAdmissionSchedule> createCommonApp() {
        return new PatientAdmissionScheduleApp();
    }
}
