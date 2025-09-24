package ai.timefold.solver.core.impl.domain.variable.listener.support.violation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.BasicVariableChangeEvent;
import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishEntity;
import ai.timefold.solver.core.testdomain.score.lavish.TestdataLavishEntityGroup;

import org.junit.jupiter.api.Test;

public class NormalVariableTrackerTest {
    final static VariableDescriptor<TestdataSolution> VARIABLE_DESCRIPTOR = TestdataEntity.buildVariableDescriptorForValue();

    @Test
    void testMissingBeforeEvents() {
        var tracker = new VariableTracker<>(VARIABLE_DESCRIPTOR);

        var a = new TestdataEntity("a");
        var b = new TestdataEntity("b");

        tracker.beforeChange(null, new BasicVariableChangeEvent<>(a));
        tracker.afterChange(null, new BasicVariableChangeEvent<>(a));

        // intentionally missing before event for b
        tracker.afterChange(null, new BasicVariableChangeEvent<>(b));

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>(VARIABLE_DESCRIPTOR, b))))
                .containsExactlyInAnyOrder("Entity (" + b + ") is missing a beforeVariableChanged call for variable (value).");
    }

    @Test
    void testMissingAfterEvents() {
        var tracker = new VariableTracker<>(VARIABLE_DESCRIPTOR);

        var a = new TestdataEntity("a");
        var b = new TestdataEntity("b");

        tracker.beforeChange(null, new BasicVariableChangeEvent<>(a));
        tracker.afterChange(null, new BasicVariableChangeEvent<>(a));

        // intentionally missing after event for b
        tracker.beforeChange(null, new BasicVariableChangeEvent<>(b));

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>(VARIABLE_DESCRIPTOR, b))))
                .containsExactlyInAnyOrder("Entity (" + b + ") is missing a afterVariableChanged call for variable (value).");
    }

    @Test
    void testMissingBeforeAndAfterEvents() {
        var tracker = new VariableTracker<>(VARIABLE_DESCRIPTOR);

        var a = new TestdataEntity("a");
        var b = new TestdataEntity("b");

        tracker.beforeChange(null, new BasicVariableChangeEvent<>(a));
        tracker.afterChange(null, new BasicVariableChangeEvent<>(a));

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>(VARIABLE_DESCRIPTOR, b))))
                .containsExactlyInAnyOrder("Entity (" + b + ") is missing a beforeVariableChanged call for variable (value).",
                        "Entity (" + b + ") is missing a afterVariableChanged call for variable (value).");
    }

    @Test
    void testNoMissingEvents() {
        var tracker = new VariableTracker<>(VARIABLE_DESCRIPTOR);

        var a = new TestdataEntity("a");
        var b = new TestdataEntity("b");

        tracker.beforeChange(null, new BasicVariableChangeEvent<>(a));
        tracker.afterChange(null, new BasicVariableChangeEvent<>(a));
        tracker.beforeChange(null, new BasicVariableChangeEvent<>(b));
        tracker.afterChange(null, new BasicVariableChangeEvent<>(b));

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>(VARIABLE_DESCRIPTOR, b)))).isEmpty();
    }

    @Test
    void testEventsResetAfterCall() {
        var tracker = new VariableTracker<>(VARIABLE_DESCRIPTOR);

        var a = new TestdataEntity("a");
        var b = new TestdataEntity("b");

        tracker.beforeChange(null, new BasicVariableChangeEvent<>(a));
        tracker.afterChange(null, new BasicVariableChangeEvent<>(a));
        tracker.beforeChange(null, new BasicVariableChangeEvent<>(b));
        tracker.afterChange(null, new BasicVariableChangeEvent<>(b));

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
        var tracker = new VariableTracker<>(VARIABLE_DESCRIPTOR);
        var otherVariableDescriptor =
                TestdataLavishEntity.buildVariableDescriptorForValue();

        var a = new TestdataEntity("a");
        var b = new TestdataLavishEntity("b", new TestdataLavishEntityGroup("group"));

        tracker.beforeChange(null, new BasicVariableChangeEvent<>(a));
        tracker.afterChange(null, new BasicVariableChangeEvent<>(a));

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>((VariableDescriptor) otherVariableDescriptor, b)))).isEmpty();
    }
}
