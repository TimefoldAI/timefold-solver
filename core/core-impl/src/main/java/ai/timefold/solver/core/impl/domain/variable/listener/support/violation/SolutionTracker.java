package ai.timefold.solver.core.impl.domain.variable.listener.support.violation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.supply.SupplyManager;

public final class SolutionTracker<Solution_> {
    private final SolutionDescriptor<Solution_> solutionDescriptor;
    private final List<VariableTracker<Solution_>> normalVariableTrackers;
    private final List<ListVariableTracker<Solution_>> listVariableTrackers;
    private List<String> missingEventsForward;
    private List<String> missingEventsBackward;
    Solution_ beforeMoveSolution;
    VariableSnapshotTotal<Solution_> beforeVariables;
    Solution_ afterMoveSolution;
    VariableSnapshotTotal<Solution_> afterVariables;
    Solution_ afterUndoSolution;
    VariableSnapshotTotal<Solution_> undoVariables;
    VariableSnapshotTotal<Solution_> undoFromScratchVariables;
    VariableSnapshotTotal<Solution_> beforeFromScratchVariables;

    public SolutionTracker(SolutionDescriptor<Solution_> solutionDescriptor,
            SupplyManager supplyManager) {
        this.solutionDescriptor = solutionDescriptor;
        normalVariableTrackers = new ArrayList<>();
        listVariableTrackers = new ArrayList<>();
        for (EntityDescriptor<Solution_> entityDescriptor : solutionDescriptor.getEntityDescriptors()) {
            for (VariableDescriptor<Solution_> variableDescriptor : entityDescriptor.getDeclaredVariableDescriptors()) {
                if (variableDescriptor instanceof ListVariableDescriptor<Solution_> listVariableDescriptor) {
                    listVariableTrackers.add(new ListVariableTracker<>(listVariableDescriptor));
                } else {
                    normalVariableTrackers.add(new VariableTracker<>(variableDescriptor));
                }
            }
        }
        normalVariableTrackers.forEach(tracker -> supplyManager.demand(tracker.demand()));
        listVariableTrackers.forEach(tracker -> supplyManager.demand(tracker.demand()));
    }

    public Solution_ getBeforeMoveSolution() {
        return beforeMoveSolution;
    }

    public Solution_ getAfterMoveSolution() {
        return afterMoveSolution;
    }

    public Solution_ getAfterUndoSolution() {
        return afterUndoSolution;
    }

    public void setBeforeMoveSolution(Solution_ workingSolution) {
        beforeVariables = VariableSnapshotTotal.takeSnapshot(solutionDescriptor, workingSolution);
        beforeMoveSolution = cloneSolution(workingSolution);
    }

    public void setAfterMoveSolution(Solution_ workingSolution) {
        afterVariables = VariableSnapshotTotal.takeSnapshot(solutionDescriptor, workingSolution);
        afterMoveSolution = cloneSolution(workingSolution);

        if (beforeVariables != null) {
            missingEventsForward = getEntitiesMissingBeforeAfterEvents(beforeVariables, afterVariables);
        } else {
            missingEventsBackward = Collections.emptyList();
        }
    }

    public void setAfterUndoSolution(Solution_ workingSolution) {
        undoVariables = VariableSnapshotTotal.takeSnapshot(solutionDescriptor, workingSolution);
        afterUndoSolution = cloneSolution(workingSolution);
        if (beforeVariables != null) {
            missingEventsBackward = getEntitiesMissingBeforeAfterEvents(undoVariables, afterVariables);
        } else {
            missingEventsBackward = Collections.emptyList();
        }
    }

    public void setUndoFromScratchSolution(Solution_ workingSolution) {
        undoFromScratchVariables = VariableSnapshotTotal.takeSnapshot(solutionDescriptor, workingSolution);
    }

    public void setBeforeFromScratchSolution(Solution_ workingSolution) {
        beforeFromScratchVariables = VariableSnapshotTotal.takeSnapshot(solutionDescriptor, workingSolution);
    }

    private Solution_ cloneSolution(Solution_ workingSolution) {
        return solutionDescriptor.getSolutionCloner().cloneSolution(workingSolution);
    }

    public void restoreBeforeSolution() {
        beforeVariables.restore();
    }

    private List<String> getEntitiesMissingBeforeAfterEvents(VariableSnapshotTotal<Solution_> beforeSolution,
            VariableSnapshotTotal<Solution_> afterSolution) {
        List<String> out = new ArrayList<>();
        var changes = afterSolution.changedVariablesFrom(beforeSolution);
        for (VariableTracker<Solution_> normalVariableTracker : normalVariableTrackers) {
            out.addAll(normalVariableTracker.getEntitiesMissingBeforeAfterEvents(changes));
        }
        for (ListVariableTracker<Solution_> listVariableTracker : listVariableTrackers) {
            out.addAll(listVariableTracker.getEntitiesMissingBeforeAfterEvents(changes));
        }
        return out;
    }

    public String buildScoreCorruptionMessage() {
        if (beforeMoveSolution == null) {
            return "";
        }

        StringBuilder out = new StringBuilder();
        var changedBetweenBeforeAndUndo = getVariableChangedViolations(beforeVariables,
                undoVariables);

        var changedBetweenBeforeAndScratch = getVariableChangedViolations(beforeFromScratchVariables,
                beforeVariables);

        var changedBetweenUndoAndScratch = getVariableChangedViolations(undoFromScratchVariables,
                undoVariables);

        if (!changedBetweenBeforeAndUndo.isEmpty()) {
            out.append("""
                    Variables that are different between before and undo:
                    %s
                    """.formatted(formatList(changedBetweenBeforeAndUndo)));
        }

        if (!changedBetweenBeforeAndScratch.isEmpty()) {
            out.append("""
                    Variables that are different between from scratch and before:
                    %s
                    """.formatted(formatList(changedBetweenBeforeAndScratch)));
        }

        if (!changedBetweenUndoAndScratch.isEmpty()) {
            out.append("""
                    Variables that are different between from scratch and undo:
                    %s
                    """.formatted(formatList(changedBetweenUndoAndScratch)));
        }

        if (!missingEventsForward.isEmpty()) {
            out.append("""
                    Missing variable listener events for actual move:
                    %s
                    """.formatted(formatList(missingEventsForward)));
        }

        if (!missingEventsBackward.isEmpty()) {
            out.append("""
                    Missing variable listener events for undo move:")
                    %s
                    """.formatted(formatList(missingEventsBackward)));
        }

        if (out.isEmpty()) {
            return "Genuine and shadow variables agree with from scratch calculation after the undo move and match the state prior to the move.";
        }

        return out.toString();
    }

    static <Solution_> List<String> getVariableChangedViolations(
            VariableSnapshotTotal<Solution_> expectedSnapshot,
            VariableSnapshotTotal<Solution_> actualSnapshot) {
        List<String> out = new ArrayList<>();
        var changedVariables = expectedSnapshot.changedVariablesFrom(actualSnapshot);
        for (var changedVariable : changedVariables) {
            var expectedSnapshotVariable = expectedSnapshot.getVariableSnapshot(changedVariable);
            var actualSnapshotVariable = actualSnapshot.getVariableSnapshot(changedVariable);
            out.add("Actual value (%s) of variable %s on %s entity (%s) differs from expected (%s)"
                    .formatted(actualSnapshotVariable.getValue(),
                            expectedSnapshotVariable.getVariableDescriptor().getVariableName(),
                            expectedSnapshotVariable.getVariableDescriptor().getEntityDescriptor().getEntityClass()
                                    .getSimpleName(),
                            expectedSnapshotVariable.getEntity(),
                            expectedSnapshotVariable.getValue()));
        }
        return out;
    }

    static String formatList(List<String> messages) {
        final int LIMIT = 5;
        if (messages.isEmpty()) {
            return "";
        }
        if (messages.size() <= LIMIT) {
            return messages.stream()
                    .collect(Collectors.joining("\n  - ",
                            "  - ", "\n"));
        }
        return messages.stream()
                .limit(LIMIT)
                .collect(Collectors.joining("\n  - ",
                        "  - ", "\n  ...(" + (messages.size() - LIMIT) + " more)\n"));
    }
}
