package ai.timefold.solver.core.enterprise;

import java.lang.reflect.InvocationTargetException;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionCacheType;
import ai.timefold.solver.core.config.heuristic.selector.common.SelectionOrder;
import ai.timefold.solver.core.config.heuristic.selector.common.nearby.NearbySelectionConfig;
import ai.timefold.solver.core.config.heuristic.selector.entity.EntitySelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.DestinationSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.list.SubListSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.MultistageMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.move.generic.list.ListMultistageMoveSelectorConfig;
import ai.timefold.solver.core.config.heuristic.selector.value.ValueSelectorConfig;
import ai.timefold.solver.core.config.partitionedsearch.PartitionedSearchPhaseConfig;
import ai.timefold.solver.core.config.solver.EnvironmentMode;
import ai.timefold.solver.core.impl.bavet.common.InnerConstraintProfiler;
import ai.timefold.solver.core.impl.constructionheuristic.decider.ConstructionHeuristicDecider;
import ai.timefold.solver.core.impl.constructionheuristic.decider.forager.ConstructionHeuristicForager;
import ai.timefold.solver.core.impl.domain.entity.descriptor.EntityDescriptor;
import ai.timefold.solver.core.impl.domain.variable.declarative.TopologicalOrderGraph;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.ElementDestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.RandomSubListSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubListSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;
import ai.timefold.solver.core.impl.localsearch.decider.LocalSearchDecider;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.Acceptor;
import ai.timefold.solver.core.impl.localsearch.decider.forager.LocalSearchForager;
import ai.timefold.solver.core.impl.neighborhood.MoveRepository;
import ai.timefold.solver.core.impl.partitionedsearch.PartitionedSearchPhase;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.solver.termination.SolverTermination;

public interface TimefoldSolverEnterpriseService {

    final class InstanceCarrier {

        // Workaround to be able to have a lazy singleton inside of an interface.
        // Otherwise load() would be calling the reflective constructor every time, which is expensive.
        private static volatile TimefoldSolverEnterpriseService INSTANCE;

    }

    String SOLVER_NAME = "Timefold Solver";
    String COMMUNITY_NAME = "Community Edition";
    String COMMUNITY_COORDINATES = "ai.timefold.solver:timefold-solver-core";
    String ENTERPRISE_NAME = "Enterprise Edition";
    String ENTERPRISE_COORDINATES = "ai.timefold.solver.enterprise:timefold-solver-enterprise-core";
    String DEVELOPMENT_SNAPSHOT = "Development Snapshot";

    static String identifySolverVersion() {
        var packaging = COMMUNITY_NAME;
        try {
            load();
            packaging = ENTERPRISE_NAME;
        } catch (Exception e) {
            // No need to do anything, just checking if Enterprise exists.
        }
        var version = getVersionString(SolverFactory.class);
        return packaging + " " + version;
    }

    @SuppressWarnings("unchecked")
    static TimefoldSolverEnterpriseService load() throws ClassNotFoundException, NoSuchMethodException,
            InvocationTargetException, IllegalAccessException, InstantiationException {
        if (InstanceCarrier.INSTANCE == null) {
            synchronized (InstanceCarrier.class) {
                if (InstanceCarrier.INSTANCE == null) {
                    // Avoids ServiceLoader by using reflection directly.
                    var clz = (Class<? extends TimefoldSolverEnterpriseService>) Class
                            .forName("ai.timefold.solver.enterprise.core.DefaultTimefoldSolverEnterpriseService");
                    var ctor = clz.getDeclaredConstructor(Function.class);
                    InstanceCarrier.INSTANCE =
                            ctor.newInstance((Function<Class<?>, String>) TimefoldSolverEnterpriseService::getVersionString);
                }
            }
        }
        return InstanceCarrier.INSTANCE;
    }

    static String getVersionString(Class<?> clz) {
        var version = clz.getPackage().getImplementationVersion();
        return (version == null ? DEVELOPMENT_SNAPSHOT : "v" + version);
    }

    static TimefoldSolverEnterpriseService loadOrFail(Feature feature) {
        try {
            return load();
        } catch (EnterpriseLicenseException cause) {
            throw new IllegalStateException("""
                    No valid Timefold Enterprise License was found.
                    Please contact Timefold to obtain a valid license,
                    or if you believe that this message was given in error.""", cause);
        } catch (Exception cause) {
            throw new IllegalStateException("""
                    %s requested but %s %s could not be loaded.
                    Maybe add the %s dependency, or %s.
                    Note: %s %s is a commercial product.
                    Visit https://timefold.ai to find out more, or contact Timefold customer support.""".formatted(
                    feature.getName(), SOLVER_NAME, ENTERPRISE_NAME, feature.getWorkaround(), ENTERPRISE_COORDINATES,
                    SOLVER_NAME, ENTERPRISE_NAME), cause);
        }
    }

    static <T> T loadOrDefault(Function<TimefoldSolverEnterpriseService, T> builder, Supplier<T> defaultValue) {
        try {
            return builder.apply(load());
        } catch (Exception e) {
            return defaultValue.get();
        }
    }

    TopologicalOrderGraph buildTopologyGraph(int size);

    /**
     * Will create new classes that apply node-sharing to the given {@link ConstraintProvider}.
     * To reuse these classes, make sure to cache the returned {@link ConstraintProviderNodeSharer}.
     *
     * @return never null
     */
    ConstraintProviderNodeSharer createNodeSharer();

    <Solution_> ConstructionHeuristicDecider<Solution_> buildConstructionHeuristic(PhaseTermination<Solution_> termination,
            ConstructionHeuristicForager<Solution_> forager, HeuristicConfigPolicy<Solution_> configPolicy);

    <Solution_> LocalSearchDecider<Solution_> buildLocalSearch(int moveThreadCount, PhaseTermination<Solution_> termination,
            MoveRepository<Solution_> moveRepository, Acceptor<Solution_> acceptor, LocalSearchForager<Solution_> forager,
            EnvironmentMode environmentMode, HeuristicConfigPolicy<Solution_> configPolicy);

    <Solution_> PartitionedSearchPhase<Solution_> buildPartitionedSearch(int phaseIndex,
            PartitionedSearchPhaseConfig phaseConfig, HeuristicConfigPolicy<Solution_> solverConfigPolicy,
            SolverTermination<Solution_> solverTermination,
            BiFunction<HeuristicConfigPolicy<Solution_>, SolverTermination<Solution_>, PhaseTermination<Solution_>> phaseTerminationFunction);

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

    <Solution_> AbstractMoveSelectorFactory<Solution_, MultistageMoveSelectorConfig>
            buildBasicMultistageMoveSelectorFactory(MultistageMoveSelectorConfig moveSelectorConfig);

    <Solution_> AbstractMoveSelectorFactory<Solution_, ListMultistageMoveSelectorConfig>
            buildListMultistageMoveSelectorFactory(ListMultistageMoveSelectorConfig moveSelectorConfig);

    InnerConstraintProfiler buildConstraintProfiler();

    enum Feature {
        MULTITHREADED_SOLVING("Multi-threaded solving", "remove moveThreadCount from solver configuration"),
        PARTITIONED_SEARCH("Partitioned search", "remove partitioned search phase from solver configuration"),
        NEARBY_SELECTION("Nearby selection", "remove nearby selection from solver configuration"),
        AUTOMATIC_NODE_SHARING("Automatic node sharing", "remove automatic node sharing from solver configuration"),
        MULTISTAGE_MOVE("Multistage move selector",
                "remove multistageMoveSelector and/or listMultistageMoveSelector from the solver configuration"),
        CONSTRAINT_PROFILING("Constraint profiling", "remove constraintStreamProfilingEnabled from the solver configuration");

        private final String name;
        private final String workaround;

        Feature(String name, String workaround) {
            this.name = name;
            this.workaround = workaround;
        }

        public String getName() {
            return name;
        }

        public String getWorkaround() {
            return workaround;
        }

    }

    interface ConstraintProviderNodeSharer {

        <T extends ConstraintProvider> Class<T> buildNodeSharedConstraintProvider(Class<T> constraintProviderClass);

    }

    final class EnterpriseLicenseException extends RuntimeException {

        public EnterpriseLicenseException(String message) {
            super(message);
        }

        public EnterpriseLicenseException(String message, Throwable cause) {
            super(message, cause);
        }

    }

}
