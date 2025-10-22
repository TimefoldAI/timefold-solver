package ai.timefold.solver.core.impl.heuristic.selector.move;

import java.util.Comparator;

import ai.timefold.solver.core.api.domain.common.ComparatorFactory;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.common.decorator.SelectionSorterOrder;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.move.Move;
import ai.timefold.solver.core.impl.heuristic.selector.AbstractSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.ComparatorFactoryAdapter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.ComparatorSelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.FactorySelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionFilter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionProbabilityWeightFactory;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorter;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionSorterWeightFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.decorator.CachingMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.decorator.FilteringMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.decorator.ProbabilityMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.decorator.SelectedCountLimitMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.decorator.ShufflingMoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.decorator.SortingMoveSelector;

public abstract class AbstractMoveSelectorFactory<Solution_, MoveSelectorConfig_ extends MoveSelectorConfig<MoveSelectorConfig_>>
        extends AbstractSelectorFactory<Solution_, MoveSelectorConfig_> implements MoveSelectorFactory<Solution_> {

    public AbstractMoveSelectorFactory(MoveSelectorConfig_ moveSelectorConfig) {
        super(moveSelectorConfig);
    }

    /**
     * Builds a base {@link MoveSelector} without any advanced capabilities (filtering, sorting, ...).
     *
     * @param configPolicy never null
     * @param minimumCacheType never null, If caching is used (different from {@link SelectionCacheType#JUST_IN_TIME}),
     *        then it should be at least this {@link SelectionCacheType} because an ancestor already uses such caching
     *        and less would be pointless.
     * @param randomSelection true is equivalent to {@link SelectionOrder#RANDOM},
     *        false is equivalent to {@link SelectionOrder#ORIGINAL}
     * @return never null
     */
    protected abstract MoveSelector<Solution_> buildBaseMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection);

    /**
     * {@inheritDoc}
     */
    @Override
    public MoveSelector<Solution_> buildMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, SelectionOrder inheritedSelectionOrder, boolean skipNonDoableMoves) {
        MoveSelectorConfig<?> unfoldedMoveSelectorConfig = buildUnfoldedMoveSelectorConfig(configPolicy);
        if (unfoldedMoveSelectorConfig != null) {
            return MoveSelectorFactory.<Solution_> create(unfoldedMoveSelectorConfig)
                    .buildMoveSelector(configPolicy, minimumCacheType, inheritedSelectionOrder, skipNonDoableMoves);
        }

        SelectionCacheType resolvedCacheType = SelectionCacheType.resolve(config.getCacheType(), minimumCacheType);
        SelectionOrder resolvedSelectionOrder =
                SelectionOrder.resolve(config.getSelectionOrder(), inheritedSelectionOrder);

        validateCacheTypeVersusSelectionOrder(resolvedCacheType, resolvedSelectionOrder, false);
        validateSorting(resolvedSelectionOrder);
        validateProbability(resolvedSelectionOrder);
        validateSelectedLimit(minimumCacheType);

        boolean randomMoveSelection = determineBaseRandomSelection(resolvedCacheType, resolvedSelectionOrder);
        SelectionCacheType selectionCacheType = SelectionCacheType.max(minimumCacheType, resolvedCacheType);
        MoveSelector<Solution_> moveSelector = buildBaseMoveSelector(configPolicy, selectionCacheType, randomMoveSelection);
        validateResolvedCacheType(resolvedCacheType, moveSelector);

        moveSelector = applyFiltering(moveSelector, skipNonDoableMoves);
        moveSelector = applySorting(resolvedCacheType, resolvedSelectionOrder, moveSelector);
        moveSelector = applyProbability(resolvedCacheType, resolvedSelectionOrder, moveSelector);
        moveSelector = applyShuffling(resolvedCacheType, resolvedSelectionOrder, moveSelector);
        moveSelector = applyCaching(resolvedCacheType, resolvedSelectionOrder, moveSelector);
        moveSelector = applySelectedLimit(moveSelector);
        return moveSelector;
    }

    /**
     * To provide unfolded MoveSelectorConfig, override this method in a subclass.
     *
     * @param configPolicy never null
     * @return null if no unfolding is needed
     */
    protected MoveSelectorConfig<?> buildUnfoldedMoveSelectorConfig(
            HeuristicConfigPolicy<Solution_> configPolicy) {
        return null;
    }

    protected static <T> T checkUnfolded(String configPropertyName, T configProperty) {
        if (configProperty == null) {
            throw new IllegalStateException("The %s (%s) should haven been initialized during unfolding."
                    .formatted(configPropertyName, configProperty));
        }
        return configProperty;
    }

    private void validateResolvedCacheType(SelectionCacheType resolvedCacheType, MoveSelector<Solution_> moveSelector) {
        if (!moveSelector.supportsPhaseAndSolverCaching() && resolvedCacheType.compareTo(SelectionCacheType.PHASE) >= 0) {
            throw new IllegalArgumentException("The moveSelectorConfig (" + config
                    + ") has a resolvedCacheType (" + resolvedCacheType + ") that is not supported.\n"
                    + "Maybe don't use a <cacheType> on this type of moveSelector.");
        }
    }

    protected boolean determineBaseRandomSelection(SelectionCacheType resolvedCacheType,
            SelectionOrder resolvedSelectionOrder) {
        return switch (resolvedSelectionOrder) {
            case ORIGINAL, SORTED, SHUFFLED, PROBABILISTIC ->
                // baseValueSelector and lower should be ORIGINAL if they are going to get cached completely
                false;
            case RANDOM ->
                // Predict if caching will occur
                resolvedCacheType.isNotCached() || isBaseInherentlyCached() && config.getFilterClass() == null;
            default -> throw new IllegalStateException("The selectionOrder (" + resolvedSelectionOrder
                    + ") is not implemented.");
        };
    }

    private String determineComparatorPropertyName(MoveSelectorConfig_ moveSelectorConfig) {
        var sorterComparatorClass = moveSelectorConfig.getSorterComparatorClass();
        var comparatorClass = moveSelectorConfig.getComparatorClass();
        if (sorterComparatorClass != null && comparatorClass != null) {
            throw new IllegalArgumentException(
                    "The moveSelectorConfig (%s) cannot have a %s (%s) and %s (%s) at the same time.".formatted(
                            moveSelectorConfig, "sorterComparatorClass", sorterComparatorClass,
                            "comparatorClass", comparatorClass));
        }
        return sorterComparatorClass != null ? "sorterComparatorClass" : "comparatorClass";
    }

    private Class<? extends Comparator> determineComparatorClass(MoveSelectorConfig_ moveSelectorConfig) {
        var propertyName = determineComparatorPropertyName(moveSelectorConfig);
        if (propertyName.equals("sorterComparatorClass")) {
            return moveSelectorConfig.getSorterComparatorClass();
        } else {
            return moveSelectorConfig.getComparatorClass();
        }
    }

    private String determineComparatorFactoryPropertyName(MoveSelectorConfig_ moveSelectorConfig) {
        var weightFactoryClass = moveSelectorConfig.getSorterWeightFactoryClass();
        var comparatorFactoryClass = moveSelectorConfig.getComparatorFactoryClass();
        if (weightFactoryClass != null && comparatorFactoryClass != null) {
            throw new IllegalArgumentException(
                    "The moveSelectorConfig (%s) cannot have a %s (%s) and %s (%s) at the same time.".formatted(
                            moveSelectorConfig, "sorterWeightFactoryClass", weightFactoryClass,
                            "comparatorFactoryClass", comparatorFactoryClass));
        }
        return weightFactoryClass != null ? "sorterWeightFactoryClass" : "comparatorFactoryClass";
    }

    private Class<?> determineComparatorFactoryClass(MoveSelectorConfig_ moveSelectorConfig) {
        var propertyName = determineComparatorFactoryPropertyName(moveSelectorConfig);
        if (propertyName.equals("sorterWeightFactoryClass")) {
            return moveSelectorConfig.getSorterWeightFactoryClass();
        } else {
            return moveSelectorConfig.getComparatorFactoryClass();
        }
    }

    protected boolean isBaseInherentlyCached() {
        return false;
    }

    private MoveSelector<Solution_> applyFiltering(MoveSelector<Solution_> moveSelector, boolean skipNonDoableMoves) {
        /*
         * Do not filter out pointless moves in Construction Heuristics and Exhaustive Search,
         * because the original value of the entity is irrelevant.
         * If the original value is null and the variable allows unassigned values,
         * the change move to null must be done too.
         */
        SelectionFilter<Solution_, Move<Solution_>> baseFilter = skipNonDoableMoves
                ? DoableMoveSelectionFilter.INSTANCE
                : null;
        var filterClass = config.getFilterClass();
        if (filterClass != null) {
            SelectionFilter<Solution_, Move<Solution_>> selectionFilter =
                    ConfigUtils.newInstance(config, "filterClass", filterClass);
            SelectionFilter<Solution_, Move<Solution_>> finalFilter =
                    baseFilter == null ? selectionFilter : SelectionFilter.compose(baseFilter, selectionFilter);
            return FilteringMoveSelector.of(moveSelector, finalFilter);
        } else if (baseFilter != null) {
            return FilteringMoveSelector.of(moveSelector, baseFilter);
        } else {
            return moveSelector;
        }
    }

    protected void validateSorting(SelectionOrder resolvedSelectionOrder) {
        var comparatorClass = determineComparatorClass(config);
        var comparatorFactoryClass = determineComparatorFactoryClass(config);
        if ((comparatorClass != null || comparatorFactoryClass != null
                || config.getSorterOrder() != null || config.getSorterClass() != null)
                && resolvedSelectionOrder != SelectionOrder.SORTED) {
            throw new IllegalArgumentException(
                    "The moveSelectorConfig (%s) with %s (%s) and %s (%s) and sorterOrder (%s) and sorterClass (%s) has a resolvedSelectionOrder (%s) that is not %s."
                            .formatted(config, determineComparatorPropertyName(config), comparatorClass,
                                    determineComparatorFactoryPropertyName(config),
                                    comparatorFactoryClass, config.getSorterOrder(), config.getSorterClass(),
                                    resolvedSelectionOrder, SelectionOrder.SORTED));
        }
        if (comparatorClass != null && comparatorFactoryClass != null) {
            throw new IllegalArgumentException(
                    "The moveSelectorConfig (%s) has both a %s (%s) and a %s (%s).".formatted(config,
                            determineComparatorPropertyName(config), comparatorClass,
                            determineComparatorFactoryPropertyName(config),
                            comparatorFactoryClass));
        }
        if (comparatorClass != null && config.getSorterClass() != null) {
            throw new IllegalArgumentException(
                    "The moveSelectorConfig (%s) has both a %s (%s) and a sorterClass (%s)."
                            .formatted(config, determineComparatorPropertyName(config), comparatorClass,
                                    config.getSorterClass()));
        }
        if (comparatorFactoryClass != null && config.getSorterClass() != null) {
            throw new IllegalArgumentException(
                    "The moveSelectorConfig (%s) has both a %s (%s) and a sorterClass (%s).".formatted(config,
                            determineComparatorFactoryPropertyName(config), comparatorFactoryClass,
                            config.getSorterClass()));
        }
        if (config.getSorterClass() != null && config.getSorterOrder() != null) {
            throw new IllegalArgumentException(
                    "The moveSelectorConfig (%s) with sorterClass (%s) has a non-null sorterOrder (%s).".formatted(config,
                            config.getSorterClass(), config.getSorterOrder()));
        }
    }

    protected MoveSelector<Solution_> applySorting(SelectionCacheType resolvedCacheType,
            SelectionOrder resolvedSelectionOrder, MoveSelector<Solution_> moveSelector) {
        if (resolvedSelectionOrder == SelectionOrder.SORTED) {
            SelectionSorter<Solution_, Move<Solution_>> sorter;
            var comparatorClass = determineComparatorClass(config);
            var comparatorFactoryClass = determineComparatorFactoryClass(config);
            var sorterClass = config.getSorterClass();
            if (comparatorClass != null) {
                Comparator<Move<Solution_>> sorterComparator =
                        ConfigUtils.newInstance(config, determineComparatorPropertyName(config), comparatorClass);
                sorter = new ComparatorSelectionSorter<>(sorterComparator,
                        SelectionSorterOrder.resolve(config.getSorterOrder()));
            } else if (comparatorFactoryClass != null) {
                var instance =
                        ConfigUtils.newInstance(config, determineComparatorFactoryPropertyName(config), comparatorFactoryClass);
                ComparatorFactory<Solution_, Move<Solution_>, ?> comparatorFactory;
                if (instance instanceof ComparatorFactory<?, ?, ?> factoryInstance) {
                    comparatorFactory = (ComparatorFactory<Solution_, Move<Solution_>, ?>) factoryInstance;
                } else {
                    comparatorFactory =
                            new ComparatorFactoryAdapter<>((SelectionSorterWeightFactory<Solution_, Move<Solution_>>) instance);
                }
                sorter = new FactorySelectionSorter<>(comparatorFactory, SelectionSorterOrder.resolve(config.getSorterOrder()));
            } else if (sorterClass != null) {
                sorter = ConfigUtils.newInstance(config, "sorterClass", sorterClass);
            } else {
                throw new IllegalArgumentException(
                        "The moveSelectorConfig (%s) with resolvedSelectionOrder (%s) needs a %s (%s) or a %s (%s) or a sorterClass (%s)."
                                .formatted(config, resolvedSelectionOrder, determineComparatorPropertyName(config),
                                        comparatorClass, determineComparatorFactoryPropertyName(config), comparatorFactoryClass,
                                        sorterClass));
            }
            moveSelector = new SortingMoveSelector<>(moveSelector, resolvedCacheType, sorter);
        }
        return moveSelector;
    }

    private void validateProbability(SelectionOrder resolvedSelectionOrder) {
        var probabilityWeightFactoryClass = config.getProbabilityWeightFactoryClass();
        if (probabilityWeightFactoryClass != null && resolvedSelectionOrder != SelectionOrder.PROBABILISTIC) {
            throw new IllegalArgumentException(
                    "The moveSelectorConfig (%s) with probabilityWeightFactoryClass (%s) has a resolvedSelectionOrder (%s) that is not %s."
                            .formatted(config, probabilityWeightFactoryClass, resolvedSelectionOrder,
                                    SelectionOrder.PROBABILISTIC));
        }
    }

    private MoveSelector<Solution_> applyProbability(SelectionCacheType resolvedCacheType,
            SelectionOrder resolvedSelectionOrder, MoveSelector<Solution_> moveSelector) {
        if (resolvedSelectionOrder == SelectionOrder.PROBABILISTIC) {
            var probabilityWeightFactoryClass = config.getProbabilityWeightFactoryClass();
            if (probabilityWeightFactoryClass == null) {
                throw new IllegalArgumentException(
                        "The moveSelectorConfig (%s) with resolvedSelectionOrder (%s) needs a probabilityWeightFactoryClass (%s)."
                                .formatted(config, resolvedSelectionOrder, probabilityWeightFactoryClass));
            }
            SelectionProbabilityWeightFactory<Solution_, Move<Solution_>> probabilityWeightFactory =
                    ConfigUtils.newInstance(config, "probabilityWeightFactoryClass", probabilityWeightFactoryClass);
            moveSelector = new ProbabilityMoveSelector<>(moveSelector, resolvedCacheType, probabilityWeightFactory);
        }
        return moveSelector;
    }

    private MoveSelector<Solution_> applyShuffling(SelectionCacheType resolvedCacheType,
            SelectionOrder resolvedSelectionOrder, MoveSelector<Solution_> moveSelector) {
        if (resolvedSelectionOrder == SelectionOrder.SHUFFLED) {
            moveSelector = new ShufflingMoveSelector<>(moveSelector, resolvedCacheType);
        }
        return moveSelector;
    }

    private MoveSelector<Solution_> applyCaching(SelectionCacheType resolvedCacheType,
            SelectionOrder resolvedSelectionOrder, MoveSelector<Solution_> moveSelector) {
        if (resolvedCacheType.isCached() && resolvedCacheType.compareTo(moveSelector.getCacheType()) > 0) {
            moveSelector =
                    new CachingMoveSelector<>(moveSelector, resolvedCacheType,
                            resolvedSelectionOrder.toRandomSelectionBoolean());
        }
        return moveSelector;
    }

    private void validateSelectedLimit(SelectionCacheType minimumCacheType) {
        if (config.getSelectedCountLimit() != null
                && minimumCacheType.compareTo(SelectionCacheType.JUST_IN_TIME) > 0) {
            throw new IllegalArgumentException("The moveSelectorConfig (" + config
                    + ") with selectedCountLimit (" + config.getSelectedCountLimit()
                    + ") has a minimumCacheType (" + minimumCacheType
                    + ") that is higher than " + SelectionCacheType.JUST_IN_TIME + ".");
        }
    }

    private MoveSelector<Solution_> applySelectedLimit(MoveSelector<Solution_> moveSelector) {
        if (config.getSelectedCountLimit() != null) {
            moveSelector = new SelectedCountLimitMoveSelector<>(moveSelector, config.getSelectedCountLimit());
        }
        return moveSelector;
    }
}
