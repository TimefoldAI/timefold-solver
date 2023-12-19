package ai.timefold.solver.core.impl.testdata.domain.list;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.ElementRef;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.pinned.index.TestdataPinnedWithIndexListValue;

public final class TestdataListUtils {

    private TestdataListUtils() {
    }

    public static int listSize(TestdataListEntity entity) {
        return entity.getValueList().size();
    }

    public static int listSize(TestdataPinnedWithIndexListEntity entity) {
        return entity.getValueList().size();
    }

    public static EntitySelector<TestdataListSolution> mockEntitySelector(TestdataListEntity... entities) {
        return SelectorTestUtils.mockEntitySelector(TestdataListEntity.buildEntityDescriptor(), (Object[]) entities);
    }

    public static EntitySelector<TestdataPinnedWithIndexListSolution>
            mockEntitySelector(TestdataPinnedWithIndexListEntity... entities) {
        return SelectorTestUtils.mockEntitySelector(TestdataPinnedWithIndexListEntity.buildEntityDescriptor(),
                (Object[]) entities);
    }

    public static <Solution_> EntityIndependentValueSelector<Solution_> mockEntityIndependentValueSelector(
            ListVariableDescriptor<Solution_> listVariableDescriptor, Object... values) {
        return SelectorTestUtils.mockEntityIndependentValueSelector(listVariableDescriptor, values);
    }

    public static EntityIndependentValueSelector<TestdataListSolution> mockNeverEndingEntityIndependentValueSelector(
            ListVariableDescriptor<TestdataListSolution> listVariableDescriptor, TestdataListValue... values) {
        var valueSelector = mockEntityIndependentValueSelector(
                listVariableDescriptor, (Object[]) values);
        when(valueSelector.isNeverEnding()).thenReturn(true);
        when(valueSelector.iterator()).thenAnswer(invocation -> cyclicIterator(Arrays.asList(values)));
        return valueSelector;
    }

    public static EntityIndependentValueSelector<TestdataPinnedWithIndexListSolution> mockNeverEndingEntityIndependentValueSelector(
            ListVariableDescriptor<TestdataPinnedWithIndexListSolution> listVariableDescriptor, TestdataPinnedWithIndexListValue... values) {
        var valueSelector = mockEntityIndependentValueSelector(listVariableDescriptor, (Object[]) values);
        when(valueSelector.isNeverEnding()).thenReturn(true);
        when(valueSelector.iterator()).thenAnswer(invocation -> cyclicIterator(Arrays.asList(values)));
        return valueSelector;
    }

    public static DestinationSelector<TestdataListSolution> mockNeverEndingDestinationSelector(
            ElementRef... elementRefs) {
        return mockNeverEndingDestinationSelector(elementRefs.length, elementRefs);
    }

    public static DestinationSelector<TestdataListSolution> mockNeverEndingDestinationSelector(long size,
            ElementRef... elementRefs) {
        DestinationSelector<TestdataListSolution> destinationSelector = mock(DestinationSelector.class);
        when(destinationSelector.isCountable()).thenReturn(true);
        when(destinationSelector.isNeverEnding()).thenReturn(true);
        when(destinationSelector.getSize()).thenReturn(size);
        when(destinationSelector.iterator()).thenAnswer(invocation -> cyclicIterator(Arrays.asList(elementRefs)));
        return destinationSelector;
    }

    public static <Solution_> DestinationSelector<Solution_> mockDestinationSelector(ElementRef... elementRefs) {
        DestinationSelector<Solution_> destinationSelector = mock(DestinationSelector.class);
        List<ElementRef> refList = Arrays.asList(elementRefs);
        when(destinationSelector.isCountable()).thenReturn(true);
        when(destinationSelector.isNeverEnding()).thenReturn(false);
        when(destinationSelector.getSize()).thenReturn((long) refList.size());
        when(destinationSelector.iterator()).thenAnswer(invocation -> refList.iterator());
        return destinationSelector;
    }

    public static ListVariableDescriptor<TestdataListSolution> getListVariableDescriptor(
            InnerScoreDirector<TestdataListSolution, ?> scoreDirector) {
        return (ListVariableDescriptor<TestdataListSolution>) scoreDirector
                .getSolutionDescriptor()
                .getEntityDescriptorStrict(TestdataListEntity.class)
                .getGenuineVariableDescriptor("valueList");
    }

    public static ListVariableDescriptor<TestdataPinnedWithIndexListSolution> getPinnedListVariableDescriptor(
            InnerScoreDirector<TestdataPinnedWithIndexListSolution, ?> scoreDirector) {
        return (ListVariableDescriptor<TestdataPinnedWithIndexListSolution>) scoreDirector
                .getSolutionDescriptor()
                .getEntityDescriptorStrict(TestdataPinnedWithIndexListEntity.class)
                .getGenuineVariableDescriptor("valueList");
    }

    private static <T> Iterator<T> cyclicIterator(List<T> elements) {
        if (elements.isEmpty()) {
            return Collections.emptyIterator();
        }
        if (elements.size() == 1) {
            return new Iterator<>() {

                private final T element = elements.get(0);

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public T next() {
                    return element;
                }
            };
        }
        return new Iterator<>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public T next() {
                T element = elements.get(i % elements.size());
                i++;
                return element;
            }
        };
    }
}
