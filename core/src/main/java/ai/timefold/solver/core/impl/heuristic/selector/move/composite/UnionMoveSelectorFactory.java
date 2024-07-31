package ai.timefold.solver.core.impl.heuristic.selector.move.composite;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.move.MoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.NearbyAutoConfigurationEnabled;
import ai.timefold.solver.core.config.heuristic.selector.move.composite.UnionMoveSelectorConfig;
import ai.timefold.solver.core.config.util.ConfigUtils;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.common.decorator.SelectionProbabilityWeightFactory;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;

public class UnionMoveSelectorFactory<Solution_>
        extends AbstractCompositeMoveSelectorFactory<Solution_, UnionMoveSelectorConfig> {

    public UnionMoveSelectorFactory(UnionMoveSelectorConfig moveSelectorConfig) {
        super(moveSelectorConfig);
    }

    @Override
    protected MoveSelector<Solution_> buildBaseMoveSelector(HeuristicConfigPolicy<Solution_> configPolicy,
            SelectionCacheType minimumCacheType, boolean randomSelection) {
        var moveSelectorConfigList = new LinkedList<>(config.getMoveSelectorList());
        if (configPolicy.getNearbyDistanceMeterClass() != null) {
            for (var selectorConfig : config.getMoveSelectorList()) {
                if (selectorConfig instanceof NearbyAutoConfigurationEnabled nearbySelectorConfig) {
                    if (selectorConfig.hasNearbySelectionConfig()) {
                        throw new IllegalArgumentException(
                                """
                                        The selector configuration (%s) already includes the Nearby Selection setting, making it incompatible with the top-level property nearbyDistanceMeterClass (%s).
                                        Remove the Nearby setting from the selector configuration or remove the top-level nearbyDistanceMeterClass."""
                                        .formatted(nearbySelectorConfig, configPolicy.getNearbyDistanceMeterClass()));
                    }
                    // We delay the autoconfiguration to the deepest UnionMoveSelectorConfig node in the tree
                    // to avoid duplicating configuration
                    // when there are nested unionMoveSelector configurations
                    if (selectorConfig instanceof UnionMoveSelectorConfig) {
                        continue;
                    }
                    // Add a new configuration with Nearby Selection enabled
                    moveSelectorConfigList
                            .add(nearbySelectorConfig.enableNearbySelection(configPolicy.getNearbyDistanceMeterClass(),
                                    configPolicy.getRandom()));

                }
            }
        }
        List<MoveSelector<Solution_>> moveSelectorList =
                buildInnerMoveSelectors(moveSelectorConfigList, configPolicy, minimumCacheType, randomSelection);

        SelectionProbabilityWeightFactory<Solution_, MoveSelector<Solution_>> selectorProbabilityWeightFactory;
        if (config.getSelectorProbabilityWeightFactoryClass() != null) {
            if (!randomSelection) {
                throw new IllegalArgumentException("The moveSelectorConfig (" + config
                        + ") with selectorProbabilityWeightFactoryClass ("
                        + config.getSelectorProbabilityWeightFactoryClass()
                        + ") has non-random randomSelection (" + randomSelection + ").");
            }
            selectorProbabilityWeightFactory = ConfigUtils.newInstance(config,
                    "selectorProbabilityWeightFactoryClass", config.getSelectorProbabilityWeightFactoryClass());
        } else if (randomSelection) {
            Map<MoveSelector<Solution_>, Double> fixedProbabilityWeightMap =
                    new HashMap<>(moveSelectorConfigList.size());
            for (int i = 0; i < moveSelectorConfigList.size(); i++) {
                MoveSelectorConfig<?> innerMoveSelectorConfig = moveSelectorConfigList.get(i);
                MoveSelector<Solution_> moveSelector = moveSelectorList.get(i);
                Double fixedProbabilityWeight = innerMoveSelectorConfig.getFixedProbabilityWeight();
                if (fixedProbabilityWeight != null) {
                    fixedProbabilityWeightMap.put(moveSelector, fixedProbabilityWeight);
                }
            }
            if (fixedProbabilityWeightMap.isEmpty()) { // Will end up using UniformRandomUnionMoveIterator.
                selectorProbabilityWeightFactory = null;
            } else { // Will end up using BiasedRandomUnionMoveIterator.
                selectorProbabilityWeightFactory = new FixedSelectorProbabilityWeightFactory<>(fixedProbabilityWeightMap);
            }
        } else {
            selectorProbabilityWeightFactory = null;
        }
        return new UnionMoveSelector<>(moveSelectorList, randomSelection, selectorProbabilityWeightFactory);
    }
}
