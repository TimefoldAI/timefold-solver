package ai.timefold.solver.core.impl.heuristic.selector.value;

import static ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicyTestUtils.buildHeuristicConfigPolicy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.AbstractValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.valuerange.descriptor.FromEntityPropertyValueRangeDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.SelectorTestUtils;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionProbabilityWeightFactory;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.AssignedListValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.FilteringValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.IterableFromEntityPropertyValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.ProbabilityValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.ShufflingValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.UnassignedListValueSelector;
import ai.timefold.solver.core.impl.phase.scope.AbstractPhaseScope;
import ai.timefold.solver.core.impl.phase.scope.AbstractStepScope;
import ai.timefold.solver.core.impl.score.director.ScoreDirector;
import ai.timefold.solver.core.impl.solver.ClassInstanceCache;
import ai.timefold.solver.core.impl.solver.scope.SolverScope;
import ai.timefold.solver.core.testdomain.TestdataEntity;
import ai.timefold.solver.core.testdomain.TestdataSolution;
import ai.timefold.solver.core.testdomain.TestdataValue;
import ai.timefold.solver.core.testdomain.common.DummyValueComparatorFactory;
import ai.timefold.solver.core.testdomain.list.TestdataListEntity;
import ai.timefold.solver.core.testdomain.list.TestdataListSolution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ValueSelectorFactoryTest {

    @Test
    void phaseOriginal() {
        HeuristicConfigPolicy configPolicy = buildHeuristicConfigPolicy();
        EntityDescriptor entityDescriptor = configPolicy.getSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataEntity.class);
        ValueSelectorConfig valueSelectorConfig = new ValueSelectorConfig()
                .withCacheType(SelectionCacheType.PHASE)
                .withSelectionOrder(SelectionOrder.ORIGINAL);
        ValueSelector valueSelector = ValueSelectorFactory.create(valueSelectorConfig).buildValueSelector(configPolicy,
                entityDescriptor, SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM);
        assertThat(valueSelector).isInstanceOf(IterableFromSolutionPropertyValueSelector.class)
                .isNotInstanceOf(ShufflingValueSelector.class);
        assertThat(valueSelector.getCacheType()).isEqualTo(SelectionCacheType.PHASE);
    }

    @Test
    void stepOriginal() {
        HeuristicConfigPolicy configPolicy = buildHeuristicConfigPolicy();
        EntityDescriptor entityDescriptor = configPolicy.getSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataEntity.class);
        ValueSelectorConfig valueSelectorConfig = new ValueSelectorConfig()
                .withCacheType(SelectionCacheType.STEP)
                .withSelectionOrder(SelectionOrder.ORIGINAL);
        ValueSelector valueSelector = ValueSelectorFactory.create(valueSelectorConfig).buildValueSelector(configPolicy,
                entityDescriptor, SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM);
        assertThat(valueSelector).isInstanceOf(IterableFromSolutionPropertyValueSelector.class)
                .isNotInstanceOf(ShufflingValueSelector.class);
        // PHASE instead of STEP because these values are cacheable, so there's no reason not to cache them?
        assertThat(valueSelector.getCacheType()).isEqualTo(SelectionCacheType.PHASE);
    }

    @Test
    void justInTimeOriginal() {
        HeuristicConfigPolicy configPolicy = buildHeuristicConfigPolicy();
        EntityDescriptor entityDescriptor = configPolicy.getSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataEntity.class);
        ValueSelectorConfig valueSelectorConfig = new ValueSelectorConfig()
                .withCacheType(SelectionCacheType.JUST_IN_TIME)
                .withSelectionOrder(SelectionOrder.ORIGINAL);
        ValueSelector valueSelector = ValueSelectorFactory.create(valueSelectorConfig).buildValueSelector(configPolicy,
                entityDescriptor, SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM);
        assertThat(valueSelector).isInstanceOf(IterableFromSolutionPropertyValueSelector.class);
        // cacheType gets upgraded to STEP
        // assertEquals(SelectionCacheType.JUST_IN_TIME, valueSelector.getCacheType());
    }

    @Test
    void phaseRandom() {
        HeuristicConfigPolicy configPolicy = buildHeuristicConfigPolicy();
        EntityDescriptor entityDescriptor = configPolicy.getSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataEntity.class);
        ValueSelectorConfig valueSelectorConfig = new ValueSelectorConfig()
                .withCacheType(SelectionCacheType.PHASE)
                .withSelectionOrder(SelectionOrder.RANDOM);
        ValueSelector valueSelector = ValueSelectorFactory.create(valueSelectorConfig).buildValueSelector(configPolicy,
                entityDescriptor, SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM);
        assertThat(valueSelector)
                .isInstanceOf(IterableFromSolutionPropertyValueSelector.class)
                .isNotInstanceOf(ShufflingValueSelector.class);
        assertThat(valueSelector.getCacheType()).isEqualTo(SelectionCacheType.PHASE);
    }

    @Test
    void stepRandom() {
        HeuristicConfigPolicy configPolicy = buildHeuristicConfigPolicy();
        EntityDescriptor entityDescriptor = configPolicy.getSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataEntity.class);
        ValueSelectorConfig valueSelectorConfig = new ValueSelectorConfig()
                .withCacheType(SelectionCacheType.STEP)
                .withSelectionOrder(SelectionOrder.RANDOM);
        ValueSelector valueSelector = ValueSelectorFactory.create(valueSelectorConfig).buildValueSelector(configPolicy,
                entityDescriptor, SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM);
        assertThat(valueSelector).isInstanceOf(IterableFromSolutionPropertyValueSelector.class)
                .isNotInstanceOf(ShufflingValueSelector.class);
        // PHASE instead of STEP because these values are cacheable, so there's no reason not to cache them?
        assertThat(valueSelector.getCacheType()).isEqualTo(SelectionCacheType.PHASE);
    }

    @Test
    void justInTimeRandom() {
        HeuristicConfigPolicy configPolicy = buildHeuristicConfigPolicy();
        EntityDescriptor entityDescriptor = configPolicy.getSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataEntity.class);
        ValueSelectorConfig valueSelectorConfig = new ValueSelectorConfig()
                .withCacheType(SelectionCacheType.JUST_IN_TIME)
                .withSelectionOrder(SelectionOrder.RANDOM);
        ValueSelector valueSelector = ValueSelectorFactory.create(valueSelectorConfig).buildValueSelector(configPolicy,
                entityDescriptor, SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM);
        assertThat(valueSelector).isInstanceOf(IterableFromSolutionPropertyValueSelector.class);
        // cacheType gets upgraded to STEP
        // assertEquals(SelectionCacheType.JUST_IN_TIME, valueSelector.getCacheType());
    }

    @Test
    void phaseShuffled() {
        HeuristicConfigPolicy configPolicy = buildHeuristicConfigPolicy();
        EntityDescriptor entityDescriptor = configPolicy.getSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataEntity.class);
        ValueSelectorConfig valueSelectorConfig = new ValueSelectorConfig()
                .withCacheType(SelectionCacheType.PHASE)
                .withSelectionOrder(SelectionOrder.SHUFFLED);
        ValueSelector valueSelector = ValueSelectorFactory.create(valueSelectorConfig).buildValueSelector(configPolicy,
                entityDescriptor, SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM);
        assertThat(valueSelector).isInstanceOf(ShufflingValueSelector.class);
        assertThat(((ShufflingValueSelector) valueSelector).getChildValueSelector())
                .isInstanceOf(IterableFromSolutionPropertyValueSelector.class);
        assertThat(valueSelector.getCacheType()).isEqualTo(SelectionCacheType.PHASE);
    }

    @Test
    void stepShuffled() {
        HeuristicConfigPolicy configPolicy = buildHeuristicConfigPolicy();
        EntityDescriptor entityDescriptor = configPolicy.getSolutionDescriptor()
                .findEntityDescriptorOrFail(TestdataEntity.class);
        ValueSelectorConfig valueSelectorConfig = new ValueSelectorConfig()
                .withCacheType(SelectionCacheType.STEP)
                .withSelectionOrder(SelectionOrder.SHUFFLED);
        ValueSelector valueSelector = ValueSelectorFactory.create(valueSelectorConfig).buildValueSelector(configPolicy,
                entityDescriptor, SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM);
        assertThat(valueSelector).isInstanceOf(ShufflingValueSelector.class);
        assertThat(((ShufflingValueSelector) valueSelector).getChildValueSelector())
                .isInstanceOf(IterableFromSolutionPropertyValueSelector.class);
        assertThat(valueSelector.getCacheType()).isEqualTo(SelectionCacheType.STEP);
    }

    @Test
    void justInTimeShuffled() {
        HeuristicConfigPolicy configPolicy = buildHeuristicConfigPolicy();
        EntityDescriptor entityDescriptor =
                configPolicy.getSolutionDescriptor().findEntityDescriptorOrFail(TestdataEntity.class);
        ValueSelectorConfig valueSelectorConfig = new ValueSelectorConfig()
                .withCacheType(SelectionCacheType.JUST_IN_TIME)
                .withSelectionOrder(SelectionOrder.SHUFFLED);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ValueSelectorFactory.create(valueSelectorConfig).buildValueSelector(configPolicy,
                        entityDescriptor, SelectionCacheType.JUST_IN_TIME, SelectionOrder.RANDOM));
    }

    @Test
    void applyFiltering_withFilterClass() {
        ValueSelectorConfig valueSelectorConfig = new ValueSelectorConfig()
                .withFilterClass(DummyValueFilter.class);
        ValueSelector baseValueSelector =
                SelectorTestUtils.mockValueSelector(TestdataEntity.class, "value", new TestdataValue("v1"));
        ValueSelector resultingValueSelector =
                ValueSelectorFactory.create(valueSelectorConfig).applyFiltering(baseValueSelector, ClassInstanceCache.create());
        assertThat(resultingValueSelector).isExactlyInstanceOf(FilteringValueSelector.class);
    }

    @Test
    void applyProbability_withSelectionProbabilityWeightFactory() {
        ValueSelectorConfig valueSelectorConfig = new ValueSelectorConfig()
                .withProbabilityWeightFactoryClass(DummySelectionProbabilityWeightFactory.class);
        ValueSelector baseValueSelector = mock(IterableValueSelector.class);
        ValueSelectorFactory valueSelectorFactory =
                ValueSelectorFactory.create(valueSelectorConfig);
        valueSelectorFactory.validateProbability(SelectionOrder.PROBABILISTIC);
        ValueSelector resultingValueSelector = valueSelectorFactory.applyProbability(SelectionCacheType.PHASE,
                SelectionOrder.PROBABILISTIC, baseValueSelector, ClassInstanceCache.create());
        assertThat(resultingValueSelector).isExactlyInstanceOf(ProbabilityValueSelector.class);
    }

    @Test
    void applySorting_withComparatorClass() {
        ValueSelectorConfig valueSelectorConfig = new ValueSelectorConfig()
                .withCacheType(SelectionCacheType.PHASE)
                .withComparatorClass(DummyValueComparator.class);
        applySorting(valueSelectorConfig, true);
        applySorting(valueSelectorConfig, false);
    }

    @Test
    void applySorting_withComparatorFactoryClass() {
        ValueSelectorConfig valueSelectorConfig = new ValueSelectorConfig()
                .withCacheType(SelectionCacheType.PHASE)
                .withComparatorFactoryClass(DummyValueComparatorFactory.class);
        applySorting(valueSelectorConfig, true);
        applySorting(valueSelectorConfig, false);
    }

    private void applySorting(ValueSelectorConfig valueSelectorConfig, boolean canExtractValueRangeFromSolution) {
        ValueSelectorFactory valueSelectorFactory =
                ValueSelectorFactory.create(valueSelectorConfig);
        valueSelectorFactory.validateSorting(SelectionOrder.SORTED);
        EntityDescriptor entityDescriptor = mock(EntityDescriptor.class);
        GenuineVariableDescriptor variableDescriptor = mock(GenuineVariableDescriptor.class);
        when(entityDescriptor.getGenuineVariableDescriptorList()).thenReturn(List.of(variableDescriptor));
        AbstractValueRangeDescriptor valueRangeDescriptor = mock(FromEntityPropertyValueRangeDescriptor.class);
        when(variableDescriptor.getValueRangeDescriptor()).thenReturn(valueRangeDescriptor);
        when(valueRangeDescriptor.getVariableDescriptor()).thenReturn(variableDescriptor);
        when(valueRangeDescriptor.canExtractValueRangeFromSolution()).thenReturn(canExtractValueRangeFromSolution);

        if (canExtractValueRangeFromSolution) {
            IterableFromSolutionPropertyValueSelector baseValueSelector =
                    (IterableFromSolutionPropertyValueSelector) valueSelectorFactory.buildValueSelector(
                            buildHeuristicConfigPolicy(),
                            entityDescriptor, SelectionCacheType.PHASE, SelectionOrder.SORTED);
            assertThat(baseValueSelector.getSelectionSorter()).isNotNull();
            assertThat(baseValueSelector.getCacheType()).isEqualTo(SelectionCacheType.PHASE);
        } else {
            IterableFromEntityPropertyValueSelector baseValueSelector =
                    (IterableFromEntityPropertyValueSelector) valueSelectorFactory.buildValueSelector(
                            buildHeuristicConfigPolicy(),
                            entityDescriptor, SelectionCacheType.PHASE, SelectionOrder.SORTED);
            assertThat(baseValueSelector.getChildValueSelector().getSelectionSorter()).isNotNull();
            assertThat(baseValueSelector.getCacheType()).isEqualTo(SelectionCacheType.PHASE);
        }
    }

    @Test
    void failFast_ifMimicRecordingIsUsedWithOtherProperty() {
        ValueSelectorConfig valueSelectorConfig = new ValueSelectorConfig()
                .withSelectedCountLimit(10L)
                .withMimicSelectorRef("someSelectorId");

        assertThatIllegalArgumentException().isThrownBy(
                () -> ValueSelectorFactory.create(valueSelectorConfig)
                        .buildMimicReplaying(mock(HeuristicConfigPolicy.class)))
                .withMessageContaining("has another property");
    }

    static Stream<Arguments> applyListValueFiltering() {
        return Stream.of(
                arguments(true, ValueSelectorFactory.ListValueFilteringType.ACCEPT_ASSIGNED, AssignedListValueSelector.class),
                arguments(true, ValueSelectorFactory.ListValueFilteringType.ACCEPT_UNASSIGNED,
                        UnassignedListValueSelector.class),
                arguments(true, ValueSelectorFactory.ListValueFilteringType.NONE,
                        DummyIterableValueSelector.class),
                arguments(false, ValueSelectorFactory.ListValueFilteringType.ACCEPT_ASSIGNED,
                        DummyIterableValueSelector.class),
                arguments(false, ValueSelectorFactory.ListValueFilteringType.ACCEPT_UNASSIGNED,
                        DummyIterableValueSelector.class),
                arguments(false, ValueSelectorFactory.ListValueFilteringType.NONE,
                        DummyIterableValueSelector.class));
    }

    @ParameterizedTest
    @MethodSource
    void applyListValueFiltering(
            boolean isUnassignedValuesAllowed,
            ValueSelectorFactory.ListValueFilteringType listValueFilteringType,
            Class<? extends ValueSelector> expectedValueSelectorClass) {
        HeuristicConfigPolicy<TestdataListSolution> configPolicy = mock(HeuristicConfigPolicy.class);
        when(configPolicy.isUnassignedValuesAllowed()).thenReturn(isUnassignedValuesAllowed);

        DummyIterableValueSelector baseValueSelector = new DummyIterableValueSelector();
        GenuineVariableDescriptor<TestdataListSolution> variableDescriptor = baseValueSelector.getVariableDescriptor();

        ValueSelector<TestdataListSolution> valueSelector =
                ValueSelectorFactory.<TestdataListSolution> create(new ValueSelectorConfig())
                        .applyListValueFiltering(configPolicy, listValueFilteringType, variableDescriptor, baseValueSelector);

        assertThat(valueSelector).isExactlyInstanceOf(expectedValueSelectorClass);
    }

    public static class DummyValueFilter implements SelectionFilter<TestdataSolution, TestdataValue> {
        @Override
        public boolean accept(ScoreDirector<TestdataSolution> scoreDirector, TestdataValue selection) {
            return true;
        }
    }

    public static class DummySelectionProbabilityWeightFactory
            implements SelectionProbabilityWeightFactory<TestdataSolution, TestdataValue> {

        @Override
        public double createProbabilityWeight(ScoreDirector<TestdataSolution> scoreDirector, TestdataValue selection) {
            return 0.0;
        }
    }

    public static class DummyValueComparator implements Comparator<TestdataValue> {
        @Override
        public int compare(TestdataValue testdataValue, TestdataValue testdataValue2) {
            return 0;
        }
    }

    private static class DummyIterableValueSelector implements IterableValueSelector<TestdataListSolution> {

        @Override
        public long getSize() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Object> iterator() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isNeverEnding() {
            return false;
        }

        @Override
        public SelectionCacheType getCacheType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public GenuineVariableDescriptor<TestdataListSolution> getVariableDescriptor() {
            return TestdataListEntity.buildVariableDescriptorForValueList();
        }

        @Override
        public long getSize(Object entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Object> iterator(Object entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Iterator<Object> endingIterator(Object entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void phaseStarted(AbstractPhaseScope<TestdataListSolution> phaseScope) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void stepStarted(AbstractStepScope<TestdataListSolution> stepScope) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void stepEnded(AbstractStepScope<TestdataListSolution> stepScope) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void phaseEnded(AbstractPhaseScope<TestdataListSolution> phaseScope) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void solvingStarted(SolverScope<TestdataListSolution> solverScope) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void solvingEnded(SolverScope<TestdataListSolution> solverScope) {
            throw new UnsupportedOperationException();
        }
    }
}
