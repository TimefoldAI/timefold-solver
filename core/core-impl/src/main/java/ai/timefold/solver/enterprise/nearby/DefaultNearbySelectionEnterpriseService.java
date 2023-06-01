package ai.timefold.solver.enterprise.nearby;

import java.util.Optional;

import ai.timefold.solver.core.NearbySelectionEnterpriseService;
import ai.timefold.solver.core.api.domain.valuerange.ValueRangeProvider;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.common.nearby.NearbySelectionConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.DestinationSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.SubListSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.common.nearby.NearbyDistanceMeter;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.ElementDestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.RandomSubListSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubListSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubListSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.value.EntityIndependentValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelectorFactory;
import ai.timefold.solver.core.impl.util.Pair;
import ai.timefold.solver.enterprise.nearby.common.NearbyRandom;
import ai.timefold.solver.enterprise.nearby.common.NearbyRandomFactory;
import ai.timefold.solver.enterprise.nearby.entity.NearEntityNearbyEntitySelector;
import ai.timefold.solver.enterprise.nearby.list.NearSubListNearbyDestinationSelector;
import ai.timefold.solver.enterprise.nearby.list.NearSubListNearbySubListSelector;
import ai.timefold.solver.enterprise.nearby.list.NearValueNearbyDestinationSelector;
import ai.timefold.solver.enterprise.nearby.value.NearEntityNearbyValueSelector;
import ai.timefold.solver.enterprise.nearby.value.NearValueNearbyValueSelector;

public final class DefaultNearbySelectionEnterpriseService implements NearbySelectionEnterpriseService {
    @Override
    public <Solution_> EntitySelector<Solution_> applyNearbySelection(EntitySelectorConfig config,
            HeuristicConfigPolicy<Solution_> configPolicy, NearbySelectionConfig nearbySelectionConfig,
            SelectionCacheType minimumCacheType, SelectionOrder resolvedSelectionOrder,
            EntitySelector<Solution_> entitySelector) {
        boolean randomSelection = resolvedSelectionOrder.toRandomSelectionBoolean();
        if (nearbySelectionConfig.getOriginEntitySelectorConfig() == null) {
            throw new IllegalArgumentException("The entitySelector (" + config
                    + ")'s nearbySelectionConfig (" + nearbySelectionConfig + ") requires an originEntitySelector.");
        }
        EntitySelectorFactory<Solution_> entitySelectorFactory =
                EntitySelectorFactory.create(nearbySelectionConfig.getOriginEntitySelectorConfig());
        EntitySelector<Solution_> originEntitySelector =
                entitySelectorFactory.buildEntitySelector(configPolicy, minimumCacheType, resolvedSelectionOrder);
        NearbyDistanceMeter nearbyDistanceMeter =
                configPolicy.getClassInstanceCache().newInstance(nearbySelectionConfig, "nearbyDistanceMeterClass",
                        nearbySelectionConfig.getNearbyDistanceMeterClass());
        // TODO Check nearbyDistanceMeterClass.getGenericInterfaces() to confirm generic type S is an entityClass
        NearbyRandom nearbyRandom = NearbyRandomFactory.create(nearbySelectionConfig).buildNearbyRandom(randomSelection);
        return new NearEntityNearbyEntitySelector<>(entitySelector, originEntitySelector, nearbyDistanceMeter,
                nearbyRandom, randomSelection);
    }

    @Override
    public <Solution_> ValueSelector<Solution_> applyNearbySelection(ValueSelectorConfig config,
            HeuristicConfigPolicy<Solution_> configPolicy, EntityDescriptor<Solution_> entityDescriptor,
            SelectionCacheType minimumCacheType, SelectionOrder resolvedSelectionOrder,
            ValueSelector<Solution_> valueSelector) {
        NearbySelectionConfig nearbySelectionConfig = config.getNearbySelectionConfig();
        boolean randomSelection = resolvedSelectionOrder.toRandomSelectionBoolean();
        NearbyDistanceMeter<?, ?> nearbyDistanceMeter = configPolicy.getClassInstanceCache().newInstance(nearbySelectionConfig,
                "nearbyDistanceMeterClass", nearbySelectionConfig.getNearbyDistanceMeterClass());
        // TODO Check nearbyDistanceMeterClass.getGenericInterfaces() to confirm generic type S is an entityClass
        NearbyRandom nearbyRandom = NearbyRandomFactory.create(nearbySelectionConfig).buildNearbyRandom(randomSelection);
        if (nearbySelectionConfig.getOriginEntitySelectorConfig() != null) {
            EntitySelector<Solution_> originEntitySelector = EntitySelectorFactory
                    .<Solution_> create(nearbySelectionConfig.getOriginEntitySelectorConfig())
                    .buildEntitySelector(configPolicy, minimumCacheType, resolvedSelectionOrder);
            return new NearEntityNearbyValueSelector<>(valueSelector, originEntitySelector, nearbyDistanceMeter,
                    nearbyRandom, randomSelection);
        } else if (nearbySelectionConfig.getOriginValueSelectorConfig() != null) {
            ValueSelector<Solution_> originValueSelector = ValueSelectorFactory
                    .<Solution_> create(nearbySelectionConfig.getOriginValueSelectorConfig())
                    .buildValueSelector(configPolicy, entityDescriptor, minimumCacheType, resolvedSelectionOrder);
            if (!(valueSelector instanceof EntityIndependentValueSelector)) {
                throw new IllegalArgumentException(
                        "The valueSelectorConfig (" + config
                                + ") needs to be based on an "
                                + EntityIndependentValueSelector.class.getSimpleName() + " (" + valueSelector + ")."
                                + " Check your @" + ValueRangeProvider.class.getSimpleName() + " annotations.");
            }
            if (!(originValueSelector instanceof EntityIndependentValueSelector)) {
                throw new IllegalArgumentException(
                        "The originValueSelectorConfig (" + nearbySelectionConfig.getOriginValueSelectorConfig()
                                + ") needs to be based on an "
                                + EntityIndependentValueSelector.class.getSimpleName() + " (" + originValueSelector + ")."
                                + " Check your @" + ValueRangeProvider.class.getSimpleName() + " annotations.");
            }
            return new NearValueNearbyValueSelector<>(
                    (EntityIndependentValueSelector<Solution_>) valueSelector,
                    (EntityIndependentValueSelector<Solution_>) originValueSelector,
                    nearbyDistanceMeter, nearbyRandom, randomSelection);
        } else {
            throw new IllegalArgumentException("The valueSelector (" + config
                    + ")'s nearbySelectionConfig (" + nearbySelectionConfig
                    + ") requires an originEntitySelector or an originValueSelector.");
        }
    }

    @Override
    public <Solution_> SubListSelector<Solution_> applyNearbySelection(SubListSelectorConfig config,
            HeuristicConfigPolicy<Solution_> configPolicy, SelectionCacheType minimumCacheType,
            SelectionOrder resolvedSelectionOrder, RandomSubListSelector<Solution_> subListSelector) {
        NearbySelectionConfig nearbySelectionConfig = config.getNearbySelectionConfig();
        randomDistributionNearbyLimitation(nearbySelectionConfig).ifPresent(configPropertyNameAndValue -> {
            if (config.getMinimumSubListSize() != null && config.getMinimumSubListSize() > 1) {
                throw new IllegalArgumentException("Using minimumSubListSize (" + config.getMinimumSubListSize()
                        + ") is not allowed because the nearby selection distribution uses a "
                        + configPropertyNameAndValue.getKey() + " (" + configPropertyNameAndValue.getValue()
                        + ") which may limit the ability to select all nearby values."
                        + " As a consequence, it may be impossible to select a subList with the required minimumSubListSize."
                        + " Therefore, this combination is prohibited.");
            }
        });

        nearbySelectionConfig.validateNearby(minimumCacheType, resolvedSelectionOrder);

        boolean randomSelection = resolvedSelectionOrder.toRandomSelectionBoolean();

        NearbyDistanceMeter<?, ?> nearbyDistanceMeter =
                configPolicy.getClassInstanceCache().newInstance(nearbySelectionConfig,
                        "nearbyDistanceMeterClass", nearbySelectionConfig.getNearbyDistanceMeterClass());
        // TODO Check nearbyDistanceMeterClass.getGenericInterfaces() to confirm generic type S is an entityClass
        NearbyRandom nearbyRandom = NearbyRandomFactory.create(nearbySelectionConfig).buildNearbyRandom(randomSelection);

        if (nearbySelectionConfig.getOriginSubListSelectorConfig() == null) {
            throw new IllegalArgumentException("The subListSelector (" + config
                    + ")'s nearbySelectionConfig (" + nearbySelectionConfig
                    + ") requires an originSubListSelector.");
        }
        SubListSelector<Solution_> replayingOriginSubListSelector = SubListSelectorFactory
                .<Solution_> create(nearbySelectionConfig.getOriginSubListSelectorConfig())
                // Entity selector not needed for replaying selector.
                .buildSubListSelector(configPolicy, null, minimumCacheType, resolvedSelectionOrder);
        return new NearSubListNearbySubListSelector<>(
                subListSelector,
                replayingOriginSubListSelector,
                nearbyDistanceMeter,
                nearbyRandom);
    }

    private static Optional<Pair<String, Object>>
            randomDistributionNearbyLimitation(NearbySelectionConfig nearbySelectionConfig) {
        if (nearbySelectionConfig.getBlockDistributionSizeRatio() != null
                && nearbySelectionConfig.getBlockDistributionSizeRatio() < 1) {
            return Optional.of(Pair.of("blockDistributionSizeRatio", nearbySelectionConfig.getBlockDistributionSizeRatio()));
        }
        if (nearbySelectionConfig.getBlockDistributionSizeMaximum() != null) {
            return Optional
                    .of(Pair.of("blockDistributionSizeMaximum", nearbySelectionConfig.getBlockDistributionSizeMaximum()));
        }
        if (nearbySelectionConfig.getLinearDistributionSizeMaximum() != null) {
            return Optional
                    .of(Pair.of("linearDistributionSizeMaximum", nearbySelectionConfig.getLinearDistributionSizeMaximum()));
        }
        if (nearbySelectionConfig.getParabolicDistributionSizeMaximum() != null) {
            return Optional.of(
                    Pair.of("parabolicDistributionSizeMaximum", nearbySelectionConfig.getParabolicDistributionSizeMaximum()));
        }
        return Optional.empty();
    }

    @Override
    public <Solution_> DestinationSelector<Solution_> applyNearbySelection(DestinationSelectorConfig config,
            HeuristicConfigPolicy<Solution_> configPolicy, SelectionCacheType minimumCacheType,
            SelectionOrder resolvedSelectionOrder, ElementDestinationSelector<Solution_> destinationSelector) {
        NearbySelectionConfig nearbySelectionConfig = config.getNearbySelectionConfig();
        nearbySelectionConfig.validateNearby(minimumCacheType, resolvedSelectionOrder);

        boolean randomSelection = resolvedSelectionOrder.toRandomSelectionBoolean();

        NearbyDistanceMeter<?, ?> nearbyDistanceMeter =
                configPolicy.getClassInstanceCache().newInstance(nearbySelectionConfig,
                        "nearbyDistanceMeterClass", nearbySelectionConfig.getNearbyDistanceMeterClass());
        // TODO Check nearbyDistanceMeterClass.getGenericInterfaces() to confirm generic type S is an entityClass
        NearbyRandom nearbyRandom = NearbyRandomFactory.create(nearbySelectionConfig).buildNearbyRandom(randomSelection);

        if (nearbySelectionConfig.getOriginValueSelectorConfig() != null) {
            ValueSelector<Solution_> originValueSelector = ValueSelectorFactory
                    .<Solution_> create(nearbySelectionConfig.getOriginValueSelectorConfig())
                    .buildValueSelector(configPolicy, destinationSelector.getEntityDescriptor(), minimumCacheType,
                            resolvedSelectionOrder);
            return new NearValueNearbyDestinationSelector<>(
                    destinationSelector,
                    ((EntityIndependentValueSelector<Solution_>) originValueSelector),
                    nearbyDistanceMeter,
                    nearbyRandom,
                    randomSelection);
        } else if (nearbySelectionConfig.getOriginSubListSelectorConfig() != null) {
            SubListSelector<Solution_> subListSelector = SubListSelectorFactory
                    .<Solution_> create(nearbySelectionConfig.getOriginSubListSelectorConfig())
                    // Entity selector not needed for replaying selector.
                    .buildSubListSelector(configPolicy, null, minimumCacheType, resolvedSelectionOrder);
            return new NearSubListNearbyDestinationSelector<>(
                    destinationSelector,
                    subListSelector,
                    nearbyDistanceMeter,
                    nearbyRandom,
                    randomSelection);
        } else {
            throw new IllegalArgumentException("The destinationSelector (" + config
                    + ")'s nearbySelectionConfig (" + nearbySelectionConfig
                    + ") requires an originSubListSelector or an originValueSelector.");
        }
    }

}
