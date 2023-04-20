package ai.timefold.solver.examples.machinereassignment.domain.solver;

import java.util.Comparator;

import ai.timefold.solver.examples.machinereassignment.domain.MrProcessAssignment;

public class MrProcessAssignmentDifficultyComparator implements Comparator<MrProcessAssignment> {

    private static final Comparator<MrProcessAssignment> COMPARATOR = Comparator
            .comparingInt((MrProcessAssignment assignment) -> assignment.getProcess().getUsageMultiplicand())
            .thenComparingLong(MrProcessAssignment::getId);

    @Override
    public int compare(MrProcessAssignment a, MrProcessAssignment b) {
        return COMPARATOR.compare(a, b);
    }
}
