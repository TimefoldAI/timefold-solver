package ai.timefold.solver.core.impl.domain.variable.listener.support.violation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.descriptor.VariableDescriptor;
import ai.timefold.solver.core.impl.testdata.domain.TestdataEntity;
import ai.timefold.solver.core.impl.testdata.domain.TestdataSolution;
import ai.timefold.solver.core.impl.testdata.domain.score.lavish.TestdataLavishEntity;
import ai.timefold.solver.core.impl.testdata.domain.score.lavish.TestdataLavishEntityGroup;
import ai.timefold.solver.core.impl.testdata.domain.score.lavish.TestdataLavishSolution;

import org.junit.jupiter.api.Test;

public class NormalVariableTrackerTest {
    final static VariableDescriptor<TestdataSolution> VARIABLE_DESCRIPTOR = TestdataEntity.buildVariableDescriptorForValue();

    @Test
    void testMissingBeforeEvents() {
        VariableTracker<TestdataSolution> tracker = new VariableTracker<>(VARIABLE_DESCRIPTOR);

        TestdataEntity a = new TestdataEntity("a");
        TestdataEntity b = new TestdataEntity("b");

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
        VariableTracker<TestdataSolution> tracker = new VariableTracker<>(VARIABLE_DESCRIPTOR);

        TestdataEntity a = new TestdataEntity("a");
        TestdataEntity b = new TestdataEntity("b");

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
        VariableTracker<TestdataSolution> tracker = new VariableTracker<>(VARIABLE_DESCRIPTOR);

        TestdataEntity a = new TestdataEntity("a");
        TestdataEntity b = new TestdataEntity("b");

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
        VariableTracker<TestdataSolution> tracker = new VariableTracker<>(VARIABLE_DESCRIPTOR);

        TestdataEntity a = new TestdataEntity("a");
        TestdataEntity b = new TestdataEntity("b");

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
        VariableTracker<TestdataSolution> tracker = new VariableTracker<>(VARIABLE_DESCRIPTOR);

        TestdataEntity a = new TestdataEntity("a");
        TestdataEntity b = new TestdataEntity("b");

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

        VariableTracker<TestdataSolution> tracker = new VariableTracker<>(VARIABLE_DESCRIPTOR);
        VariableDescriptor<TestdataLavishSolution> otherVariableDescriptor =
                TestdataLavishEntity.buildVariableDescriptorForValue();

        TestdataEntity a = new TestdataEntity("a");
        TestdataLavishEntity b = new TestdataLavishEntity("b", new TestdataLavishEntityGroup("group"));

        tracker.beforeVariableChanged(null, a);
        tracker.afterVariableChanged(null, a);

        assertThat(tracker.getEntitiesMissingBeforeAfterEvents(List.of(
                new VariableId<>(VARIABLE_DESCRIPTOR, a),
                new VariableId<>((VariableDescriptor) otherVariableDescriptor, b)))).isEmpty();
    }
}
