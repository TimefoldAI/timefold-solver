package ai.timefold.solver.core.impl.phase.scope;

import java.util.ArrayList;
import java.util.Objects;

/**
 * Identifies which move thread, which phase, step and move/move tree the solver is currently executing.
 *
 * @param moveThreadIndex the index of the move thread, or -1 if moveThreadCount = NONE.
 * @param phaseIndex the index of the phase.
 * @param stepIndex the index of the step.
 * @param moveIndex the index of the move, or -1 if exhaustive search.
 * @param treeId the id of the move tree if exhaustive search, null otherwise.
 */
public record SolverLifecyclePoint(int moveThreadIndex, int phaseIndex, int stepIndex, int moveIndex, String treeId) {

    public static SolverLifecyclePoint of(AbstractMoveScope<?> moveScope) { // General purpose.
        var stepScope = moveScope.getStepScope();
        return new SolverLifecyclePoint(-1, stepScope.getPhaseScope().getPhaseIndex(), stepScope.getStepIndex(),
                moveScope.moveIndex, null);
    }

    public static SolverLifecyclePoint of(AbstractStepScope<?> stepScope, String treeId) { // Used in exhaustive search.
        return new SolverLifecyclePoint(-1, stepScope.getPhaseScope().getPhaseIndex(), stepScope.getStepIndex(), -1,
                Objects.requireNonNull(treeId));
    }

    public static SolverLifecyclePoint of(int moveThreadIndex, int phaseIndex, int stepIndex, int moveIndex) {
        // Used in multi-threaded solving.
        return new SolverLifecyclePoint(moveThreadIndex, phaseIndex, stepIndex, moveIndex, null);
    }

    @Override
    public String toString() {
        var stringList = new ArrayList<String>();
        stringList.add("Phase index (%d)".formatted(phaseIndex));
        if (moveThreadIndex >= 0) {
            stringList.add("move thread index (%d)".formatted(moveThreadIndex));
        }
        stringList.add("step index (%d)".formatted(stepIndex));
        if (moveIndex == -1) {
            stringList.add("move tree id (%s)".formatted(treeId));
        } else {
            stringList.add("move index (%d)".formatted(moveIndex));
        }
        return String.join(", ", stringList) + ".";
    }

}
