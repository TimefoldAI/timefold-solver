package ai.timefold.solver.core.impl.domain.variable.declarative;

import static ai.timefold.solver.core.impl.domain.variable.declarative.SortedChangeBatch.sortKey;
import static ai.timefold.solver.core.impl.domain.variable.declarative.SortedChangeBatch.sortKeyIndex;
import static ai.timefold.solver.core.impl.domain.variable.declarative.SortedChangeBatch.sortKeyPosition;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import ai.timefold.solver.core.impl.domain.variable.ListVariableState;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;

import org.junit.jupiter.api.Test;

class SortedChangeBatchTest {

    @Test
    void decodesIndexAndPosition() {
        for (var index : new int[] { 0, 1, 5, Integer.MAX_VALUE }) {
            for (var position : new int[] { 0, 1, 1000, Integer.MAX_VALUE }) {
                var sortKey = sortKey(index, position);
                assertThat(sortKeyIndex(sortKey)).isEqualTo(index);
                assertThat(sortKeyPosition(sortKey)).isEqualTo(position);
            }
        }
    }

    @Test
    void sortsByIndexThenByPosition() {
        // Ties keep the insertion order.
        var sortKeyArray = new long[] {
                sortKey(3, 0),
                sortKey(2, 1),
                sortKey(0, 2),
                sortKey(2, 3),
                sortKey(3, 4),
                sortKey(1, 5)
        };
        Arrays.sort(sortKeyArray);
        assertThat(Arrays.stream(sortKeyArray).mapToInt(SortedChangeBatch::sortKeyPosition))
                .containsExactly(2, 5, 1, 3, 0, 4);
    }

    @SuppressWarnings("unchecked")
    @Test
    void backwardBatchReadsInDescendingIndexOrder() {
        var ownerA = "A";
        var ownerB = "B";
        ListVariableState<?, Object, Object> listVariableState = mock(ListVariableState.class);
        when(listVariableState.getElementPosition("a1")).thenReturn(ElementPosition.of(ownerA, 1));
        when(listVariableState.getElementPosition("b2")).thenReturn(ElementPosition.of(ownerB, 2));
        when(listVariableState.getElementPosition("u")).thenReturn(ElementPosition.unassigned());
        when(listVariableState.getElementPosition("a3")).thenReturn(ElementPosition.of(ownerA, 3));
        when(listVariableState.getElementPosition("a0")).thenReturn(ElementPosition.of(ownerA, 0));

        var batch = new SortedChangeBatch(false);
        batch.load(List.of("a1", "b2", "u", "a3", "a0"), listVariableState);

        assertThat(IntStream.range(0, batch.size()).mapToObj(batch::entity))
                .containsExactly("a3", "b2", "a1", "a0", "u");
        assertThat(IntStream.range(0, batch.size()).map(batch::index))
                .containsExactly(3, 2, 1, 0, 0);

        var lastProcessedIndex = batch.lastProcessedIndexToAdvance(0);
        assertThat(lastProcessedIndex).isNotNull();
        lastProcessedIndex.setValue(1); // The walk from a3 stopped at a1.
        assertThat(batch.lastProcessedIndexToAdvance(2)).isNull();
        assertThat(batch.lastProcessedIndexToAdvance(3)).isSameAs(lastProcessedIndex);
    }

}
