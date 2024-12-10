package ai.timefold.solver.core.impl.domain.variable.listener.support.violation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.shadow_history.TestdataListEntityWithShadowHistory;
import ai.timefold.solver.core.impl.testdata.domain.list.shadow_history.TestdataListSolutionWithShadowHistory;

import org.junit.jupiter.api.Test;

public class ListVariableTrackerTest {
    final static SolutionDescriptor<TestdataListSolution> SOLUTION_DESCRIPTOR = TestdataListSolution.buildSolutionDescriptor();
    final static ListVariableDescriptor<TestdataListSolution> VARIABLE_DESCRIPTOR =
            SOLUTION_DESCRIPTOR.getListVariableDescriptor();

    final static SolutionDescriptor<TestdataListSolutionWithShadowHistory> SOLUTION_WITH_SHADOW_HISTORY_SOLUTION_DESCRIPTOR =
            TestdataListSolutionWithShadowHistory.buildSolutionDescriptor();
    final static ListVariableDescriptor<TestdataListSolutionWithShadowHistory> VARIABLE_DESCRIPTOR_FOR_SHADOW_ENTITY =
            SOLUTION_WITH_SHADOW_HISTORY_SOLUTION_DESCRIPTOR.getListVariableDescriptor();

    @Test
    void testMissingBeforeEvents() {
        ListVariableTracker<TestdataListSolution> tracker = new ListVariableTracker<>(VARIABLE_DESCRIPTOR);

        var a = new TestdataListEntity("a");
        var b = new TestdataListEntity("b");

        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(Collections.emptyList());
        var before = VariableSnapshotTotal.takeSnapshot(SOLUTION_DESCRIPTOR,
                solution);

        tracker.beforeListVariableChanged(null, a, 0, 1);
        tracker.afterListVariableChanged(null, a, 0, 1);

        // intentionally missing before event for b
        tracker.afterListVariableChanged(null, b, 0, 1);

        var after = VariableSnapshotTotal.takeSnapshot(SOLUTION_DESCRIPTOR,
                solution);

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>(VARIABLE_DESCRIPTOR, b)),
                before, after)).containsExactlyInAnyOrder(
                        "Entity (" + b + ") is missing a beforeListVariableChanged call for list variable (valueList).");
    }

    @Test
    void testMissingAfterEvents() {
        ListVariableTracker<TestdataListSolution> tracker = new ListVariableTracker<>(VARIABLE_DESCRIPTOR);

        var a = new TestdataListEntity("a");
        var b = new TestdataListEntity("b");

        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(Collections.emptyList());
        var before = VariableSnapshotTotal.takeSnapshot(SOLUTION_DESCRIPTOR,
                solution);

        tracker.beforeListVariableChanged(null, a, 0, 1);
        tracker.afterListVariableChanged(null, a, 0, 1);

        // intentionally missing after event for b
        tracker.beforeListVariableChanged(null, b, 0, 1);

        var after = VariableSnapshotTotal.takeSnapshot(SOLUTION_DESCRIPTOR,
                solution);

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>(VARIABLE_DESCRIPTOR, b)),
                before, after)).containsExactlyInAnyOrder(
                        "Entity (" + b + ") is missing a afterListVariableChanged call for list variable (valueList).");
    }

    @Test
    void testMissingBeforeAndAfterEvents() {
        ListVariableTracker<TestdataListSolution> tracker = new ListVariableTracker<>(VARIABLE_DESCRIPTOR);

        var a = new TestdataListEntity("a");
        var b = new TestdataListEntity("b");

        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(Collections.emptyList());
        var before = VariableSnapshotTotal.takeSnapshot(SOLUTION_DESCRIPTOR,
                solution);

        tracker.beforeListVariableChanged(null, a, 0, 1);
        tracker.afterListVariableChanged(null, a, 0, 1);

        var after = VariableSnapshotTotal.takeSnapshot(SOLUTION_DESCRIPTOR,
                solution);

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>(VARIABLE_DESCRIPTOR, b)),
                before, after)).containsExactlyInAnyOrder(
                        "Entity (" + b + ") is missing a beforeListVariableChanged call for list variable (valueList).",
                        "Entity (" + b + ") is missing a afterListVariableChanged call for list variable (valueList).");
    }

    @Test
    void testMissingUnassignEvents() {
        ListVariableTracker<TestdataListSolution> tracker = new ListVariableTracker<>(VARIABLE_DESCRIPTOR);

        var value = new TestdataListValue("v1");
        var a = new TestdataListEntity("a", value);
        var b = new TestdataListEntity("b");

        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(List.of(value));

        var before = VariableSnapshotTotal.takeSnapshot(SOLUTION_DESCRIPTOR,
                solution);

        tracker.beforeListVariableChanged(null, a, 0, 1);
        a.getValueList().clear();
        tracker.afterListVariableChanged(null, a, 0, 0);

        var after = VariableSnapshotTotal.takeSnapshot(SOLUTION_DESCRIPTOR,
                solution);

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a)),
                before, after)).containsExactlyInAnyOrder("Missing afterListElementUnassigned: v1");
    }

    @Test
    void testNoMissingEvents() {
        ListVariableTracker<TestdataListSolution> tracker = new ListVariableTracker<>(VARIABLE_DESCRIPTOR);

        var a = new TestdataListEntity("a");
        var b = new TestdataListEntity("b");

        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(Collections.emptyList());
        var before = VariableSnapshotTotal.takeSnapshot(SOLUTION_DESCRIPTOR,
                solution);

        tracker.beforeListVariableChanged(null, a, 0, 1);
        tracker.afterListVariableChanged(null, a, 0, 1);
        tracker.beforeListVariableChanged(null, b, 0, 1);
        tracker.afterListVariableChanged(null, b, 0, 1);

        var after = VariableSnapshotTotal.takeSnapshot(SOLUTION_DESCRIPTOR,
                solution);

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>(VARIABLE_DESCRIPTOR, b)),
                before, after)).isEmpty();
    }

    @Test
    void testEventsResetAfterCall() {
        ListVariableTracker<TestdataListSolution> tracker = new ListVariableTracker<>(VARIABLE_DESCRIPTOR);

        var a = new TestdataListEntity("a");
        var b = new TestdataListEntity("b");

        var solution = new TestdataListSolution();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(Collections.emptyList());
        var before = VariableSnapshotTotal.takeSnapshot(SOLUTION_DESCRIPTOR,
                solution);

        tracker.beforeListVariableChanged(null, a, 0, 1);
        tracker.afterListVariableChanged(null, a, 0, 1);
        tracker.beforeListVariableChanged(null, b, 0, 1);
        tracker.afterListVariableChanged(null, b, 0, 1);

        var after = VariableSnapshotTotal.takeSnapshot(SOLUTION_DESCRIPTOR,
                solution);

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>(VARIABLE_DESCRIPTOR, b)),
                before, after)).isEmpty();

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>(VARIABLE_DESCRIPTOR, b)),
                before, after)).containsExactlyInAnyOrder(
                        "Entity (" + a + ") is missing a beforeListVariableChanged call for list variable (valueList).",
                        "Entity (" + a + ") is missing a afterListVariableChanged call for list variable (valueList).",
                        "Entity (" + b + ") is missing a beforeListVariableChanged call for list variable (valueList).",
                        "Entity (" + b + ") is missing a afterListVariableChanged call for list variable (valueList).");
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void testDoesNotIncludeMissingEventsForOtherVariables() {
        ListVariableTracker<TestdataListSolutionWithShadowHistory> tracker =
                new ListVariableTracker<>(VARIABLE_DESCRIPTOR_FOR_SHADOW_ENTITY);
        VariableDescriptor<?> otherVariableDescriptor =
                TestdataListEntityWithShadowHistory.buildVariableDescriptorForValueList();

        var a = new TestdataListEntityWithShadowHistory("a");
        var b = new TestdataListEntityWithShadowHistory("b");

        var solution = new TestdataListSolutionWithShadowHistory();
        solution.setEntityList(List.of(a, b));
        solution.setValueList(Collections.emptyList());
        var before = VariableSnapshotTotal.takeSnapshot(SOLUTION_WITH_SHADOW_HISTORY_SOLUTION_DESCRIPTOR,
                solution);

        tracker.beforeListVariableChanged(null, a, 0, 1);
        tracker.afterListVariableChanged(null, a, 0, 1);

        var after = VariableSnapshotTotal.takeSnapshot(SOLUTION_WITH_SHADOW_HISTORY_SOLUTION_DESCRIPTOR,
                solution);

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR_FOR_SHADOW_ENTITY, a),
                new VariableId<>((VariableDescriptor) otherVariableDescriptor, b)),
                before, after)).isEmpty();
    }
}
