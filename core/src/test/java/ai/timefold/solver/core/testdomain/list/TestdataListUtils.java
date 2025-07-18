package ai.timefold.solver.core.testdomain.list;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ai.timefold.solver.core.impl.domain.valuerange.descriptor.ValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.FromEntityPropertyValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.IterableFromEntityPropertyValueSelector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.preview.api.domain.metamodel.ElementPosition;
import ai.timefold.solver.core.preview.api.domain.metamodel.PositionInList;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListEntity;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListSolution;
import ai.timefold.solver.core.testdomain.list.pinned.index.TestdataPinnedWithIndexListValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.TestdataAllowsUnassignedValuesListValue;
import ai.timefold.solver.core.testdomain.list.unassignedvar.pinned.TestdataPinnedUnassignedValuesListEntity;
import ai.timefold.solver.core.testdomain.list.unassignedvar.pinned.TestdataPinnedUnassignedValuesListSolution;
import ai.timefold.solver.core.testdomain.list.unassignedvar.pinned.TestdataPinnedUnassignedValuesListValue;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.TestdataListEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.pinned.TestdataListPinnedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.pinned.TestdataListPinnedEntityProvidingSolution;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.TestdataListUnassignedEntityProvidingEntity;
import ai.timefold.solver.core.testdomain.list.valuerange.unassignedvar.TestdataListUnassignedEntityProvidingSolution;

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

    public static <Solution_> IterableValueSelector<Solution_> mockIterableValueSelector(
            ListVariableDescriptor<Solution_> listVariableDescriptor, Object... values) {
        return SelectorTestUtils.mockIterableValueSelector(listVariableDescriptor, values);
    }

    public static IterableValueSelector<TestdataListSolution> mockNeverEndingIterableValueSelector(
            ListVariableDescriptor<TestdataListSolution> listVariableDescriptor, TestdataListValue... values) {
        var valueSelector = mockIterableValueSelector(
                listVariableDescriptor, (Object[]) values);
        when(valueSelector.isNeverEnding()).thenReturn(true);
        when(valueSelector.iterator()).thenAnswer(invocation -> cyclicIterator(Arrays.asList(values)));
        return valueSelector;
    }

    public static IterableValueSelector<TestdataListEntityProvidingSolution>
            mockEntityRangeNeverEndingIterableValueSelector(
                    ListVariableDescriptor<TestdataListEntityProvidingSolution> listVariableDescriptor,
                    TestdataValue... values) {
        var valueSelector = mockIterableValueSelector(
                listVariableDescriptor, (Object[]) values);
        when(valueSelector.isNeverEnding()).thenReturn(true);
        when(valueSelector.iterator()).thenAnswer(invocation -> cyclicIterator(Arrays.asList(values)));
        return valueSelector;
    }

    public static IterableValueSelector<TestdataPinnedWithIndexListSolution>
            mockNeverEndingIterableValueSelector(
                    ListVariableDescriptor<TestdataPinnedWithIndexListSolution> listVariableDescriptor,
                    TestdataPinnedWithIndexListValue... values) {
        var valueSelector = mockIterableValueSelector(listVariableDescriptor, (Object[]) values);
        when(valueSelector.isNeverEnding()).thenReturn(true);
        when(valueSelector.iterator()).thenAnswer(invocation -> cyclicIterator(Arrays.asList(values)));
        return valueSelector;
    }

    public static IterableValueSelector<TestdataListPinnedEntityProvidingSolution>
            mockPinnedEntityRangeNeverEndingIterableValueSelector(
                    ListVariableDescriptor<TestdataListPinnedEntityProvidingSolution> listVariableDescriptor,
                    TestdataValue... values) {
        var valueSelector = mockIterableValueSelector(listVariableDescriptor, (Object[]) values);
        when(valueSelector.isNeverEnding()).thenReturn(true);
        when(valueSelector.iterator()).thenAnswer(invocation -> cyclicIterator(Arrays.asList(values)));
        return valueSelector;
    }

    public static IterableValueSelector<TestdataAllowsUnassignedValuesListSolution>
            mockNeverEndingIterableValueSelector(
                    ListVariableDescriptor<TestdataAllowsUnassignedValuesListSolution> listVariableDescriptor,
                    TestdataAllowsUnassignedValuesListValue... values) {
        var valueSelector = mockIterableValueSelector(
                listVariableDescriptor, (Object[]) values);
        when(valueSelector.isNeverEnding()).thenReturn(true);
        when(valueSelector.iterator()).thenAnswer(invocation -> cyclicIterator(Arrays.asList(values)));
        return valueSelector;
    }

    public static IterableValueSelector<TestdataListUnassignedEntityProvidingSolution>
            mockAllowsUnassignedEntityRangeNeverEndingIterableValueSelector(
                    ListVariableDescriptor<TestdataListUnassignedEntityProvidingSolution> listVariableDescriptor,
                    TestdataValue... values) {
        var valueSelector = mockIterableValueSelector(
                listVariableDescriptor, (Object[]) values);
        when(valueSelector.isNeverEnding()).thenReturn(true);
        when(valueSelector.iterator()).thenAnswer(invocation -> cyclicIterator(Arrays.asList(values)));
        return valueSelector;
    }

    public static IterableValueSelector<TestdataPinnedUnassignedValuesListSolution>
            mockNeverEndingIterableValueSelector(
                    ListVariableDescriptor<TestdataPinnedUnassignedValuesListSolution> listVariableDescriptor,
                    TestdataPinnedUnassignedValuesListValue... values) {
        var valueSelector = mockIterableValueSelector(
                listVariableDescriptor, (Object[]) values);
        when(valueSelector.isNeverEnding()).thenReturn(true);
        when(valueSelector.iterator()).thenAnswer(invocation -> cyclicIterator(Arrays.asList(values)));
        return valueSelector;
    }

    public static DestinationSelector<TestdataListSolution> mockNeverEndingDestinationSelector(
            ElementPosition... locationsInList) {
        return mockNeverEndingDestinationSelector(locationsInList.length, locationsInList);
    }

    public static DestinationSelector<TestdataListEntityProvidingSolution> mockEntityRangeNeverEndingDestinationSelector(
            ElementPosition... locationsInList) {
        return mockNeverEndingDestinationSelector(locationsInList.length, locationsInList);
    }

    public static DestinationSelector<TestdataPinnedWithIndexListSolution> mockPinnedNeverEndingDestinationSelector(
            PositionInList... locationsInList) {
        return mockNeverEndingDestinationSelector(locationsInList.length, locationsInList);
    }

    public static DestinationSelector<TestdataListPinnedEntityProvidingSolution>
            mockPinnedEntityRangeNeverEndingDestinationSelector(
                    PositionInList... locationsInList) {
        return mockNeverEndingDestinationSelector(locationsInList.length, locationsInList);
    }

    public static DestinationSelector<TestdataAllowsUnassignedValuesListSolution>
            mockAllowsUnassignedValuesNeverEndingDestinationSelector(
                    ElementPosition... locationsInList) {
        return mockNeverEndingDestinationSelector(locationsInList.length, locationsInList);
    }

    public static DestinationSelector<TestdataListUnassignedEntityProvidingSolution>
            mockAllowsUnassignedValuesEntityRangeNeverEndingDestinationSelector(
                    ElementPosition... locationsInList) {
        return mockNeverEndingDestinationSelector(locationsInList.length, locationsInList);
    }

    public static <Solution_> DestinationSelector<Solution_> mockNeverEndingDestinationSelector(long size,
            ElementPosition... locationsInList) {
        var destinationSelector = mock(DestinationSelector.class);
        when(destinationSelector.isCountable()).thenReturn(true);
        when(destinationSelector.isNeverEnding()).thenReturn(true);
        when(destinationSelector.getSize()).thenReturn(size);
        when(destinationSelector.iterator()).thenAnswer(invocation -> cyclicIterator(Arrays.asList(locationsInList)));
        return destinationSelector;
    }

    public static <Solution_> DestinationSelector<Solution_> mockDestinationSelector(ElementPosition... locationsInList) {
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

    public static ListVariableDescriptor<TestdataListEntityProvidingSolution> getEntityRangeListVariableDescriptor(
            InnerScoreDirector<TestdataListEntityProvidingSolution, ?> scoreDirector) {
        return (ListVariableDescriptor<TestdataListEntityProvidingSolution>) scoreDirector
                .getSolutionDescriptor()
                .getEntityDescriptorStrict(TestdataListEntityProvidingEntity.class)
                .getGenuineVariableDescriptor("valueList");
    }

    public static ListVariableDescriptor<TestdataListPinnedEntityProvidingSolution> getPinnedEntityRangeListVariableDescriptor(
            InnerScoreDirector<TestdataListPinnedEntityProvidingSolution, ?> scoreDirector) {
        return (ListVariableDescriptor<TestdataListPinnedEntityProvidingSolution>) scoreDirector
                .getSolutionDescriptor()
                .getEntityDescriptorStrict(TestdataListPinnedEntityProvidingEntity.class)
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

    public static ListVariableDescriptor<TestdataListUnassignedEntityProvidingSolution>
            getAllowsUnassignedvaluesEntityRangeListVariableDescriptor(
                    InnerScoreDirector<TestdataListUnassignedEntityProvidingSolution, ?> scoreDirector) {
        return (ListVariableDescriptor<TestdataListUnassignedEntityProvidingSolution>) scoreDirector
                .getSolutionDescriptor()
                .getEntityDescriptorStrict(TestdataListUnassignedEntityProvidingEntity.class)
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

    public static <T> IterableFromEntityPropertyValueSelector<T>
            getIterableFromEntityPropertyValueSelector(ValueRangeDescriptor<T> valueRangeDescriptor, boolean randomSelection) {
        var fromPropertySelector = new FromEntityPropertyValueSelector<>(valueRangeDescriptor, randomSelection);
        return new IterableFromEntityPropertyValueSelector<>(fromPropertySelector, randomSelection);
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
