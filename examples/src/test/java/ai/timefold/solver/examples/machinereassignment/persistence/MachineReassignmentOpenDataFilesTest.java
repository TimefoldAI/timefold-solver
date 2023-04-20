package ai.timefold.solver.examples.machinereassignment.persistence;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.persistence.OpenDataFilesTest;
import ai.timefold.solver.examples.machinereassignment.app.MachineReassignmentApp;
import ai.timefold.solver.examples.machinereassignment.domain.MachineReassignment;

class MachineReassignmentOpenDataFilesTest extends OpenDataFilesTest<MachineReassignment> {

    @Override
    protected CommonApp<MachineReassignment> createCommonApp() {
        return new MachineReassignmentApp();
    }
}
