package ai.timefold.solver.core.impl.heuristic.selector.value;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.common.decorator.SelectionSorterOrder;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.BasicVariableDescriptor;
import ai.timefold.solver.core.impl.domain.variable.descriptor.GenuineVariableDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.ComparatorFactorySelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.ComparatorSelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionProbabilityWeightFactory;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.AssignedListValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.CachingValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.DowncastingValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.FilteringValueRangeSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.FilteringValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.FromEntitySortingValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.InitializedValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.IterableFromEntityPropertyValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.ProbabilityValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.ReinitializeVariableValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.SelectedCountLimitValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.ShufflingValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.SortingValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.decorator.UnassignedListValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.mimic.MimicRecordingValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.mimic.MimicReplayingValueSelector;
import ai.timefold.solver.core.impl.solver.ClassInstanceCache;

public class ValueSelectorFactory<Solution_>
        extends AbstractSelectorFactory<Solution_, ValueSelectorConfig> {

    public static <Solution_> ValueSelectorFactory<Solution_> create(ValueSelectorConfig valueSelectorConfig) {
        return new ValueSelectorFactory<>(valueSelectorConfig);
    }

    public ValueSelectorFactory(ValueSelectorConfig valueSelectorConfig) {
        super(valueSelectorConfig);
    }

    public GenuineVariableDescriptor<Solution_> extractVariableDescriptor(HeuristicConfigPolicy<Solution_> configPolicy,
            EntityDescriptor<Solution_> entityDescriptor) {
        var variableName = config.getVariableName();
        var mimicSelectorRef = config.getMimicSelectorRef();
        if (variableName != null) {
            return getVariableDescriptorForName(downcastEntityDescriptor(configPolicy, entityDescriptor), variableName);
        } else if (mimicSelectorRef != null) {
            return configPolicy.getValueMimicRecorder(mimicSelectorRef).getVariableDescriptor();
        } else {
            return null;
        }
    }

    public ValueSelector<Solution_> buildValueSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            EntityDescriptor<Solution_> entityDescriptor, SelectionCacheType minimumCacheType,
            SelectionOrder inheritedSelectionOrder) {
        return buildValueSelector(configPolicy, entityDescriptor, minimumCacheType, inheritedSelectionOrder,
                configPolicy.isReinitializeVariableFilterEnabled(), ListValueFilteringType.NONE);
    }

    public ValueSelector<Solution_> buildValueSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            EntityDescriptor<Solution_> entityDescriptor, SelectionCacheType minimumCacheType,
            SelectionOrder inheritedSelectionOrder, boolean applyReinitializeVariableFiltering,
            ListValueFilteringType listValueFilteringType) {
        return buildValueSelector(configPolicy, entityDescriptor, minimumCacheType, inheritedSelectionOrder,
                applyReinitializeVariableFiltering, listValueFilteringType, null, false);
    }

    /**
     * @param configPolicy never null
     * @param entityDescriptor never null
     * @param minimumCacheType never null, If caching is used (different from {@link SelectionCacheType#JUST_IN_TIME}),
     *        then it should be at least this {@link SelectionCacheType} because an ancestor already uses such caching
     *        and less would be pointless.
     * @param inheritedSelectionOrder never null
     * @param applyReinitializeVariableFiltering the reinitialization flag
     * @param listValueFilteringType the list filtering type
     * @param entityValueRangeRecorderId the recorder id to be used to create a replaying selector when enabling entity value
     *        range
     * @param assertBothSides a flag used by the entity value range filtering select to enable different types of validations
     * @return never null
     */
    public ValueSelector<Solution_> buildValueSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            EntityDescriptor<Solution_> entityDescriptor, SelectionCacheType minimumCacheType,
            SelectionOrder inheritedSelectionOrder, boolean applyReinitializeVariableFiltering,
            ListValueFilteringType listValueFilteringType, String entityValueRangeRecorderId, boolean assertBothSides) {
        var variableDescriptor = deduceGenuineVariableDescriptor(downcastEntityDescriptor(configPolicy, entityDescriptor),
                config.getVariableName());
        if (config.getMimicSelectorRef() != null) {
            var valueSelector = buildMimicReplaying(configPolicy);
            valueSelector =
                    applyReinitializeVariableFiltering(applyReinitializeVariableFiltering, variableDescriptor, valueSelector);
            valueSelector = applyDowncasting(valueSelector);
            return valueSelector;
        }
        var resolvedCacheType = SelectionCacheType.resolve(config.getCacheType(), minimumCacheType);
        var resolvedSelectionOrder = SelectionOrder.resolve(config.getSelectionOrder(), inheritedSelectionOrder);

        var nearbySelectionConfig = config.getNearbySelectionConfig();
        if (nearbySelectionConfig != null) {
            nearbySelectionConfig.validateNearby(resolvedCacheType, resolvedSelectionOrder);
        }
        validateCacheTypeVersusSelectionOrder(resolvedCacheType, resolvedSelectionOrder, entityValueRangeRecorderId != null);
        validateSorting(resolvedSelectionOrder);
        validateProbability(resolvedSelectionOrder);
        validateSelectedLimit(minimumCacheType);

        // baseValueSelector and lower should be SelectionOrder.ORIGINAL if they are going to get cached completely
        var randomSelection = determineBaseRandomSelection(variableDescriptor, resolvedCacheType, resolvedSelectionOrder);
        var valueSelector =
                buildBaseValueSelector(variableDescriptor, SelectionCacheType.max(minimumCacheType, resolvedCacheType),
                        randomSelection);
        var instanceCache = configPolicy.getClassInstanceCache();
        if (nearbySelectionConfig != null) {
            // TODO Static filtering (such as movableEntitySelectionFilter) should affect nearbySelection too
            valueSelector = applyNearbySelection(configPolicy, entityDescriptor, minimumCacheType,
                    resolvedSelectionOrder, valueSelector);
        } else {
            /*
             * The nearby selector will implement its own logic to filter out unreachable elements.
             * Therefore, we only apply entity value range filtering if the nearby feature is not enabled;
             * otherwise, we would end up applying the filtering logic twice.
             */
            valueSelector = applyValueRangeFiltering(configPolicy, valueSelector, entityDescriptor, minimumCacheType,
                    inheritedSelectionOrder, randomSelection, entityValueRangeRecorderId, assertBothSides);
        }
        valueSelector = applyFiltering(valueSelector, instanceCache);
        valueSelector = applyInitializedChainedValueFilter(configPolicy, variableDescriptor, valueSelector);
        valueSelector = applySorting(resolvedCacheType, resolvedSelectionOrder, valueSelector, instanceCache);
        valueSelector = applyProbability(resolvedCacheType, resolvedSelectionOrder, valueSelector, instanceCache);
        valueSelector = applyShuffling(resolvedCacheType, resolvedSelectionOrder, valueSelector);
        valueSelector = applyCaching(resolvedCacheType, resolvedSelectionOrder, valueSelector);
        valueSelector = applySelectedLimit(valueSelector);
        valueSelector = applyListValueFiltering(configPolicy, listValueFilteringType, variableDescriptor, valueSelector);
        valueSelector = applyMimicRecording(configPolicy, valueSelector);
        valueSelector =
                applyReinitializeVariableFiltering(applyReinitializeVariableFiltering, variableDescriptor, valueSelector);
        valueSelector = applyDowncasting(valueSelector);
        return valueSelector;
    }

    protected ValueSelector<Solution_> buildMimicReplaying(HeuristicConfigPolicy<Solution_> configPolicy) {
        if (config.getId() != null
                || config.getCacheType() != null
                || config.getSelectionOrder() != null
                || config.getNearbySelectionConfig() != null
                || config.getFilterClass() != null
                || config.getSorterManner() != null
                || determineComparatorClass(config) != null
                || determineComparatorFactoryClass(config) != null
                || config.getSorterOrder() != null
                || config.getSorterClass() != null
                || config.getProbabilityWeightFactoryClass() != null
                || config.getSelectedCountLimit() != null) {
            throw new IllegalArgumentException(
                    "The valueSelectorConfig (%s) with mimicSelectorRef (%s) has another property that is not null."
                            .formatted(config, config.getMimicSelectorRef()));
        }
        var valueMimicRecorder = configPolicy.getValueMimicRecorder(config.getMimicSelectorRef());
        if (valueMimicRecorder == null) {
            throw new IllegalArgumentException(
                    "The valueSelectorConfig (%s) has a mimicSelectorRef (%s) for which no valueSelector with that id exists (in its solver phase)."
                            .formatted(config, config.getMimicSelectorRef()));
        }
        return new MimicReplayingValueSelector<>(valueMimicRecorder);
    }

    protected EntityDescriptor<Solution_> downcastEntityDescriptor(HeuristicConfigPolicy<Solution_> configPolicy,
            EntityDescriptor<Solution_> entityDescriptor) {
        var downcastEntityClass = config.getDowncastEntityClass();
        if (downcastEntityClass != null) {
            var parentEntityClass = entityDescriptor.getEntityClass();
            if (!parentEntityClass.isAssignableFrom(downcastEntityClass)) {
                throw new IllegalStateException(
                        "The downcastEntityClass (%s) is not a subclass of the parentEntityClass (%s) configured by the %s."
                                .formatted(downcastEntityClass, parentEntityClass, EntitySelector.class.getSimpleName()));
            }
            var solutionDescriptor = configPolicy.getSolutionDescriptor();
            entityDescriptor = solutionDescriptor.getEntityDescriptorStrict(downcastEntityClass);
            if (entityDescriptor == null) {
                throw new IllegalArgumentException("""
                        The selectorConfig (%s) has an downcastEntityClass (%s) that is not a known planning entity.
                        Check your solver configuration. If that class (%s) is not in the entityClassSet (%s), \
                        check your @%s implementation's annotated methods too."""
                        .formatted(config, downcastEntityClass, downcastEntityClass.getSimpleName(),
                                solutionDescriptor.getEntityClassSet(), PlanningSolution.class.getSimpleName()));
            }
        }
        return entityDescriptor;
    }

    protected boolean determineBaseRandomSelection(GenuineVariableDescriptor<Solution_> variableDescriptor,
            SelectionCacheType resolvedCacheType, SelectionOrder resolvedSelectionOrder) {
        return switch (resolvedSelectionOrder) {
            case ORIGINAL, SORTED, SHUFFLED, PROBABILISTIC ->
                // baseValueSelector and lower should be ORIGINAL if they are going to get cached completely
                false;
            case RANDOM ->
                // Predict if caching will occur
                resolvedCacheType.isNotCached() || !hasFiltering(variableDescriptor);
            default -> throw new IllegalStateException("The selectionOrder (" + resolvedSelectionOrder
                    + ") is not implemented.");
        };
    }

    private static String determineComparatorPropertyName(ValueSelectorConfig valueSelectorConfig) {
        var sorterComparatorClass = valueSelectorConfig.getSorterComparatorClass();
        var comparatorClass = valueSelectorConfig.getComparatorClass();
        if (sorterComparatorClass != null && comparatorClass != null) {
            throw new IllegalArgumentException(
                    "The valueSelectorConfig (%s) cannot have a %s (%s) and %s (%s) at the same time.".formatted(
                            valueSelectorConfig, "sorterComparatorClass", sorterComparatorClass,
                            "comparatorClass", comparatorClass));
        }
        return sorterComparatorClass != null ? "sorterComparatorClass" : "comparatorClass";
    }

    private static Class<? extends Comparator> determineComparatorClass(ValueSelectorConfig valueSelectorConfig) {
        var propertyName = determineComparatorPropertyName(valueSelectorConfig);
        if (propertyName.equals("sorterComparatorClass")) {
            return valueSelectorConfig.getSorterComparatorClass();
        } else {
            return valueSelectorConfig.getComparatorClass();
        }
    }

    private static String determineComparatorFactoryPropertyName(ValueSelectorConfig valueSelectorConfig) {
        var weightFactoryClass = valueSelectorConfig.getSorterWeightFactoryClass();
        var comparatorFactoryClass = valueSelectorConfig.getComparatorFactoryClass();
        if (weightFactoryClass != null && comparatorFactoryClass != null) {
            throw new IllegalArgumentException(
                    "The valueSelectorConfig (%s) cannot have a %s (%s) and %s (%s) at the same time.".formatted(
                            valueSelectorConfig, "sorterWeightFactoryClass", weightFactoryClass,
                            "comparatorFactoryClass", comparatorFactoryClass));
        }
        return weightFactoryClass != null ? "sorterWeightFactoryClass" : "comparatorFactoryClass";
    }

    private static Class<? extends ComparatorFactory> determineComparatorFactoryClass(ValueSelectorConfig valueSelectorConfig) {
        var propertyName = determineComparatorFactoryPropertyName(valueSelectorConfig);
        if (propertyName.equals("sorterWeightFactoryClass")) {
            return valueSelectorConfig.getSorterWeightFactoryClass();
        } else {
            return valueSelectorConfig.getComparatorFactoryClass();
        }
    }

    private ValueSelector<Solution_> buildBaseValueSelector(GenuineVariableDescriptor<Solution_> variableDescriptor,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        var valueRangeDescriptor = variableDescriptor.getValueRangeDescriptor();
        // TODO minimumCacheType SOLVER is only a problem if the valueRange includes entities or custom weird cloning
        if (minimumCacheType == SelectionCacheType.SOLVER) {
            // TODO Solver cached entities are not compatible with ConstraintStreams and IncrementalScoreDirector
            // because between phases the entities get cloned
            throw new IllegalArgumentException("The minimumCacheType (" + minimumCacheType
                    + ") is not yet supported. Please use " + SelectionCacheType.PHASE + " instead.");
        }
        if (valueRangeDescriptor.canExtractValueRangeFromSolution()) {
            return new IterableFromSolutionPropertyValueSelector<>(valueRangeDescriptor, minimumCacheType, randomSelection);
        } else {
            // TODO Do not allow PHASE cache on FromEntityPropertyValueSelector, except if the moveSelector is PHASE cached too.
            var fromEntityPropertySelector = new FromEntityPropertyValueSelector<>(valueRangeDescriptor, randomSelection);
            return new IterableFromEntityPropertyValueSelector<>(fromEntityPropertySelector, randomSelection);
        }
    }

    private boolean hasFiltering(GenuineVariableDescriptor<Solution_> variableDescriptor) {
        return config.getFilterClass() != null ||
                (variableDescriptor instanceof BasicVariableDescriptor<Solution_> basicVariableDescriptor
                        && basicVariableDescriptor.hasMovableChainedTrailingValueFilter());
    }

    protected ValueSelector<Solution_> applyFiltering(ValueSelector<Solution_> valueSelector,
            ClassInstanceCache instanceCache) {
        var variableDescriptor = valueSelector.getVariableDescriptor();
        if (hasFiltering(variableDescriptor)) {
            List<SelectionFilter<Solution_, Object>> filterList = new ArrayList<>(config.getFilterClass() == null ? 1 : 2);
            if (config.getFilterClass() != null) {
                filterList.add(instanceCache.newInstance(config, "filterClass", config.getFilterClass()));
            }
            // Filter out pinned entities
            if (variableDescriptor instanceof BasicVariableDescriptor<Solution_> basicVariableDescriptor
                    && basicVariableDescriptor.hasMovableChainedTrailingValueFilter()) {
                filterList.add(basicVariableDescriptor.getMovableChainedTrailingValueFilter());
            }
            valueSelector = FilteringValueSelector.of(valueSelector, SelectionFilter.compose(filterList));
        }
        return valueSelector;
    }

    protected ValueSelector<Solution_> applyInitializedChainedValueFilter(HeuristicConfigPolicy<Solution_> configPolicy,
            GenuineVariableDescriptor<Solution_> variableDescriptor, ValueSelector<Solution_> valueSelector) {
        var isChained = variableDescriptor instanceof BasicVariableDescriptor<Solution_> basicVariableDescriptor
                && basicVariableDescriptor.isChained();
        if (configPolicy.isInitializedChainedValueFilterEnabled() && isChained) {
            valueSelector = InitializedValueSelector.create(valueSelector);
        }
        return valueSelector;
    }

    protected void validateSorting(SelectionOrder resolvedSelectionOrder) {
        var sorterManner = config.getSorterManner();
        var comparatorPropertyName = determineComparatorPropertyName(config);
        var comparatorClass = determineComparatorClass(config);
        var comparatorFactoryPropertyName = determineComparatorFactoryPropertyName(config);
        var comparatorFactoryClass = determineComparatorFactoryClass(config);
        var sorterOrder = config.getSorterOrder();
        var sorterClass = config.getSorterClass();
        if ((sorterManner != null || comparatorClass != null || comparatorFactoryClass != null
                || sorterOrder != null || sorterClass != null) && resolvedSelectionOrder != SelectionOrder.SORTED) {
            throw new IllegalArgumentException("""
                    The valueSelectorConfig (%s) with sorterManner (%s) \
                    and %s (%s) and %s (%s) and sorterOrder (%s) and sorterClass (%s) \
                    has a resolvedSelectionOrder (%s) that is not %s."""
                    .formatted(config, sorterManner, comparatorPropertyName, comparatorClass, comparatorFactoryPropertyName,
                            comparatorFactoryClass, sorterOrder, sorterClass, resolvedSelectionOrder,
                            SelectionOrder.SORTED));
        }
        assertNotSorterMannerAnd(config, comparatorPropertyName, ValueSelectorFactory::determineComparatorClass);
        assertNotSorterMannerAnd(config, comparatorFactoryPropertyName,
                ValueSelectorFactory::determineComparatorFactoryClass);
        assertNotSorterMannerAnd(config, "sorterClass", ValueSelectorConfig::getSorterClass);
        assertNotSorterMannerAnd(config, "sorterOrder", ValueSelectorConfig::getSorterOrder);
        assertNotSorterClassAnd(config, comparatorPropertyName, ValueSelectorFactory::determineComparatorClass);
        assertNotSorterClassAnd(config, comparatorFactoryPropertyName,
                ValueSelectorFactory::determineComparatorFactoryClass);
        assertNotSorterClassAnd(config, "sorterOrder", ValueSelectorConfig::getSorterOrder);
        if (comparatorClass != null && comparatorFactoryClass != null) {
            throw new IllegalArgumentException(
                    "The valueSelectorConfig (%s) has both a %s (%s) and a %s (%s)."
                            .formatted(config, comparatorPropertyName, comparatorClass, comparatorFactoryPropertyName,
                                    comparatorFactoryClass));
        }
    }

    private static void assertNotSorterMannerAnd(ValueSelectorConfig config, String propertyName,
            Function<ValueSelectorConfig, Object> propertyAccessor) {
        var sorterManner = config.getSorterManner();
        var property = propertyAccessor.apply(config);
        if (sorterManner != null && property != null) {
            throw new IllegalArgumentException("The entitySelectorConfig (%s) has both a sorterManner (%s) and a %s (%s)."
                    .formatted(config, sorterManner, propertyName, property));
        }
    }

    private static void assertNotSorterClassAnd(ValueSelectorConfig config, String propertyName,
            Function<ValueSelectorConfig, Object> propertyAccessor) {
        var sorterClass = config.getSorterClass();
        var property = propertyAccessor.apply(config);
        if (sorterClass != null && property != null) {
            throw new IllegalArgumentException(
                    "The entitySelectorConfig (%s) with sorterClass (%s) has a non-null %s (%s)."
                            .formatted(config, sorterClass, propertyName, property));
        }
    }

    protected ValueSelector<Solution_> applySorting(SelectionCacheType resolvedCacheType, SelectionOrder resolvedSelectionOrder,
            ValueSelector<Solution_> valueSelector, ClassInstanceCache instanceCache) {
        if (resolvedSelectionOrder == SelectionOrder.SORTED) {
            SelectionSorter<Solution_, Object> sorter;
            var sorterManner = config.getSorterManner();
            var comparatorClass = determineComparatorClass(config);
            var comparatorFactoryClass = determineComparatorFactoryClass(config);
            if (sorterManner != null) {
                var variableDescriptor = valueSelector.getVariableDescriptor();
                if (!ValueSelectorConfig.hasSorter(sorterManner, variableDescriptor)) {
                    return valueSelector;
                }
                sorter = ValueSelectorConfig.determineSorter(sorterManner, variableDescriptor);
            } else if (comparatorClass != null) {
                Comparator<Object> sorterComparator =
                        instanceCache.newInstance(config, determineComparatorPropertyName(config), comparatorClass);
                sorter = new ComparatorSelectionSorter<>(sorterComparator,
                        SelectionSorterOrder.resolve(config.getSorterOrder()));
            } else if (comparatorFactoryClass != null) {
                var comparatorFactory = instanceCache.newInstance(config, determineComparatorFactoryPropertyName(config),
                        comparatorFactoryClass);
                sorter = new ComparatorFactorySelectionSorter<>(comparatorFactory,
                        SelectionSorterOrder.resolve(config.getSorterOrder()));
            } else if (config.getSorterClass() != null) {
                sorter = instanceCache.newInstance(config, "sorterClass", config.getSorterClass());
            } else {
                throw new IllegalArgumentException("""
                        The valueSelectorConfig (%s) with resolvedSelectionOrder (%s) needs \
                        a sorterManner (%s) or a %s (%s) or a %s (%s) \
                        or a sorterClass (%s)."""
                        .formatted(config, resolvedSelectionOrder, sorterManner, determineComparatorPropertyName(config),
                                comparatorClass, determineComparatorFactoryPropertyName(config), comparatorFactoryClass,
                                config.getSorterClass()));
            }
            if (!valueSelector.getVariableDescriptor().canExtractValueRangeFromSolution()
                    && resolvedCacheType == SelectionCacheType.STEP) {
                valueSelector = new FromEntitySortingValueSelector<>(valueSelector, resolvedCacheType, sorter);
            } else {
                if (!(valueSelector instanceof IterableValueSelector)) {
                    throw new IllegalArgumentException("The valueSelectorConfig (" + config
                            + ") with resolvedCacheType (" + resolvedCacheType
                            + ") and resolvedSelectionOrder (" + resolvedSelectionOrder
                            + ") needs to be based on an "
                            + IterableValueSelector.class.getSimpleName() + " (" + valueSelector + ")."
                            + " Check your @" + ValueRangeProvider.class.getSimpleName() + " annotations.");
                }
                valueSelector = new SortingValueSelector<>((IterableValueSelector<Solution_>) valueSelector,
                        resolvedCacheType, sorter);
            }
        }
        return valueSelector;
    }

    protected void validateProbability(SelectionOrder resolvedSelectionOrder) {
        if (config.getProbabilityWeightFactoryClass() != null
                && resolvedSelectionOrder != SelectionOrder.PROBABILISTIC) {
            throw new IllegalArgumentException("The valueSelectorConfig (" + config
                    + ") with probabilityWeightFactoryClass (" + config.getProbabilityWeightFactoryClass()
                    + ") has a resolvedSelectionOrder (" + resolvedSelectionOrder
                    + ") that is not " + SelectionOrder.PROBABILISTIC + ".");
        }
    }

    protected ValueSelector<Solution_> applyProbability(SelectionCacheType resolvedCacheType,
            SelectionOrder resolvedSelectionOrder, ValueSelector<Solution_> valueSelector, ClassInstanceCache instanceCache) {
        if (resolvedSelectionOrder == SelectionOrder.PROBABILISTIC) {
            if (config.getProbabilityWeightFactoryClass() == null) {
                throw new IllegalArgumentException("The valueSelectorConfig (" + config
                        + ") with resolvedSelectionOrder (" + resolvedSelectionOrder
                        + ") needs a probabilityWeightFactoryClass ("
                        + config.getProbabilityWeightFactoryClass() + ").");
            }
            SelectionProbabilityWeightFactory<Solution_, Object> probabilityWeightFactory = instanceCache.newInstance(config,
                    "probabilityWeightFactoryClass", config.getProbabilityWeightFactoryClass());
            if (!(valueSelector instanceof IterableValueSelector)) {
                throw new IllegalArgumentException("The valueSelectorConfig (" + config
                        + ") with resolvedCacheType (" + resolvedCacheType
                        + ") and resolvedSelectionOrder (" + resolvedSelectionOrder
                        + ") needs to be based on an "
                        + IterableValueSelector.class.getSimpleName() + " (" + valueSelector + ")."
                        + " Check your @" + ValueRangeProvider.class.getSimpleName() + " annotations.");
            }
            valueSelector = new ProbabilityValueSelector<>((IterableValueSelector<Solution_>) valueSelector,
                    resolvedCacheType, probabilityWeightFactory);
        }
        return valueSelector;
    }

    private ValueSelector<Solution_> applyShuffling(SelectionCacheType resolvedCacheType,
            SelectionOrder resolvedSelectionOrder, ValueSelector<Solution_> valueSelector) {
        if (resolvedSelectionOrder == SelectionOrder.SHUFFLED) {
            if (!(valueSelector instanceof IterableValueSelector)) {
                throw new IllegalArgumentException("The valueSelectorConfig (" + config
                        + ") with resolvedCacheType (" + resolvedCacheType
                        + ") and resolvedSelectionOrder (" + resolvedSelectionOrder
                        + ") needs to be based on an "
                        + IterableValueSelector.class.getSimpleName() + " (" + valueSelector + ")."
                        + " Check your @" + ValueRangeProvider.class.getSimpleName() + " annotations.");
            }
            valueSelector = new ShufflingValueSelector<>((IterableValueSelector<Solution_>) valueSelector,
                    resolvedCacheType);
        }
        return valueSelector;
    }

    private ValueSelector<Solution_> applyCaching(SelectionCacheType resolvedCacheType,
            SelectionOrder resolvedSelectionOrder, ValueSelector<Solution_> valueSelector) {
        if (resolvedCacheType.isCached() && resolvedCacheType.compareTo(valueSelector.getCacheType()) > 0) {
            if (!(valueSelector instanceof IterableValueSelector)) {
                throw new IllegalArgumentException("The valueSelectorConfig (" + config
                        + ") with resolvedCacheType (" + resolvedCacheType
                        + ") and resolvedSelectionOrder (" + resolvedSelectionOrder
                        + ") needs to be based on an "
                        + IterableValueSelector.class.getSimpleName() + " (" + valueSelector + ")."
                        + " Check your @" + ValueRangeProvider.class.getSimpleName() + " annotations.");
            }
            valueSelector = new CachingValueSelector<>((IterableValueSelector<Solution_>) valueSelector,
                    resolvedCacheType, resolvedSelectionOrder.toRandomSelectionBoolean());
        }
        return valueSelector;
    }

    private void validateSelectedLimit(SelectionCacheType minimumCacheType) {
        if (config.getSelectedCountLimit() != null && minimumCacheType.compareTo(SelectionCacheType.JUST_IN_TIME) > 0) {
            throw new IllegalArgumentException("The valueSelectorConfig (" + config
                    + ") with selectedCountLimit (" + config.getSelectedCountLimit()
                    + ") has a minimumCacheType (" + minimumCacheType
                    + ") that is higher than " + SelectionCacheType.JUST_IN_TIME + ".");
        }
    }

    private ValueSelector<Solution_> applySelectedLimit(ValueSelector<Solution_> valueSelector) {
        if (config.getSelectedCountLimit() != null) {
            valueSelector = new SelectedCountLimitValueSelector<>(valueSelector, config.getSelectedCountLimit());
        }
        return valueSelector;
    }

    private ValueSelector<Solution_> applyNearbySelection(HeuristicConfigPolicy<Solution_> configPolicy,
            EntityDescriptor<Solution_> entityDescriptor, SelectionCacheType minimumCacheType,
            SelectionOrder resolvedSelectionOrder, ValueSelector<Solution_> valueSelector) {
        return TimefoldSolverEnterpriseService.loadOrFail(TimefoldSolverEnterpriseService.Feature.NEARBY_SELECTION)
                .applyNearbySelection(config, configPolicy, entityDescriptor, minimumCacheType, resolvedSelectionOrder,
                        valueSelector);
    }

    private ValueSelector<Solution_> applyMimicRecording(HeuristicConfigPolicy<Solution_> configPolicy,
            ValueSelector<Solution_> valueSelector) {
        var id = config.getId();
        if (id != null) {
            if (id.isEmpty()) {
                throw new IllegalArgumentException("The valueSelectorConfig (%s) has an empty id (%s).".formatted(config, id));
            }
            if (!(valueSelector instanceof IterableValueSelector)) {
                throw new IllegalArgumentException("""
                        The valueSelectorConfig (%s) with id (%s) needs to be based on an %s (%s).
                        Check your @%s annotations."""
                        .formatted(config, id, IterableValueSelector.class.getSimpleName(), valueSelector,
                                ValueRangeProvider.class.getSimpleName()));
            }
            var mimicRecordingValueSelector =
                    new MimicRecordingValueSelector<>((IterableValueSelector<Solution_>) valueSelector);
            configPolicy.addValueMimicRecorder(id, mimicRecordingValueSelector);
            valueSelector = mimicRecordingValueSelector;
        }
        return valueSelector;
    }

    ValueSelector<Solution_> applyListValueFiltering(HeuristicConfigPolicy<?> configPolicy,
            ListValueFilteringType listValueFilteringType,
            GenuineVariableDescriptor<Solution_> variableDescriptor, ValueSelector<Solution_> valueSelector) {
        if (variableDescriptor.isListVariable() && configPolicy.isUnassignedValuesAllowed()
                && listValueFilteringType != ListValueFilteringType.NONE) {
            if (!(valueSelector instanceof IterableValueSelector)) {
                throw new IllegalArgumentException("The valueSelectorConfig (" + config
                        + ") with id (" + config.getId()
                        + ") needs to be based on an "
                        + IterableValueSelector.class.getSimpleName() + " (" + valueSelector + ")."
                        + " Check your @" + ValueRangeProvider.class.getSimpleName() + " annotations.");
            }
            valueSelector = listValueFilteringType == ListValueFilteringType.ACCEPT_ASSIGNED
                    ? new AssignedListValueSelector<>(((IterableValueSelector<Solution_>) valueSelector))
                    : new UnassignedListValueSelector<>(((IterableValueSelector<Solution_>) valueSelector));
        }
        return valueSelector;
    }

    private ValueSelector<Solution_> applyReinitializeVariableFiltering(boolean applyReinitializeVariableFiltering,
            GenuineVariableDescriptor<Solution_> variableDescriptor, ValueSelector<Solution_> valueSelector) {
        if (applyReinitializeVariableFiltering && !variableDescriptor.isListVariable()) {
            valueSelector = new ReinitializeVariableValueSelector<>(valueSelector);
        }
        return valueSelector;
    }

    private ValueSelector<Solution_> applyDowncasting(ValueSelector<Solution_> valueSelector) {
        if (config.getDowncastEntityClass() != null) {
            valueSelector = new DowncastingValueSelector<>(valueSelector, config.getDowncastEntityClass());
        }
        return valueSelector;
    }

    public static <Solution_> ValueSelector<Solution_> applyValueRangeFiltering(
            HeuristicConfigPolicy<Solution_> configPolicy, ValueSelector<Solution_> valueSelector,
            EntityDescriptor<Solution_> entityDescriptor, SelectionCacheType minimumCacheType, SelectionOrder selectionOrder,
            boolean randomSelection, String entityValueRangeRecorderId, boolean assertBothSides) {
        if (entityValueRangeRecorderId == null) {
            return valueSelector;
        }
        var valueSelectorConfig = new ValueSelectorConfig()
                .withMimicSelectorRef(entityValueRangeRecorderId);
        var replayingValueSelector =
                (IterableValueSelector<Solution_>) ValueSelectorFactory.<Solution_> create(valueSelectorConfig)
                        .buildValueSelector(configPolicy, entityDescriptor, minimumCacheType, selectionOrder);
        return new FilteringValueRangeSelector<>((IterableValueSelector<Solution_>) valueSelector, replayingValueSelector,
                randomSelection, assertBothSides);
    }

    public enum ListValueFilteringType {
        NONE,
        ACCEPT_ASSIGNED,
        ACCEPT_UNASSIGNED,
    }
}
