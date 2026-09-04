package ai.timefold.solver.core.impl.move;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListValue;

import org.junit.jupiter.api.Test;

class PendingListChangeTrackerTest {

    private ListVariableBeforeChangeAction<?, ?, ?> mockAction() {
        return mock(ListVariableBeforeChangeAction.class);
    }

    @Test
    void putThenRemove_singleSlot_noEscalation() {
        var tracker = new PendingListChangeTracker();
        var entity = new TestdataListEntity("e", new TestdataListValue("v"));
        var action = mockAction();

        tracker.put(entity, action);

        assertThat(tracker.remove(entity)).isSameAs(action);
        // Consumed: a second remove for the same entity finds nothing.
        assertThat(tracker.remove(entity)).isNull();
    }

    @Test
    void sameEntityReopeningAFreshBracket_overwritesWithoutEscalating() {
        var tracker = new PendingListChangeTracker();
        var entity = new TestdataListEntity("e", new TestdataListValue("v"));
        var action1 = mockAction();
        var action2 = mockAction();

        tracker.put(entity, action1);
        assertThat(tracker.remove(entity)).isSameAs(action1); // Bracket 1 resolved.
        tracker.put(entity, action2); // Bracket 2 reopens the same entity.

        // Still resolvable as a single slot - the previous, already-resolved bracket for the same
        // entity must not have forced an unnecessary escalation to the map.
        assertThat(tracker.remove(entity)).isSameAs(action2);
    }

    @Test
    void secondDistinctEntity_escalatesToMap() {
        var tracker = new PendingListChangeTracker();
        var entityA = new TestdataListEntity("a", new TestdataListValue("v"));
        var entityB = new TestdataListEntity("b", new TestdataListValue("v"));
        var actionA = mockAction();
        var actionB = mockAction();

        tracker.put(entityA, actionA);
        tracker.put(entityB, actionB); // entityA is still pending: forces escalation.

        assertThat(tracker.remove(entityA)).isSameAs(actionA);
        assertThat(tracker.remove(entityB)).isSameAs(actionB);
    }

    @Test
    void removeForUnknownEntity_returnsNull() {
        var tracker = new PendingListChangeTracker();
        var entity = new TestdataListEntity("e", new TestdataListValue("v"));

        assertThat(tracker.remove(entity)).isNull();
    }

    @Test
    void clear_removesEveryPendingEntry() {
        var tracker = new PendingListChangeTracker();
        var entityA = new TestdataListEntity("a", new TestdataListValue("v"));
        var entityB = new TestdataListEntity("b", new TestdataListValue("v"));
        tracker.put(entityA, mockAction());
        tracker.put(entityB, mockAction()); // Escalated to map.

        tracker.clear();

        assertThat(tracker.remove(entityA)).isNull();
        assertThat(tracker.remove(entityB)).isNull();
    }

    @Test
    void clear_alsoResetsTheSingleSlotBeforeAnyEscalation() {
        var tracker = new PendingListChangeTracker();
        var entity = new TestdataListEntity("e", new TestdataListValue("v"));
        tracker.put(entity, mockAction());

        tracker.clear();

        assertThat(tracker.remove(entity)).isNull();
    }

}
