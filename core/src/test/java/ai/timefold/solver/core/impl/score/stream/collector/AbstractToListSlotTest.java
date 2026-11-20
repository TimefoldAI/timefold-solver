package ai.timefold.solver.core.impl.score.stream.collector;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

class AbstractToListSlotTest {

    @Test
    void resultKeepsInsertionOrder() {
        var state = new AbstractToListSlot.State<String>();
        var slotList = new ArrayList<TestSlot>();
        for (var i = 0; i < 10; i++) {
            var slot = new TestSlot(state);
            slot.addMapped("e" + i);
            slotList.add(slot);
        }
        slotList.get(2).removeMapped();
        slotList.get(5).removeMapped();

        assertThat(state.result()).containsExactly("e0", "e1", "e3", "e4", "e6", "e7", "e8", "e9");
    }

    private static final class TestSlot extends AbstractToListSlot<String> {

        TestSlot(State<String> state) {
            super(state);
        }

    }

}
