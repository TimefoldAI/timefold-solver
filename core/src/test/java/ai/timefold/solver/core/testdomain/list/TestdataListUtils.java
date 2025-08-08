package ai.timefold.solver.core.testdomain.list;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.DestinationSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.solution.descriptor.SolutionDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.ValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.ListVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.value.FromEntityPropertyValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.IterableValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.FilteringValueRangeSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.IterableFromEntityPropertyValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.mimic.MimicRecordingValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.mimic.MimicReplayingValueSelector;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.ClassInstanceCache;
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

    public static <Solution_, Entity_> EntitySelector<Solution_>
            mockEntitySelector(EntityDescriptor<Solution_> entityDescriptor, Entity_... entities) {
        return SelectorTestUtils.mockEntitySelector(entityDescriptor, entities);
    }

    public static EntitySelector<TestdataListEntityProvidingSolution>
            mockEntitySelector(TestdataListEntityProvidingEntity... entities) {
        return SelectorTestUtils.mockEntitySelector(
                TestdataListEntityProvidingEntity.buildEntityDescriptor(), (Object[]) entities);
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

    public static EntitySelector<TestdataListUnassignedEntityProvidingSolution>
            mockEntitySelector(TestdataListUnassignedEntityProvidingEntity... entities) {
        return SelectorTestUtils.mockEntitySelector(TestdataListUnassignedEntityProvidingEntity.buildEntityDescriptor(),
                (Object[]) entities);
    }

    public static <Solution_> IterableValueSelector<Solution_> mockIterableValueSelector(
            ListVariableDescriptor<Solution_> listVariableDescriptor, Object... values) {
        return SelectorTestUtils.mockIterableValueSelector(listVariableDescriptor, values);
    }

    public static <Solution_> IterableFromEntityPropertyValueSelector<Solution_>
            mockIterableFromEntityPropertyValueSelector(IterableValueSelector<Solution_> childMoveSelector,
                    boolean randomSelection) {
        var fromEntityValueSelector = mock(FromEntityPropertyValueSelector.class);
        doReturn(childMoveSelector.iterator()).when(fromEntityValueSelector).iterator(any());
        doReturn(childMoveSelector.getVariableDescriptor()).when(fromEntityValueSelector).getVariableDescriptor();
        doReturn(childMoveSelector.isCountable()).when(fromEntityValueSelector).isCountable();
        return new IterableFromEntityPropertyValueSelector(fromEntityValueSelector, randomSelection);
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

    public static <Solution_> ListVariableDescriptor<Solution_> getListVariableDescriptor(
            InnerScoreDirector<Solution_, ?> scoreDirector) {
        return (ListVariableDescriptor<Solution_>) scoreDirector
                .getSolutionDescriptor()
                .getGenuineEntityDescriptors()
                .iterator()
                .next()
                .getGenuineVariableDescriptor("valueList");
    }

    public static <Solution_> BasicVariableDescriptor<Solution_> getBasicVariableDescriptor(
            InnerScoreDirector<Solution_, ?> scoreDirector) {
        return (BasicVariableDescriptor<Solution_>) scoreDirector
                .getSolutionDescriptor()
                .getGenuineEntityDescriptors()
                .iterator()
                .next()
                .getGenuineVariableDescriptor("value");
    }

    public static <Solution_> EntityDescriptor<Solution_> getEntityDescriptor(
            InnerScoreDirector<Solution_, ?> scoreDirector) {
        return scoreDirector
                .getSolutionDescriptor()
                .getGenuineEntityDescriptors()
                .iterator()
                .next();
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

    public static <T> MimicRecordingValueSelector<T>
            getMimicRecordingIterableValueSelector(ValueRangeDescriptor<T> valueRangeDescriptor,
                    boolean randomSelection) {
        var valueSelector = getIterableFromEntityPropertyValueSelector(valueRangeDescriptor, randomSelection);
        return new MimicRecordingValueSelector<>(valueSelector);
    }

    public static <T> MimicRecordingValueSelector<T>
            getMimicRecordingIterableValueSelector(ListVariableDescriptor<T> listVariableDescriptor, Object... values) {
        var valueSelector = mockIterableValueSelector(listVariableDescriptor, values);
        return new MimicRecordingValueSelector<>(valueSelector);
    }

    public static <T> FilteringValueRangeSelector<T>
            getFilteringValueRangeSelector(MimicRecordingValueSelector<T> mimicRecordingValueSelector,
                    IterableValueSelector<T> nonReplaying,
                    boolean randomSelection, boolean assertBothSides, boolean generateIterableFromEntityProperty) {
        var replayingValueSelector = new MimicReplayingValueSelector<>(mimicRecordingValueSelector);
        if (generateIterableFromEntityProperty) {
            var iterableEntityPropertyValueSelector =
                    mockIterableFromEntityPropertyValueSelector(nonReplaying, randomSelection);
            // Ensure OptimizedRandomFilteringValueRangeIterator is created for random iterators
            return new FilteringValueRangeSelector<>(iterableEntityPropertyValueSelector, replayingValueSelector,
                    randomSelection, assertBothSides);
        } else {
            return new FilteringValueRangeSelector<>(nonReplaying, replayingValueSelector, randomSelection,
                    assertBothSides);
        }
    }

    public static <T, V> DestinationSelector<T> getEntityValueRangeDestinationSelector(
            MimicRecordingValueSelector<T> mimicRecordingValueSelector, SolutionDescriptor<T> solutionDescriptor,
            EntityDescriptor<V> entityDescriptor, boolean randomSelection) {
        var destinationSelectorConfig = new DestinationSelectorConfig();
        destinationSelectorConfig.setEntitySelectorConfig(new EntitySelectorConfig()
                .withEntityClass(entityDescriptor.getEntityClass()));
        destinationSelectorConfig.setValueSelectorConfig(new ValueSelectorConfig()
                .withVariableName(entityDescriptor.getGenuineListVariableDescriptor().getVariableName()));
        var configPolicy = mock(HeuristicConfigPolicy.class);
        doReturn(solutionDescriptor).when(configPolicy).getSolutionDescriptor();
        doReturn(mimicRecordingValueSelector).when(configPolicy).getValueMimicRecorder(any());
        return DestinationSelectorFactory.<T> create(destinationSelectorConfig)
                .buildDestinationSelector(configPolicy, SelectionCacheType.JUST_IN_TIME, randomSelection, true, "any");
    }

    public static <S> DestinationSelector<S> getEntityValueRangeDestinationSelector(
            MimicRecordingValueSelector<S> innerMimicRecordingValueSelector, SolutionDescriptor<S> solutionDescriptor,
            EntityDescriptor<S> entityDescriptor, Class<? extends SelectionFilter> selectionFilterClass,
            boolean randomSelection) {
        var destinationSelectorConfig = new DestinationSelectorConfig();
        destinationSelectorConfig.setEntitySelectorConfig(new EntitySelectorConfig()
                .withEntityClass(entityDescriptor.getEntityClass()));
        destinationSelectorConfig.setValueSelectorConfig(new ValueSelectorConfig()
                .withFilterClass(selectionFilterClass)
                .withVariableName(entityDescriptor.getGenuineListVariableDescriptor().getVariableName()));
        var configPolicy = mock(HeuristicConfigPolicy.class);
        doReturn(solutionDescriptor).when(configPolicy).getSolutionDescriptor();
        doReturn(innerMimicRecordingValueSelector).when(configPolicy).getValueMimicRecorder(any());
        doReturn(ClassInstanceCache.create()).when(configPolicy).getClassInstanceCache();
        return DestinationSelectorFactory.<S> create(destinationSelectorConfig)
                .buildDestinationSelector(configPolicy, SelectionCacheType.JUST_IN_TIME, randomSelection, true, "any");
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
