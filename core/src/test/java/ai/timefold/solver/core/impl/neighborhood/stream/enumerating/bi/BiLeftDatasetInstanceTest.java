package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.bi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.impl.bavet.common.tuple.BiTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDataset;

import org.junit.jupiter.api.Test;

class BiLeftDatasetInstanceTest {

    @Test
    void keyedLookupDoesNotMoveTheSlotsHeldByARetiringIterator() {
        var instance = new BiLeftDatasetInstance<Object, String, String>(mock(AbstractDataset.class), 0);
        // A gap, as after a retract.
        var retractedTuple = BiTuple.<String, String> of("other", "gap", 1);
        instance.insert(retractedTuple);
        var bList = List.of("b0", "b1", "b2", "b3", "b4");
        for (var b : bList) {
            instance.insert(BiTuple.of("key", b, 1));
        }
        instance.retract(retractedTuple);
        var random = new Random(0);
        // A move iterator of the same step retires a left element, which it remembers by its slot.
        var leftIterator = instance.retiringRandomIterator(random);
        var retiredB = leftIterator.next().getB();
        leftIterator.retire();

        // The keyed lookup of PillarSwapMoveIterator builds an index of the whole dataset.
        instance.randomIterator("key", random);

        var drawnBs = new HashSet<String>();
        for (var i = 0; i < 1_000 && leftIterator.hasNext(); i++) {
            drawnBs.add(leftIterator.next().getB());
        }
        assertThat(drawnBs).containsExactlyInAnyOrderElementsOf(bList.stream().filter(b -> !b.equals(retiredB)).toList());
    }

}
