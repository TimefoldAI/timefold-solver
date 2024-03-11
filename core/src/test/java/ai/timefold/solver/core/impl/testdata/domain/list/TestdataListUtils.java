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
import ai.timefold.solver.core.impl.heuristic.selector.list.ElementLocation;
import ai.timefold.solver.core.impl.heuristic.selector.list.LocationInList;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.pinned.TestdataPinnedUnassignedValuesListEntity;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.pinned.TestdataPinnedUnassignedValuesListSolution;
import ai.timefold.solver.core.impl.testdata.domain.list.allows_unassigned.pinned.TestdataPinnedUnassignedValuesListValue;
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

    public static int listSize(TestdataAllowsUnassignedValuesListEntity entity) {
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

    public static EntitySelector<TestdataAllowsUnassignedValuesListSolution>
            mockEntitySelector(TestdataAllowsUnassignedValuesListEntity... entities) {
        return SelectorTestUtils.mockEntitySelector(TestdataAllowsUnassignedValuesListEntity.buildEntityDescriptor(),
                (Object[]) entities);
    }

    public static EntitySelector<TestdataPinnedUnassignedValuesListSolution>
            mockNeverEndingEntitySelector(TestdataPinnedUnassignedValuesListEntity... entities) {
        return SelectorTestUtils.mockEntitySelector(TestdataPinnedUnassignedValuesListEntity.buildEntityDescriptor(),
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

    public static EntityIndependentValueSelector<TestdataPinnedWithIndexListSolution>
            mockNeverEndingEntityIndependentValueSelector(
                    ListVariableDescriptor<TestdataPinnedWithIndexListSolution> listVariableDescriptor,
                    TestdataPinnedWithIndexListValue... values) {
        var valueSelector = mockEntityIndependentValueSelector(listVariableDescriptor, (Object[]) values);
        when(valueSelector.isNeverEnding()).thenReturn(true);
        when(valueSelector.iterator()).thenAnswer(invocation -> cyclicIterator(Arrays.asList(values)));
        return valueSelector;
    }

    public static EntityIndependentValueSelector<TestdataAllowsUnassignedValuesListSolution>
            mockNeverEndingEntityIndependentValueSelector(
                    ListVariableDescriptor<TestdataAllowsUnassignedValuesListSolution> listVariableDescriptor,
                    TestdataAllowsUnassignedValuesListValue... values) {
        var valueSelector = mockEntityIndependentValueSelector(
                listVariableDescriptor, (Object[]) values);
        when(valueSelector.isNeverEnding()).thenReturn(true);
        when(valueSelector.iterator()).thenAnswer(invocation -> cyclicIterator(Arrays.asList(values)));
        return valueSelector;
    }

    public static EntityIndependentValueSelector<TestdataPinnedUnassignedValuesListSolution>
            mockNeverEndingEntityIndependentValueSelector(
                    ListVariableDescriptor<TestdataPinnedUnassignedValuesListSolution> listVariableDescriptor,
                    TestdataPinnedUnassignedValuesListValue... values) {
        var valueSelector = mockEntityIndependentValueSelector(
                listVariableDescriptor, (Object[]) values);
        when(valueSelector.isNeverEnding()).thenReturn(true);
        when(valueSelector.iterator()).thenAnswer(invocation -> cyclicIterator(Arrays.asList(values)));
        return valueSelector;
    }

    public static DestinationSelector<TestdataListSolution> mockNeverEndingDestinationSelector(
            ElementLocation... locationsInList) {
        return mockNeverEndingDestinationSelector(locationsInList.length, locationsInList);
    }

    public static DestinationSelector<TestdataPinnedWithIndexListSolution> mockPinnedNeverEndingDestinationSelector(
            LocationInList... locationsInList) {
        return mockNeverEndingDestinationSelector(locationsInList.length, locationsInList);
    }

    public static DestinationSelector<TestdataAllowsUnassignedValuesListSolution>
            mockAllowsUnassignedValuesNeverEndingDestinationSelector(
                    ElementLocation... locationsInList) {
        return mockNeverEndingDestinationSelector(locationsInList.length, locationsInList);
    }

    public static <Solution_> DestinationSelector<Solution_> mockNeverEndingDestinationSelector(long size,
            ElementLocation... locationsInList) {
        var destinationSelector = mock(DestinationSelector.class);
        when(destinationSelector.isCountable()).thenReturn(true);
        when(destinationSelector.isNeverEnding()).thenReturn(true);
        when(destinationSelector.getSize()).thenReturn(size);
        when(destinationSelector.iterator()).thenAnswer(invocation -> cyclicIterator(Arrays.asList(locationsInList)));
        return destinationSelector;
    }

    public static <Solution_> DestinationSelector<Solution_> mockDestinationSelector(ElementLocation... locationsInList) {
        DestinationSelector<Solution_> destinationSelector = mock(DestinationSelector.class);
        var refList = Arrays.asList(locationsInList);
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

    public static ListVariableDescriptor<TestdataAllowsUnassignedValuesListSolution>
            getAllowsUnassignedvaluesListVariableDescriptor(
                    InnerScoreDirector<TestdataAllowsUnassignedValuesListSolution, ?> scoreDirector) {
        return (ListVariableDescriptor<TestdataAllowsUnassignedValuesListSolution>) scoreDirector
                .getSolutionDescriptor()
                .getEntityDescriptorStrict(TestdataAllowsUnassignedValuesListEntity.class)
                .getGenuineVariableDescriptor("valueList");
    }

    public static ListVariableDescriptor<TestdataPinnedUnassignedValuesListSolution>
            getPinnedAllowsUnassignedvaluesListVariableDescriptor(
                    InnerScoreDirector<TestdataPinnedUnassignedValuesListSolution, ?> scoreDirector) {
        return (ListVariableDescriptor<TestdataPinnedUnassignedValuesListSolution>) scoreDirector
                .getSolutionDescriptor()
                .getEntityDescriptorStrict(TestdataPinnedUnassignedValuesListEntity.class)
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
                var element = elements.get(i % elements.size());
                i++;
                return element;
            }
        };
    }
}
