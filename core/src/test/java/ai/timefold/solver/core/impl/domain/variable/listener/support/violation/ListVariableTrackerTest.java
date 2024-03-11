package ai.timefold.solver.core.impl.domain.variable.listener.support.violation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.TestdataListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.shadow_history.TestdataListEntityWithShadowHistory;

import org.junit.jupiter.api.Test;

public class ListVariableTrackerTest {
    final static ListVariableDescriptor<TestdataListSolution> VARIABLE_DESCRIPTOR =
            TestdataListEntity.buildVariableDescriptorForValueList();

    @Test
    void testMissingBeforeEvents() {
        ListVariableTracker<TestdataListSolution> tracker = new ListVariableTracker<>(VARIABLE_DESCRIPTOR);

        TestdataEntity a = new TestdataEntity("a");
        TestdataEntity b = new TestdataEntity("b");

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
        ListVariableTracker<TestdataListSolution> tracker = new ListVariableTracker<>(VARIABLE_DESCRIPTOR);

        TestdataEntity a = new TestdataEntity("a");
        TestdataEntity b = new TestdataEntity("b");

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
        ListVariableTracker<TestdataListSolution> tracker = new ListVariableTracker<>(VARIABLE_DESCRIPTOR);

        TestdataEntity a = new TestdataEntity("a");
        TestdataEntity b = new TestdataEntity("b");

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
        ListVariableTracker<TestdataListSolution> tracker = new ListVariableTracker<>(VARIABLE_DESCRIPTOR);

        TestdataEntity a = new TestdataEntity("a");
        TestdataEntity b = new TestdataEntity("b");

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
        ListVariableTracker<TestdataListSolution> tracker = new ListVariableTracker<>(VARIABLE_DESCRIPTOR);

        TestdataEntity a = new TestdataEntity("a");
        TestdataEntity b = new TestdataEntity("b");

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
        ListVariableTracker<TestdataListSolution> tracker = new ListVariableTracker<>(VARIABLE_DESCRIPTOR);
        VariableDescriptor<?> otherVariableDescriptor =
                TestdataListEntityWithShadowHistory.buildVariableDescriptorForValueList();

        TestdataEntity a = new TestdataEntity("a");
        TestdataListEntityWithShadowHistory b = new TestdataListEntityWithShadowHistory("b");

        tracker.beforeListVariableChanged(null, a, 0, 1);
        tracker.afterListVariableChanged(null, a, 0, 1);

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>((VariableDescriptor) otherVariableDescriptor, b)))).isEmpty();
    }
}
