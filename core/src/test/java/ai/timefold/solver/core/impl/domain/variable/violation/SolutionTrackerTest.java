package ai.timefold.solver.core.impl.domain.variable.violation;

import static ai.timefold.solver.core.testutil.PlannerTestUtils.mockScoreDirector;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.VariableSupport;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataSolution;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class SolutionTrackerTest {

    @Test
    public void testFormatList() {
        assertThat(SolutionTracker.formatList(List.of())).isEqualTo("");
        assertThat(SolutionTracker.formatList(List.of("item 1", "item 2", "item 3")))
                .isEqualTo(
                        """
                                  - item 1
                                  - item 2
                                  - item 3
                                """);
        assertThat(SolutionTracker.formatList(List.of("item 1", "item 2", "item 3", "4", "5")))
                .isEqualTo(
                        """
                                  - item 1
                                  - item 2
                                  - item 3
                                  - 4
                                  - 5
                                """);
        assertThat(SolutionTracker.formatList(List.of("item 1", "item 2", "item 3", "4", "5", "6", "7", "8")))
                .isEqualTo(
                        """
                                  - item 1
                                  - item 2
                                  - item 3
                                  - 4
                                  - 5
                                  ...(3 more)
                                """);
    }

    @Test
    void testGetVariableChangedViolations() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();

        var workingSolution = TestdataSolution.generateSolution(3, 3);
        VariableSnapshotTotal<TestdataSolution> before = VariableSnapshotTotal.takeSnapshot(solutionDescriptor,
                workingSolution);

        workingSolution.getEntityList().get(0).setValue(null);
        workingSolution.getEntityList().get(1).setValue(workingSolution.getValueList().get(0));

        VariableSnapshotTotal<TestdataSolution> after = VariableSnapshotTotal.takeSnapshot(solutionDescriptor,
                workingSolution);

        assertThat(SolutionTracker.getVariableChangedViolations(before, after)).containsExactlyInAnyOrder(
                "Actual value (null) of variable value on TestdataEntity entity (" + workingSolution.getEntityList()
                        .get(0) + ") differs from expected (" + workingSolution.getValueList().get(0) + ")",
                "Actual value (" + workingSolution.getValueList()
                        .get(0) + ") of variable value on TestdataEntity entity ("
                        + workingSolution.getEntityList()
                                .get(1)
                        + ") differs from expected (" + workingSolution.getValueList().get(1) + ")");
    }

    @Test
    void testGetVariableChangedViolationsNoChanges() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();

        var workingSolution = TestdataSolution.generateSolution(3, 3);
        VariableSnapshotTotal<TestdataSolution> before = VariableSnapshotTotal.takeSnapshot(solutionDescriptor,
                workingSolution);

        VariableSnapshotTotal<TestdataSolution> after = VariableSnapshotTotal.takeSnapshot(solutionDescriptor,
                workingSolution);

        assertThat(SolutionTracker.getVariableChangedViolations(before, after)).isEmpty();
    }

    @Test
    void testBuildScoreCorruptionMessage() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var scoreDirector = mockScoreDirector(solutionDescriptor);
        var tracker = new SolutionTracker<>(solutionDescriptor, scoreDirector);

        var workingSolution = TestdataSolution.generateSolution(3, 3);
        tracker.setBeforeMoveSolution(workingSolution);

        workingSolution.getEntityList().get(0).setValue(null);
        workingSolution.getEntityList().get(1).setValue(workingSolution.getValueList().get(0));

        tracker.setAfterMoveSolution(workingSolution);

        workingSolution.getEntityList().get(1).setValue(workingSolution.getValueList().get(1));

        tracker.setAfterUndoSolution(workingSolution);

        workingSolution.getEntityList().get(1).setValue(null);

        tracker.setUndoFromScratchSolution(workingSolution);
        tracker.setBeforeFromScratchSolution(workingSolution);

        assertThat(tracker.buildScoreCorruptionMessage())
                .contains("Variables that are different between before and undo:",
                        "Actual value (null) of variable value on TestdataEntity entity (Generated Entity 0) differs from expected (Generated Value 0)",
                        "Variables that are different between from scratch and before",
                        "Actual value (Generated Value 1) of variable value on TestdataEntity entity (Generated Entity 1) differs from expected (null)",
                        "Actual value (Generated Value 0) of variable value on TestdataEntity entity (Generated Entity 0) differs from expected (null)",
                        "Variables that are different between from scratch and undo",
                        "Actual value (Generated Value 1) of variable value on TestdataEntity entity (Generated Entity 1) differs from expected (null)",
                        "Missing shadow variable update events for actual move",
                        "Entity (Generated Entity 1) is missing a beforeVariableChanged call for variable (value).",
                        "Entity (Generated Entity 1) is missing a afterVariableChanged call for variable (value).",
                        "Entity (Generated Entity 0) is missing a beforeVariableChanged call for variable (value).",
                        "Entity (Generated Entity 0) is missing a afterVariableChanged call for variable (value).",
                        "Missing shadow variable update events for undo move",
                        "Entity (Generated Entity 1) is missing a beforeVariableChanged call for variable (value).",
                        "Entity (Generated Entity 1) is missing a afterVariableChanged call for variable (value).");
    }

    @Test
    void testBuildScoreCorruptionMessageGoodShadowVariables() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var scoreDirector = mockScoreDirector(solutionDescriptor);
        var variableSupport = VariableSupport.create(scoreDirector);
        var variableTrackers = new ArrayList<BasicVariableTracker<?>>();
        Mockito.doAnswer(invocation -> {
            var variableDescriptor = invocation.getArgument(0, VariableDescriptor.class);
            var variableTracker = variableSupport.getBasicVariableTracker(variableDescriptor);
            variableTrackers.add(variableTracker);
            return variableTracker;
        }).when(scoreDirector).getBasicVariableTracker(any(VariableDescriptor.class));
        var tracker = new SolutionTracker<>(solutionDescriptor, scoreDirector);

        var workingSolution = TestdataSolution.generateSolution(3, 3);
        tracker.setBeforeMoveSolution(workingSolution);

        variableTrackers.forEach(variableTracker -> {
            variableTracker.beforeVariableChanged(null, workingSolution.getEntityList().get(0));
        });
        workingSolution.getEntityList().get(0).setValue(null);
        variableTrackers.forEach(variableTracker -> {
            variableTracker.afterVariableChanged(null, workingSolution.getEntityList().get(0));
        });

        variableTrackers.forEach(variableTracker -> {
            variableTracker.beforeVariableChanged(null, workingSolution.getEntityList().get(1));
        });
        workingSolution.getEntityList().get(1).setValue(workingSolution.getValueList().get(0));
        variableTrackers.forEach(variableTracker -> {
            variableTracker.afterVariableChanged(null, workingSolution.getEntityList().get(1));
        });

        tracker.setAfterMoveSolution(workingSolution);

        variableTrackers.forEach(variableTracker -> {
            variableTracker.beforeVariableChanged(null, workingSolution.getEntityList().get(1));
        });
        workingSolution.getEntityList().get(1).setValue(workingSolution.getValueList().get(1));
        variableTrackers.forEach(variableTracker -> {
            variableTracker.afterVariableChanged(null, workingSolution.getEntityList().get(1));
        });

        tracker.setAfterUndoSolution(workingSolution);

        workingSolution.getEntityList().get(1).setValue(null);

        tracker.setUndoFromScratchSolution(workingSolution);
        tracker.setBeforeFromScratchSolution(workingSolution);

        assertThat(tracker.buildScoreCorruptionMessage())
                .contains("Variables that are different between before and undo:",
                        "Actual value (null) of variable value on TestdataEntity entity (Generated Entity 0) differs from expected (Generated Value 0)",
                        "Variables that are different between from scratch and before",
                        "Actual value (Generated Value 1) of variable value on TestdataEntity entity (Generated Entity 1) differs from expected (null)",
                        "Actual value (Generated Value 0) of variable value on TestdataEntity entity (Generated Entity 0) differs from expected (null)",
                        "Variables that are different between from scratch and undo",
                        "Actual value (Generated Value 1) of variable value on TestdataEntity entity (Generated Entity 1) differs from expected (null)")
                .doesNotContain("Missing shadow variable update events for actual move",
                        "Entity (Generated Entity 1) is missing a beforeVariableChanged call for variable (value).",
                        "Entity (Generated Entity 1) is missing a afterVariableChanged call for variable (value).",
                        "Entity (Generated Entity 0) is missing a beforeVariableChanged call for variable (value).",
                        "Entity (Generated Entity 0) is missing a afterVariableChanged call for variable (value).",
                        "Missing shadow variable update events for undo move",
                        "Entity (Generated Entity 1) is missing a beforeVariableChanged call for variable (value).",
                        "Entity (Generated Entity 1) is missing a afterVariableChanged call for variable (value).");
    }

    @Test
    void testBuildScoreCorruptionMessageGoodForwardShadowVariables() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var scoreDirector = mockScoreDirector(solutionDescriptor);
        var variableSupport = VariableSupport.create(scoreDirector);
        var variableTrackers = new ArrayList<BasicVariableTracker<?>>();
        Mockito.doAnswer(invocation -> {
            var variableDescriptor = invocation.getArgument(0, VariableDescriptor.class);
            var variableTracker = variableSupport.getBasicVariableTracker(variableDescriptor);
            variableTrackers.add(variableTracker);
            return variableTracker;
        }).when(scoreDirector).getBasicVariableTracker(any(VariableDescriptor.class));
        var tracker = new SolutionTracker<>(solutionDescriptor, scoreDirector);

        var workingSolution = TestdataSolution.generateSolution(3, 3);
        tracker.setBeforeMoveSolution(workingSolution);

        variableTrackers.forEach(variableTracker -> {
            variableTracker.beforeVariableChanged(null, workingSolution.getEntityList().get(0));
        });
        workingSolution.getEntityList().get(0).setValue(null);
        variableTrackers.forEach(variableTracker -> {
            variableTracker.afterVariableChanged(null, workingSolution.getEntityList().get(0));
        });

        variableTrackers.forEach(variableTracker -> {
            variableTracker.beforeVariableChanged(null, workingSolution.getEntityList().get(1));
        });
        workingSolution.getEntityList().get(1).setValue(workingSolution.getValueList().get(0));
        variableTrackers.forEach(variableTracker -> {
            variableTracker.afterVariableChanged(null, workingSolution.getEntityList().get(1));
        });

        tracker.setAfterMoveSolution(workingSolution);

        workingSolution.getEntityList().get(1).setValue(workingSolution.getValueList().get(1));

        tracker.setAfterUndoSolution(workingSolution);

        workingSolution.getEntityList().get(1).setValue(null);

        tracker.setUndoFromScratchSolution(workingSolution);
        tracker.setBeforeFromScratchSolution(workingSolution);

        assertThat(tracker.buildScoreCorruptionMessage())
                .contains("Variables that are different between before and undo:",
                        "Actual value (null) of variable value on TestdataEntity entity (Generated Entity 0) differs from expected (Generated Value 0)",
                        "Variables that are different between from scratch and before",
                        "Actual value (Generated Value 1) of variable value on TestdataEntity entity (Generated Entity 1) differs from expected (null)",
                        "Actual value (Generated Value 0) of variable value on TestdataEntity entity (Generated Entity 0) differs from expected (null)",
                        "Variables that are different between from scratch and undo",
                        "Actual value (Generated Value 1) of variable value on TestdataEntity entity (Generated Entity 1) differs from expected (null)",
                        "Missing shadow variable update events for undo move",
                        "Entity (Generated Entity 1) is missing a beforeVariableChanged call for variable (value).",
                        "Entity (Generated Entity 1) is missing a afterVariableChanged call for variable (value).")
                .doesNotContain("Missing shadow variable update events for actual move",
                        "Entity (Generated Entity 0) is missing a beforeVariableChanged call for variable (value).",
                        "Entity (Generated Entity 0) is missing a afterVariableChanged call for variable (value).");
    }

    @Test
    void testBuildScoreCorruptionMessageGoodUndoShadowVariables() {
        var solutionDescriptor = TestdataSolution.buildSolutionDescriptor();
        var scoreDirector = mockScoreDirector(solutionDescriptor);
        var variableSupport = VariableSupport.create(scoreDirector);
        var variableTrackers = new ArrayList<BasicVariableTracker<?>>();
        Mockito.doAnswer(invocation -> {
            var variableDescriptor = invocation.getArgument(0, VariableDescriptor.class);
            var variableTracker = variableSupport.getBasicVariableTracker(variableDescriptor);
            variableTrackers.add(variableTracker);
            return variableTracker;
        }).when(scoreDirector).getBasicVariableTracker(any(VariableDescriptor.class));
        var tracker = new SolutionTracker<>(solutionDescriptor, scoreDirector);

        var workingSolution = TestdataSolution.generateSolution(3, 3);
        tracker.setBeforeMoveSolution(workingSolution);

        workingSolution.getEntityList().get(0).setValue(null);
        workingSolution.getEntityList().get(1).setValue(workingSolution.getValueList().get(0));

        tracker.setAfterMoveSolution(workingSolution);

        variableTrackers.forEach(variableTracker -> {
            variableTracker.beforeVariableChanged(null, workingSolution.getEntityList().get(1));
        });
        workingSolution.getEntityList().get(1).setValue(workingSolution.getValueList().get(1));
        variableTrackers.forEach(variableTracker -> {
            variableTracker.afterVariableChanged(null, workingSolution.getEntityList().get(1));
        });

        tracker.setAfterUndoSolution(workingSolution);

        workingSolution.getEntityList().get(1).setValue(null);

        tracker.setUndoFromScratchSolution(workingSolution);
        tracker.setBeforeFromScratchSolution(workingSolution);

        assertThat(tracker.buildScoreCorruptionMessage())
                .contains("Variables that are different between before and undo:",
                        "Actual value (null) of variable value on TestdataEntity entity (Generated Entity 0) differs from expected (Generated Value 0)",
                        "Variables that are different between from scratch and before",
                        "Actual value (Generated Value 1) of variable value on TestdataEntity entity (Generated Entity 1) differs from expected (null)",
                        "Actual value (Generated Value 0) of variable value on TestdataEntity entity (Generated Entity 0) differs from expected (null)",
                        "Variables that are different between from scratch and undo",
                        "Actual value (Generated Value 1) of variable value on TestdataEntity entity (Generated Entity 1) differs from expected (null)")
                .doesNotContain("Missing shadow variable update events for undo move");
    }
}
