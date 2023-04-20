package ai.timefold.solver.examples.machinereassignment.solver.solution.initializer;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.phase.custom.CustomPhaseCommand;
import ai.timefold.solver.examples.machinereassignment.domain.MachineReassignment;
import ai.timefold.solver.examples.machinereassignment.domain.MrMachine;
import ai.timefold.solver.examples.machinereassignment.domain.MrProcessAssignment;

public class ToOriginalMachineSolutionInitializer implements CustomPhaseCommand<MachineReassignment> {

    @Override
    public void changeWorkingSolution(ScoreDirector<MachineReassignment> scoreDirector) {
        MachineReassignment machineReassignment = scoreDirector.getWorkingSolution();
        initializeProcessAssignmentList(scoreDirector, machineReassignment);
    }

    private void initializeProcessAssignmentList(ScoreDirector<MachineReassignment> scoreDirector,
            MachineReassignment machineReassignment) {
        for (MrProcessAssignment processAssignment : machineReassignment.getProcessAssignmentList()) {
            MrMachine originalMachine = processAssignment.getOriginalMachine();
            MrMachine machine = originalMachine == null ? machineReassignment.getMachineList().get(0) : originalMachine;
            scoreDirector.beforeVariableChanged(processAssignment, "machine");
            processAssignment.setMachine(machine);
            scoreDirector.afterVariableChanged(processAssignment, "machine");
            scoreDirector.triggerVariableListeners();
        }
    }

}
