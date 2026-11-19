package ai.timefold.solver.core.impl.heuristic.selector.move.generic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.ListIterableSelector;
import ai.timefold.solver.core.impl.heuristic.selector.common.iterator.AbstractOriginalSwapIterator;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.pillar.PillarSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubListSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListChangeMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.ListSwapMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.RandomSubListChangeMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.generic.list.RandomSubListSwapMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;

import org.junit.jupiter.api.Test;

/**
 * Sizes that do not fit in a long saturate at {@link Long#MAX_VALUE} instead of wrapping to a negative value.
 */
@SuppressWarnings("unchecked")
class MoveSelectorSizeOverflowTest {

    // Its square does not fit in a long.
    private static final long HUGE = 4_000_000_000L;

    private static final ListVariableDescriptor<TestdataListSolution> LIST_VARIABLE_DESCRIPTOR =
            TestdataListEntity.buildVariableDescriptorForValueList();

    private static <T> T sized(Class<?> type, long size) {
        var selector = mock(type);
        if (selector instanceof EntitySelector<?> entitySelector) {
            when(entitySelector.getSize()).thenReturn(size);
        } else if (selector instanceof IterableValueSelector<?> valueSelector) {
            when(valueSelector.getSize()).thenReturn(size);
            when(valueSelector.getVariableDescriptor()).thenReturn((ListVariableDescriptor) LIST_VARIABLE_DESCRIPTOR);
        } else if (selector instanceof PillarSelector<?> pillarSelector) {
            when(pillarSelector.getSize()).thenReturn(size);
        } else if (selector instanceof DestinationSelector<?> destinationSelector) {
            when(destinationSelector.getSize()).thenReturn(size);
        } else if (selector instanceof SubListSelector<?> subListSelector) {
            when(subListSelector.getSize()).thenReturn(size);
            when(subListSelector.getVariableDescriptor()).thenReturn((ListVariableDescriptor) LIST_VARIABLE_DESCRIPTOR);
        } else if (selector instanceof ListIterableSelector<?, ?> listIterableSelector) {
            when(listIterableSelector.getSize()).thenReturn(size);
        }
        return (T) selector;
    }

    @Test
    void changeMoveSelectorWithIterableValues() {
        var moveSelector = new ChangeMoveSelector<TestdataSolution>(sized(EntitySelector.class, HUGE),
                sized(IterableValueSelector.class, HUGE), false);
        assertThat(moveSelector.getSize()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void changeMoveSelectorWithEntityDependentValues() {
        var entitySelector = (EntitySelector<TestdataSolution>) mock(EntitySelector.class);
        when(entitySelector.endingIterator()).thenAnswer(invocation -> List.of("a", "b").iterator());
        var valueSelector = (ValueSelector<TestdataSolution>) mock(ValueSelector.class);
        when(valueSelector.getSize(any())).thenReturn(Long.MAX_VALUE / 2 + 1);
        var moveSelector = new ChangeMoveSelector<>(entitySelector, valueSelector, false);
        assertThat(moveSelector.getSize()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void pillarChangeMoveSelector() {
        var moveSelector = new PillarChangeMoveSelector<TestdataSolution>(sized(PillarSelector.class, HUGE),
                sized(IterableValueSelector.class, HUGE), false);
        assertThat(moveSelector.getSize()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void originalSwapSize() {
        ListIterableSelector<TestdataSolution, Object> left = sized(ListIterableSelector.class, HUGE);
        ListIterableSelector<TestdataSolution, Object> right = sized(ListIterableSelector.class, HUGE);
        assertThat(AbstractOriginalSwapIterator.getSize(left, right)).isEqualTo(Long.MAX_VALUE);
        ListIterableSelector<TestdataSolution, Object> same = sized(ListIterableSelector.class, 5_000_000_000L);
        assertThat(AbstractOriginalSwapIterator.getSize(same, same)).isEqualTo(Long.MAX_VALUE);
        ListIterableSelector<TestdataSolution, Object> even = sized(ListIterableSelector.class, 10L);
        assertThat(AbstractOriginalSwapIterator.getSize(even, even)).isEqualTo(45L);
        ListIterableSelector<TestdataSolution, Object> odd = sized(ListIterableSelector.class, 11L);
        assertThat(AbstractOriginalSwapIterator.getSize(odd, odd)).isEqualTo(55L);
    }

    @Test
    void listChangeMoveSelector() {
        var moveSelector = new ListChangeMoveSelector<TestdataListSolution>(sized(IterableValueSelector.class, HUGE),
                sized(DestinationSelector.class, HUGE), false);
        assertThat(moveSelector.getSize()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void listSwapMoveSelector() {
        var moveSelector = new ListSwapMoveSelector<TestdataListSolution>(sized(IterableValueSelector.class, HUGE),
                sized(IterableValueSelector.class, HUGE), false);
        assertThat(moveSelector.getSize()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void randomSubListChangeMoveSelector() {
        var moveSelector = new RandomSubListChangeMoveSelector<TestdataListSolution>(sized(SubListSelector.class, HUGE),
                sized(DestinationSelector.class, Long.MAX_VALUE / HUGE), true);
        assertThat(moveSelector.getSize()).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void randomSubListSwapMoveSelector() {
        var moveSelector = new RandomSubListSwapMoveSelector<TestdataListSolution>(sized(SubListSelector.class, HUGE),
                sized(SubListSelector.class, Long.MAX_VALUE / HUGE), true);
        assertThat(moveSelector.getSize()).isEqualTo(Long.MAX_VALUE);
    }

}
