package ai.timefold.solver.core.impl.domain.variable.listener.support.violation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;
import ai.timefold.solver.core.testdomain.list.shadowhistory.TestdataListEntityWithShadowHistory;

import org.junit.jupiter.api.Test;

class ListVariableTrackerTest {
    static final ListVariableDescriptor<TestdataListSolution> VARIABLE_DESCRIPTOR =
            TestdataListEntity.buildVariableDescriptorForValueList();

    @Test
    void testMissingBeforeEvents() {
        var tracker = new ListVariableTracker<>(VARIABLE_DESCRIPTOR);

        var a = new TestdataEntity("a");
        var b = new TestdataEntity("b");

        tracker.beforeListVariableChanged(null, a, 0, 1);
        tracker.afterListVariableChanged(null, a, 0, 1);

        // intentionally missing before event for b
        tracker.afterListVariableChanged(null, b, 0, 1);

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>(VARIABLE_DESCRIPTOR, b)))).containsExactlyInAnyOrder(
                        "Entity (" + b + ") is missing a beforeListVariableChanged call for list variable (valueList).");
    }

    @Test
    void testMissingAfterEvents() {
        var tracker = new ListVariableTracker<>(VARIABLE_DESCRIPTOR);

        var a = new TestdataEntity("a");
        var b = new TestdataEntity("b");

        tracker.beforeListVariableChanged(null, a, 0, 1);
        tracker.afterListVariableChanged(null, a, 0, 1);

        // intentionally missing after event for b
        tracker.beforeListVariableChanged(null, b, 0, 1);

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>(VARIABLE_DESCRIPTOR, b)))).containsExactlyInAnyOrder(
                        "Entity (" + b + ") is missing a afterListVariableChanged call for list variable (valueList).");
    }

    @Test
    void testMissingBeforeAndAfterEvents() {
        var tracker = new ListVariableTracker<>(VARIABLE_DESCRIPTOR);

        var a = new TestdataEntity("a");
        var b = new TestdataEntity("b");

        tracker.beforeListVariableChanged(null, a, 0, 1);
        tracker.afterListVariableChanged(null, a, 0, 1);

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>(VARIABLE_DESCRIPTOR, b)))).containsExactlyInAnyOrder(
                        "Entity (" + b + ") is missing a beforeListVariableChanged call for list variable (valueList).",
                        "Entity (" + b + ") is missing a afterListVariableChanged call for list variable (valueList).");
    }

    @Test
    void testNoMissingEvents() {
        var tracker = new ListVariableTracker<>(VARIABLE_DESCRIPTOR);

        var a = new TestdataEntity("a");
        var b = new TestdataEntity("b");

        tracker.beforeListVariableChanged(null, a, 0, 1);
        tracker.afterListVariableChanged(null, a, 0, 1);
        tracker.beforeListVariableChanged(null, b, 0, 1);
        tracker.afterListVariableChanged(null, b, 0, 1);

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>(VARIABLE_DESCRIPTOR, b)))).isEmpty();
    }

    @Test
    void testEventsResetAfterCall() {
        var tracker = new ListVariableTracker<>(VARIABLE_DESCRIPTOR);

        var a = new TestdataEntity("a");
        var b = new TestdataEntity("b");

        tracker.beforeListVariableChanged(null, a, 0, 1);
        tracker.afterListVariableChanged(null, a, 0, 1);
        tracker.beforeListVariableChanged(null, b, 0, 1);
        tracker.afterListVariableChanged(null, b, 0, 1);

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>(VARIABLE_DESCRIPTOR, b)))).isEmpty();

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>(VARIABLE_DESCRIPTOR, b)))).containsExactlyInAnyOrder(
                        "Entity (" + a + ") is missing a beforeListVariableChanged call for list variable (valueList).",
                        "Entity (" + a + ") is missing a afterListVariableChanged call for list variable (valueList).",
                        "Entity (" + b + ") is missing a beforeListVariableChanged call for list variable (valueList).",
                        "Entity (" + b + ") is missing a afterListVariableChanged call for list variable (valueList).");
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void testDoesNotIncludeMissingEventsForOtherVariables() {
        var tracker = new ListVariableTracker<>(VARIABLE_DESCRIPTOR);
        var otherVariableDescriptor = TestdataListEntityWithShadowHistory.buildVariableDescriptorForValueList();

        var a = new TestdataEntity("a");
        var b = new TestdataListEntityWithShadowHistory("b");

        tracker.beforeListVariableChanged(null, a, 0, 1);
        tracker.afterListVariableChanged(null, a, 0, 1);

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>((VariableDescriptor) otherVariableDescriptor, b)))).isEmpty();
    }
}
