package ai.timefold.solver.core.impl.heuristic.selector.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import ai.timefold.solver.core.api.domain.solution.PlanningSolution;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.common.decorator.SelectionSorterOrder;
import ai.timefold.solver.core.config.heuristic.selector.common.nearby.NearbySelectionConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.enterprise.TimefoldSolverEnterpriseService;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.ComparatorSelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionProbabilityWeightFactory;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.WeightFactorySelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.entity.decorator.CachingEntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.decorator.FilteringEntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.decorator.ProbabilityEntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.decorator.SelectedCountLimitEntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.decorator.ShufflingEntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.decorator.SortingEntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.mimic.MimicRecordingEntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.mimic.MimicReplayingEntitySelector;
import ai.timefold.solver.core.impl.solver.ClassInstanceCache;

public class EntitySelectorFactory<Solution_> extends AbstractSelectorFactory<Solution_, EntitySelectorConfig> {

    public static <Solution_> EntitySelectorFactory<Solution_> create(EntitySelectorConfig entitySelectorConfig) {
        return new EntitySelectorFactory<>(entitySelectorConfig);
    }

    public EntitySelectorFactory(EntitySelectorConfig entitySelectorConfig) {
        super(entitySelectorConfig);
    }

    public EntityDescriptor<Solution_> extractEntityDescriptor(HeuristicConfigPolicy<Solution_> configPolicy) {
        var entityClass = config.getEntityClass();
        var mimicSelectorRef = config.getMimicSelectorRef();
        if (entityClass != null) {
            var solutionDescriptor = configPolicy.getSolutionDescriptor();
            var entityDescriptor = solutionDescriptor.getEntityDescriptorStrict(entityClass);
            if (entityDescriptor == null) {
                throw new IllegalArgumentException("""
                        The selectorConfig (%s) has an entityClass (%s) that is not a known planning entity.
                        Check your solver configuration. If that class (%s) is not in the entityClassSet (%s), \
                        check your @%s implementation's annotated methods too."""
                        .formatted(config, entityClass, entityClass.getSimpleName(),
                                solutionDescriptor.getEntityClassSet(), PlanningSolution.class.getSimpleName()));
            }
            return entityDescriptor;
        } else if (mimicSelectorRef != null) {
            return configPolicy.getEntityMimicRecorder(mimicSelectorRef).getEntityDescriptor();
        } else {
            return null;
        }
    }

    /**
     * @param configPolicy never null
     * @param minimumCacheType never null, If caching is used (different from {@link SelectionCacheType#JUST_IN_TIME}),
     *        then it should be at least this {@link SelectionCacheType} because an ancestor already uses such caching
     *        and less would be pointless.
     * @param inheritedSelectionOrder never null
     * @return never null
     */
    public EntitySelector<Solution_> buildEntitySelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, SelectionOrder inheritedSelectionOrder) {
        if (config.getMimicSelectorRef() != null) {
            return buildMimicReplaying(configPolicy);
        }
        var entityDescriptor = deduceEntityDescriptor(configPolicy, config.getEntityClass());
        var resolvedCacheType = SelectionCacheType.resolve(config.getCacheType(), minimumCacheType);
        var resolvedSelectionOrder = SelectionOrder.resolve(config.getSelectionOrder(), inheritedSelectionOrder);

        var nearbySelectionConfig = config.getNearbySelectionConfig();
        if (nearbySelectionConfig != null) {
            nearbySelectionConfig.validateNearby(resolvedCacheType, resolvedSelectionOrder);
        }
        validateCacheTypeVersusSelectionOrder(resolvedCacheType, resolvedSelectionOrder);
        validateSorting(resolvedSelectionOrder);
        validateProbability(resolvedSelectionOrder);
        validateSelectedLimit(minimumCacheType);

        // baseEntitySelector and lower should be SelectionOrder.ORIGINAL if they are going to get cached completely
        var baseRandomSelection = determineBaseRandomSelection(entityDescriptor, resolvedCacheType, resolvedSelectionOrder);
        var baseSelectionCacheType = SelectionCacheType.max(minimumCacheType, resolvedCacheType);
        var entitySelector = buildBaseEntitySelector(entityDescriptor, baseSelectionCacheType,
                baseRandomSelection);
        if (nearbySelectionConfig != null) {
            // TODO Static filtering (such as movableEntitySelectionFilter) should affect nearbySelection
            entitySelector = applyNearbySelection(configPolicy, nearbySelectionConfig, minimumCacheType,
                    resolvedSelectionOrder, entitySelector);
        }
        var instanceCache = configPolicy.getClassInstanceCache();
        entitySelector = applyFiltering(entitySelector, instanceCache);
        entitySelector = applySorting(resolvedCacheType, resolvedSelectionOrder, entitySelector, instanceCache);
        entitySelector = applyProbability(resolvedCacheType, resolvedSelectionOrder, entitySelector, instanceCache);
        entitySelector = applyShuffling(resolvedCacheType, resolvedSelectionOrder, entitySelector);
        entitySelector = applyCaching(resolvedCacheType, resolvedSelectionOrder, entitySelector);
        entitySelector = applySelectedLimit(resolvedSelectionOrder, entitySelector);
        entitySelector = applyMimicRecording(configPolicy, entitySelector);
        return entitySelector;
    }

    protected EntitySelector<Solution_> buildMimicReplaying(HeuristicConfigPolicy<Solution_> configPolicy) {
        final var anyConfigurationParameterDefined = Stream
                .of(config.getId(), config.getEntityClass(), config.getCacheType(), config.getSelectionOrder(),
                        config.getNearbySelectionConfig(), config.getFilterClass(), config.getSorterManner(),
                        config.getSorterComparatorClass(), config.getSorterWeightFactoryClass(), config.getSorterOrder(),
                        config.getSorterClass(), config.getProbabilityWeightFactoryClass(), config.getSelectedCountLimit())
                .anyMatch(Objects::nonNull);
        if (anyConfigurationParameterDefined) {
            throw new IllegalArgumentException(
                    "The entitySelectorConfig (%s) with mimicSelectorRef (%s) has another property that is not null."
                            .formatted(config, config.getMimicSelectorRef()));
        }
        var entityMimicRecorder = configPolicy.getEntityMimicRecorder(config.getMimicSelectorRef());
        if (entityMimicRecorder == null) {
            throw new IllegalArgumentException(
                    "The entitySelectorConfig (%s) has a mimicSelectorRef (%s) for which no entitySelector with that id exists (in its solver phase)."
                            .formatted(config, config.getMimicSelectorRef()));
        }
        return new MimicReplayingEntitySelector<>(entityMimicRecorder);
    }

    protected boolean determineBaseRandomSelection(EntityDescriptor<Solution_> entityDescriptor,
            SelectionCacheType resolvedCacheType, SelectionOrder resolvedSelectionOrder) {
        return switch (resolvedSelectionOrder) {
            case ORIGINAL, SORTED, SHUFFLED, PROBABILISTIC ->
                // baseValueSelector and lower should be ORIGINAL if they are going to get cached completely
                false;
            case RANDOM ->
                // Predict if caching will occur
                resolvedCacheType.isNotCached()
                        || (isBaseInherentlyCached() && !hasFiltering(entityDescriptor));
            default -> throw new IllegalStateException("The selectionOrder (%s) is not implemented."
                    .formatted(resolvedSelectionOrder));
        };
    }

    protected boolean isBaseInherentlyCached() {
        return true;
    }

    private EntitySelector<Solution_> buildBaseEntitySelector(EntityDescriptor<Solution_> entityDescriptor,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        if (minimumCacheType == SelectionCacheType.SOLVER) {
            // TODO Solver cached entities are not compatible with ConstraintStreams and IncrementalScoreDirector
            // because between phases the entities get cloned
            throw new IllegalArgumentException("The minimumCacheType (%s) is not supported here. Please use %s instead."
                    .formatted(minimumCacheType, SelectionCacheType.PHASE));
        }
        // FromSolutionEntitySelector has an intrinsicCacheType STEP
        return new FromSolutionEntitySelector<>(entityDescriptor, minimumCacheType, randomSelection);
    }

    private boolean hasFiltering(EntityDescriptor<Solution_> entityDescriptor) {
        return config.getFilterClass() != null || entityDescriptor.hasEffectiveMovableEntityFilter();
    }

    private EntitySelector<Solution_> applyNearbySelection(HeuristicConfigPolicy<Solution_> configPolicy,
            NearbySelectionConfig nearbySelectionConfig, SelectionCacheType minimumCacheType,
            SelectionOrder resolvedSelectionOrder, EntitySelector<Solution_> entitySelector) {
        return TimefoldSolverEnterpriseService.loadOrFail(TimefoldSolverEnterpriseService.Feature.NEARBY_SELECTION)
                .applyNearbySelection(config, configPolicy, nearbySelectionConfig, minimumCacheType,
                        resolvedSelectionOrder, entitySelector);
    }

    private EntitySelector<Solution_> applyFiltering(EntitySelector<Solution_> entitySelector,
            ClassInstanceCache instanceCache) {
        var entityDescriptor = entitySelector.getEntityDescriptor();
        if (hasFiltering(entityDescriptor)) {
            var filterClass = config.getFilterClass();
            var filterList = new ArrayList<SelectionFilter<Solution_, Object>>(filterClass == null ? 1 : 2);
            if (filterClass != null) {
                SelectionFilter<Solution_, Object> selectionFilter =
                        instanceCache.newInstance(config, "filterClass", filterClass);
                filterList.add(selectionFilter);
            }
            // Filter out pinned entities
            if (entityDescriptor.hasEffectiveMovableEntityFilter()) {
                filterList.add((scoreDirector, selection) -> entityDescriptor.getEffectiveMovableEntityFilter()
                        .test(scoreDirector.getWorkingSolution(), selection));
            }
            // Do not filter out initialized entities here for CH and ES, because they can be partially initialized
            // Instead, ValueSelectorConfig.applyReinitializeVariableFiltering() does that.
            entitySelector = FilteringEntitySelector.of(entitySelector, SelectionFilter.compose(filterList));
        }
        return entitySelector;
    }

    protected void validateSorting(SelectionOrder resolvedSelectionOrder) {
        var sorterManner = config.getSorterManner();
        var sorterComparatorClass = config.getSorterComparatorClass();
        var sorterWeightFactoryClass = config.getSorterWeightFactoryClass();
        var sorterOrder = config.getSorterOrder();
        var sorterClass = config.getSorterClass();
        if ((sorterManner != null || sorterComparatorClass != null || sorterWeightFactoryClass != null || sorterOrder != null
                || sorterClass != null) && resolvedSelectionOrder != SelectionOrder.SORTED) {
            throw new IllegalArgumentException("""
                    The entitySelectorConfig (%s) with sorterManner (%s) \
                    and sorterComparatorClass (%s) and sorterWeightFactoryClass (%s) and sorterOrder (%s) and sorterClass (%s) \
                    has a resolvedSelectionOrder (%s) that is not %s."""
                    .formatted(config, sorterManner, sorterComparatorClass, sorterWeightFactoryClass, sorterOrder, sorterClass,
                            resolvedSelectionOrder, SelectionOrder.SORTED));
        }
        assertNotSorterMannerAnd(config, "sorterComparatorClass", EntitySelectorConfig::getSorterComparatorClass);
        assertNotSorterMannerAnd(config, "sorterWeightFactoryClass", EntitySelectorConfig::getSorterWeightFactoryClass);
        assertNotSorterMannerAnd(config, "sorterClass", EntitySelectorConfig::getSorterClass);
        assertNotSorterMannerAnd(config, "sorterOrder", EntitySelectorConfig::getSorterOrder);
        assertNotSorterClassAnd(config, "sorterComparatorClass", EntitySelectorConfig::getSorterComparatorClass);
        assertNotSorterClassAnd(config, "sorterWeightFactoryClass", EntitySelectorConfig::getSorterWeightFactoryClass);
        assertNotSorterClassAnd(config, "sorterOrder", EntitySelectorConfig::getSorterOrder);
        if (sorterComparatorClass != null && sorterWeightFactoryClass != null) {
            throw new IllegalArgumentException(
                    "The entitySelectorConfig (%s) has both a sorterComparatorClass (%s) and a sorterWeightFactoryClass (%s)."
                            .formatted(config, sorterComparatorClass, sorterWeightFactoryClass));
        }
    }

    private static void assertNotSorterMannerAnd(EntitySelectorConfig config, String propertyName,
            Function<EntitySelectorConfig, Object> propertyAccessor) {
        var sorterManner = config.getSorterManner();
        var property = propertyAccessor.apply(config);
        if (sorterManner != null && property != null) {
            throw new IllegalArgumentException("The entitySelectorConfig (%s) has both a sorterManner (%s) and a %s (%s)."
                    .formatted(config, sorterManner, propertyName, property));
        }
    }

    private static void assertNotSorterClassAnd(EntitySelectorConfig config, String propertyName,
            Function<EntitySelectorConfig, Object> propertyAccessor) {
        var sorterClass = config.getSorterClass();
        var property = propertyAccessor.apply(config);
        if (sorterClass != null && property != null) {
            throw new IllegalArgumentException(
                    "The entitySelectorConfig (%s) with sorterClass (%s) has a non-null %s (%s)."
                            .formatted(config, sorterClass, propertyName, property));
        }
    }

    protected EntitySelector<Solution_> applySorting(SelectionCacheType resolvedCacheType,
            SelectionOrder resolvedSelectionOrder, EntitySelector<Solution_> entitySelector, ClassInstanceCache instanceCache) {
        if (resolvedSelectionOrder == SelectionOrder.SORTED) {
            SelectionSorter<Solution_, Object> sorter;
            var sorterManner = config.getSorterManner();
            if (sorterManner != null) {
                var entityDescriptor = entitySelector.getEntityDescriptor();
                if (!EntitySelectorConfig.hasSorter(sorterManner, entityDescriptor)) {
                    return entitySelector;
                }
                sorter = EntitySelectorConfig.determineSorter(sorterManner, entityDescriptor);
            } else if (config.getSorterComparatorClass() != null) {
                Comparator<Object> sorterComparator =
                        instanceCache.newInstance(config, "sorterComparatorClass", config.getSorterComparatorClass());
                sorter = new ComparatorSelectionSorter<>(sorterComparator,
                        SelectionSorterOrder.resolve(config.getSorterOrder()));
            } else if (config.getSorterWeightFactoryClass() != null) {
                SelectionSorterWeightFactory<Solution_, Object> sorterWeightFactory =
                        instanceCache.newInstance(config, "sorterWeightFactoryClass", config.getSorterWeightFactoryClass());
                sorter = new WeightFactorySelectionSorter<>(sorterWeightFactory,
                        SelectionSorterOrder.resolve(config.getSorterOrder()));
            } else if (config.getSorterClass() != null) {
                sorter = instanceCache.newInstance(config, "sorterClass", config.getSorterClass());
            } else {
                throw new IllegalArgumentException("""
                        The entitySelectorConfig (%s) with resolvedSelectionOrder (%s) needs \
                        a sorterManner (%s) or a sorterComparatorClass (%s) or a sorterWeightFactoryClass (%s) \
                        or a sorterClass (%s)."""
                        .formatted(config, resolvedSelectionOrder, sorterManner, config.getSorterComparatorClass(),
                                config.getSorterWeightFactoryClass(), config.getSorterClass()));
            }
            entitySelector = new SortingEntitySelector<>(entitySelector, resolvedCacheType, sorter);
        }
        return entitySelector;
    }

    protected void validateProbability(SelectionOrder resolvedSelectionOrder) {
        if (config.getProbabilityWeightFactoryClass() != null
                && resolvedSelectionOrder != SelectionOrder.PROBABILISTIC) {
            throw new IllegalArgumentException("The entitySelectorConfig (" + config
                    + ") with probabilityWeightFactoryClass (" + config.getProbabilityWeightFactoryClass()
                    + ") has a resolvedSelectionOrder (" + resolvedSelectionOrder
                    + ") that is not " + SelectionOrder.PROBABILISTIC + ".");
        }
    }

    protected EntitySelector<Solution_> applyProbability(SelectionCacheType resolvedCacheType,
            SelectionOrder resolvedSelectionOrder, EntitySelector<Solution_> entitySelector, ClassInstanceCache instanceCache) {
        if (resolvedSelectionOrder == SelectionOrder.PROBABILISTIC) {
            if (config.getProbabilityWeightFactoryClass() == null) {
                throw new IllegalArgumentException("The entitySelectorConfig (" + config
                        + ") with resolvedSelectionOrder (" + resolvedSelectionOrder
                        + ") needs a probabilityWeightFactoryClass ("
                        + config.getProbabilityWeightFactoryClass() + ").");
            }
            SelectionProbabilityWeightFactory<Solution_, Object> probabilityWeightFactory = instanceCache.newInstance(config,
                    "probabilityWeightFactoryClass", config.getProbabilityWeightFactoryClass());
            entitySelector = new ProbabilityEntitySelector<>(entitySelector, resolvedCacheType, probabilityWeightFactory);
        }
        return entitySelector;
    }

    private EntitySelector<Solution_> applyShuffling(SelectionCacheType resolvedCacheType,
            SelectionOrder resolvedSelectionOrder, EntitySelector<Solution_> entitySelector) {
        if (resolvedSelectionOrder == SelectionOrder.SHUFFLED) {
            entitySelector = new ShufflingEntitySelector<>(entitySelector, resolvedCacheType);
        }
        return entitySelector;
    }

    private EntitySelector<Solution_> applyCaching(SelectionCacheType resolvedCacheType,
            SelectionOrder resolvedSelectionOrder, EntitySelector<Solution_> entitySelector) {
        if (resolvedCacheType.isCached() && resolvedCacheType.compareTo(entitySelector.getCacheType()) > 0) {
            entitySelector = new CachingEntitySelector<>(entitySelector, resolvedCacheType,
                    resolvedSelectionOrder.toRandomSelectionBoolean());
        }
        return entitySelector;
    }

    private void validateSelectedLimit(SelectionCacheType minimumCacheType) {
        if (config.getSelectedCountLimit() != null
                && minimumCacheType.compareTo(SelectionCacheType.JUST_IN_TIME) > 0) {
            throw new IllegalArgumentException("The entitySelectorConfig (" + config
                    + ") with selectedCountLimit (" + config.getSelectedCountLimit()
                    + ") has a minimumCacheType (" + minimumCacheType
                    + ") that is higher than " + SelectionCacheType.JUST_IN_TIME + ".");
        }
    }

    private EntitySelector<Solution_> applySelectedLimit(SelectionOrder resolvedSelectionOrder,
            EntitySelector<Solution_> entitySelector) {
        if (config.getSelectedCountLimit() != null) {
            entitySelector = new SelectedCountLimitEntitySelector<>(entitySelector,
                    resolvedSelectionOrder.toRandomSelectionBoolean(), config.getSelectedCountLimit());
        }
        return entitySelector;
    }

    private EntitySelector<Solution_> applyMimicRecording(HeuristicConfigPolicy<Solution_> configPolicy,
            EntitySelector<Solution_> entitySelector) {
        var id = config.getId();
        if (id != null) {
            if (id.isEmpty()) {
                throw new IllegalArgumentException("The entitySelectorConfig (%s) has an empty id (%s).".formatted(config, id));
            }
            var mimicRecordingEntitySelector = new MimicRecordingEntitySelector<>(entitySelector);
            configPolicy.addEntityMimicRecorder(id, mimicRecordingEntitySelector);
            entitySelector = mimicRecordingEntitySelector;
        }
        return entitySelector;
    }
}
