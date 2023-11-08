package ai.timefold.solver.core.enterprise;

import java.util.ServiceLoader;
import java.util.function.BiFunction;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.common.nearby.NearbySelectionConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.DestinationSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.SubListSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.partitionedsearch.PartitionedSearchPhaseConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.constructionheuristic.decider.ConstructionHeuristicDecider;
import ai.timefold.solver.core.impl.constructionheuristic.decider.forager.ConstructionHeuristicForager;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.ElementDestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.RandomSubListSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubListSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.MoveSelector;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;
import ai.timefold.solver.core.impl.localsearch.decider.LocalSearchDecider;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.Acceptor;
import ai.timefold.solver.core.impl.localsearch.decider.forager.LocalSearchForager;
import ai.timefold.solver.core.impl.partitionedsearch.PartitionedSearchPhase;
import ai.timefold.solver.core.impl.solver.termination.Termination;

public interface TimefoldSolverEnterpriseService {

    static String identifySolverVersion() {
        var packaging = TimefoldSolverEnterpriseService.load() == null ? "Community Edition" : "Enterprise Edition";
        var version = SolverFactory.class.getPackage().getImplementationVersion();
        return packaging + (version == null ? " (Development snapshot)" : " " + version);
    }

    static TimefoldSolverEnterpriseService load() {
        var serviceLoader = ServiceLoader.load(TimefoldSolverEnterpriseService.class);
        var iterator = serviceLoader.iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    static TimefoldSolverEnterpriseService loadOrFail(String feature, String workaround) {
        var service = load();
        if (service == null) {
            throw new IllegalStateException("""
                     %s requested but Timefold Solver Enterprise Edition not found on classpath
                     Either add the ai.timefold.solver.enterprise:timefold-solver-enterprise-core dependency,
                     or %s.
                    "Note: Timefold Solver Enterprise Edition is a commercial product."""
                    .formatted(feature, workaround));
        }
        return service;
    }

    <Solution_> ConstructionHeuristicDecider<Solution_> buildConstructionHeuristic(int moveThreadCount,
            Termination<Solution_> termination, ConstructionHeuristicForager<Solution_> forager,
            EnvironmentMode environmentMode, HeuristicConfigPolicy<Solution_> configPolicy);

    <Solution_> LocalSearchDecider<Solution_> buildLocalSearch(int moveThreadCount, Termination<Solution_> termination,
            MoveSelector<Solution_> moveSelector, Acceptor<Solution_> acceptor, LocalSearchForager<Solution_> forager,
            EnvironmentMode environmentMode, HeuristicConfigPolicy<Solution_> configPolicy);

    <Solution_> PartitionedSearchPhase<Solution_> buildPartitionedSearch(int phaseIndex,
            PartitionedSearchPhaseConfig phaseConfig, HeuristicConfigPolicy<Solution_> solverConfigPolicy,
            Termination<Solution_> solverTermination,
            BiFunction<HeuristicConfigPolicy<Solution_>, Termination<Solution_>, Termination<Solution_>> phaseTerminationFunction);

    <Solution_> EntitySelector<Solution_> applyNearbySelection(EntitySelectorConfig entitySelectorConfig,
            HeuristicConfigPolicy<Solution_> configPolicy, NearbySelectionConfig nearbySelectionConfig,
            SelectionCacheType minimumCacheType, SelectionOrder resolvedSelectionOrder,
            EntitySelector<Solution_> entitySelector);

    <Solution_> ValueSelector<Solution_> applyNearbySelection(ValueSelectorConfig valueSelectorConfig,
            HeuristicConfigPolicy<Solution_> configPolicy, EntityDescriptor<Solution_> entityDescriptor,
            SelectionCacheType minimumCacheType, SelectionOrder resolvedSelectionOrder, ValueSelector<Solution_> valueSelector);

    <Solution_> SubListSelector<Solution_> applyNearbySelection(SubListSelectorConfig subListSelectorConfig,
            HeuristicConfigPolicy<Solution_> configPolicy, SelectionCacheType minimumCacheType,
            SelectionOrder resolvedSelectionOrder, RandomSubListSelector<Solution_> subListSelector);

    <Solution_> DestinationSelector<Solution_> applyNearbySelection(DestinationSelectorConfig destinationSelectorConfig,
            HeuristicConfigPolicy<Solution_> configPolicy, SelectionCacheType minimumCacheType,
            SelectionOrder resolvedSelectionOrder, ElementDestinationSelector<Solution_> destinationSelector);

}
