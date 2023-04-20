package ai.timefold.solver.examples.machinereassignment.app;

import ai.timefold.solver.examples.common.app.CommonApp;
import ai.timefold.solver.examples.common.app.UnsolvedDirSolveAllTurtleTest;
import ai.timefold.solver.examples.machinereassignment.domain.MachineReassignment;

class MachineReassignmentSolveAllTurtleTest extends UnsolvedDirSolveAllTurtleTest<MachineReassignment> {

    @Override
    protected CommonApp<MachineReassignment> createCommonApp() {
        return new MachineReassignmentApp();
    }
}
