package ai.timefold.solver.core.impl.domain.variable.violation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishEntity;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishEntityGroup;

import org.junit.jupiter.api.Test;

public class BasicVariableTrackerTest {
    final static VariableDescriptor<TestdataSolution> VARIABLE_DESCRIPTOR = TestdataEntity.buildVariableDescriptorForValue();

    @Test
    void testMissingBeforeEvents() {
        var tracker = new BasicVariableTracker<>(VARIABLE_DESCRIPTOR);

        var a = new TestdataEntity("a");
        var b = new TestdataEntity("b");

        tracker.beforeVariableChanged(null, a);
        tracker.afterVariableChanged(null, a);

        // intentionally missing before event for b
        tracker.afterVariableChanged(null, b);

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>(VARIABLE_DESCRIPTOR, b))))
                .containsExactlyInAnyOrder("Entity (" + b + ") is missing a beforeVariableChanged call for variable (value).");
    }

    @Test
    void testMissingAfterEvents() {
        var tracker = new BasicVariableTracker<>(VARIABLE_DESCRIPTOR);

        var a = new TestdataEntity("a");
        var b = new TestdataEntity("b");

        tracker.beforeVariableChanged(null, a);
        tracker.afterVariableChanged(null, a);

        // intentionally missing after event for b
        tracker.beforeVariableChanged(null, b);

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>(VARIABLE_DESCRIPTOR, b))))
                .containsExactlyInAnyOrder("Entity (" + b + ") is missing a afterVariableChanged call for variable (value).");
    }

    @Test
    void testMissingBeforeAndAfterEvents() {
        var tracker = new BasicVariableTracker<>(VARIABLE_DESCRIPTOR);

        var a = new TestdataEntity("a");
        var b = new TestdataEntity("b");

        tracker.beforeVariableChanged(null, a);
        tracker.afterVariableChanged(null, a);

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>(VARIABLE_DESCRIPTOR, b))))
                .containsExactlyInAnyOrder("Entity (" + b + ") is missing a beforeVariableChanged call for variable (value).",
                        "Entity (" + b + ") is missing a afterVariableChanged call for variable (value).");
    }

    @Test
    void testNoMissingEvents() {
        var tracker = new BasicVariableTracker<>(VARIABLE_DESCRIPTOR);

        var a = new TestdataEntity("a");
        var b = new TestdataEntity("b");

        tracker.beforeVariableChanged(null, a);
        tracker.afterVariableChanged(null, a);
        tracker.beforeVariableChanged(null, b);
        tracker.afterVariableChanged(null, b);

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>(VARIABLE_DESCRIPTOR, b)))).isEmpty();
    }

    @Test
    void testEventsResetAfterCall() {
        var tracker = new BasicVariableTracker<>(VARIABLE_DESCRIPTOR);

        var a = new TestdataEntity("a");
        var b = new TestdataEntity("b");

        tracker.beforeVariableChanged(null, a);
        tracker.afterVariableChanged(null, a);
        tracker.beforeVariableChanged(null, b);
        tracker.afterVariableChanged(null, b);

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>(VARIABLE_DESCRIPTOR, b)))).isEmpty();

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>(VARIABLE_DESCRIPTOR, b)))).containsExactlyInAnyOrder(
                        "Entity (" + a + ") is missing a beforeVariableChanged call for variable (value).",
                        "Entity (" + a + ") is missing a afterVariableChanged call for variable (value).",
                        "Entity (" + b + ") is missing a beforeVariableChanged call for variable (value).",
                        "Entity (" + b + ") is missing a afterVariableChanged call for variable (value).");
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void testDoesNotIncludeMissingEventsForOtherVariables() {
        var tracker = new BasicVariableTracker<>(VARIABLE_DESCRIPTOR);
        var otherVariableDescriptor =
                TestdataLavishEntity.buildVariableDescriptorForValue();

        var a = new TestdataEntity("a");
        var b = new TestdataLavishEntity("b", new TestdataLavishEntityGroup("group"));

        tracker.beforeVariableChanged(null, a);
        tracker.afterVariableChanged(null, a);

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>((VariableDescriptor) otherVariableDescriptor, b)))).isEmpty();
    }
}
