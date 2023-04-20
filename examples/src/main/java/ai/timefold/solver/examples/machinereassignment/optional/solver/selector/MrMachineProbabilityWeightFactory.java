package ai.timefold.solver.examples.machinereassignment.optional.solver.selector;

import ai.timefold.solver.core.api.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionProbabilityWeightFactory;
import ai.timefold.solver.examples.machinereassignment.domain.MachineReassignment;
import ai.timefold.solver.examples.machinereassignment.domain.MrMachine;
import ai.timefold.solver.examples.machinereassignment.domain.MrProcess;
import ai.timefold.solver.examples.machinereassignment.domain.MrProcessAssignment;
import ai.timefold.solver.examples.machinereassignment.domain.MrResource;

public class MrMachineProbabilityWeightFactory
        implements SelectionProbabilityWeightFactory<MachineReassignment, MrProcessAssignment> {

    @Override
    public double createProbabilityWeight(ScoreDirector<MachineReassignment> scoreDirector,
            MrProcessAssignment processAssignment) {
        MachineReassignment machineReassignment = scoreDirector.getWorkingSolution();
        MrMachine machine = processAssignment.getMachine();
        // TODO reuse usage calculated by of the ScoreCalculator which is a delta
        long[] usage = new long[machineReassignment.getResourceList().size()];
        for (MrProcessAssignment someProcessAssignment : machineReassignment.getProcessAssignmentList()) {
            if (someProcessAssignment.getMachine() == machine) {
                MrProcess process = someProcessAssignment.getProcess();
                for (MrResource resource : machineReassignment.getResourceList()) {
                    usage[resource.getIndex()] += process.getUsage(resource);
                }
            }
        }
        double sum = 0.0;
        for (MrResource resource : machineReassignment.getResourceList()) {
            double available = (machine.getMachineCapacity(resource).getSafetyCapacity() - usage[resource.getIndex()]);
            sum += (available * available);
        }
        return sum + 1.0;
    }

}
