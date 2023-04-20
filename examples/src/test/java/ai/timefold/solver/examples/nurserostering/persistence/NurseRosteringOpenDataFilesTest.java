package ai.timefold.solver.examples.nurserostering.persistence;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.persistence.OpenDataFilesTest;
import ai.timefold.solver.examples.nurserostering.app.NurseRosteringApp;
import ai.timefold.solver.examples.nurserostering.domain.NurseRoster;

class NurseRosteringOpenDataFilesTest extends OpenDataFilesTest<NurseRoster> {

    @Override
    protected CommonApp<NurseRoster> createCommonApp() {
        return new NurseRosteringApp();
    }
}
