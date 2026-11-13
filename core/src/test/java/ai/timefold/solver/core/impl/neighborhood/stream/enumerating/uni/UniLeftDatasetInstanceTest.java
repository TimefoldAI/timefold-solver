package ai.timefold.solver.core.impl.neighborhood.stream.enumerating.uni;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import ai.timefold.solver.core.impl.bavet.common.tuple.UniTuple;
import ai.timefold.solver.core.impl.neighborhood.stream.enumerating.common.AbstractDataset;

import org.junit.jupiter.api.Test;

class UniLeftDatasetInstanceTest {

    @Test
    void iteratingDoesNotMoveTheSlotsHeldByARetiringIterator() {
        var instance = new UniLeftDatasetInstance<Object, String>(mock(AbstractDataset.class), 0);
        // One gap among six slots, as after a retract in settle().
        var retractedTuple = UniTuple.of("gap", 1);
        instance.insert(retractedTuple);
        var aList = List.of("a0", "a1", "a2", "a3", "a4");
        for (var a : aList) {
            instance.insert(UniTuple.of(a, 1));
        }
        instance.retract(retractedTuple);
        var random = new Random(0);
        var leftIterator = instance.retiringRandomIterator(random);
        var retiredA = leftIterator.next().getA();
        leftIterator.retire();

        // JustInTimeBiDatasetInstance.size() iterates the left dataset like this, during the step.
        var iterated = new ArrayList<String>();
        for (var tuple : instance) {
            iterated.add(tuple.getA());
        }
        assertThat(iterated).containsExactlyElementsOf(aList);

        var drawnAs = new HashSet<String>();
        for (var i = 0; i < 1_000 && leftIterator.hasNext(); i++) {
            drawnAs.add(leftIterator.next().getA());
        }
        assertThat(drawnAs).containsExactlyInAnyOrderElementsOf(aList.stream().filter(a -> !a.equals(retiredA)).toList());
    }

}
