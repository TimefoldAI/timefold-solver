package ai.timefold.solver.core.enterprise;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.score.analysis.ScoreAnalysis;
import ai.timefold.solver.core.api.score.stream.ConstraintProvider;
import ai.timefold.solver.core.api.score.stream.ConstraintRef;
import ai.timefold.solver.core.api.solver.RecommendedAssignment;
import ai.timefold.solver.core.api.solver.ScoreAnalysisFetchPolicy;
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
import ai.timefold.solver.core.impl.evolutionaryalgorithm.common.state.SolutionState;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.decider.EvolutionaryDecider;
import ai.timefold.solver.core.impl.evolutionaryalgorithm.decider.HybridGeneticSearchWorkerContext;
import ai.timefold.solver.core.impl.heuristic.HeuristicConfigPolicy;
import ai.timefold.solver.core.impl.heuristic.selector.entity.EntitySelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.DestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.ElementDestinationSelector;
import ai.timefold.solver.core.impl.heuristic.selector.list.SubListSelector;
import ai.timefold.solver.core.impl.heuristic.selector.move.AbstractMoveSelectorFactory;
import ai.timefold.solver.core.impl.heuristic.selector.value.ValueSelector;
import ai.timefold.solver.core.impl.localsearch.decider.LocalSearchDecider;
import ai.timefold.solver.core.impl.localsearch.decider.acceptor.Acceptor;
import ai.timefold.solver.core.impl.localsearch.decider.forager.LocalSearchForager;
import ai.timefold.solver.core.impl.neighborhood.MoveRepository;
import ai.timefold.solver.core.impl.partitionedsearch.PartitionedSearchPhase;
import ai.timefold.solver.core.impl.score.constraint.ConstraintMatchTotal;
import ai.timefold.solver.core.impl.score.director.InnerScore;
import ai.timefold.solver.core.impl.score.director.InnerScoreDirector;
import ai.timefold.solver.core.impl.solver.DefaultSolverFactory;
import ai.timefold.solver.core.impl.solver.recaller.BestSolutionRecaller;
import ai.timefold.solver.core.impl.solver.termination.PhaseTermination;
import ai.timefold.solver.core.impl.solver.termination.SolverTermination;
import ai.timefold.solver.core.impl.util.SolverVersionUtils;
import ai.timefold.solver.core.preview.api.domain.metamodel.PlanningSolutionMetaModel;
import ai.timefold.solver.core.preview.api.domain.solution.diff.PlanningSolutionDiff;

import org.jspecify.annotations.Nullable;

public interface TimefoldSolverEnterpriseService {

    final class InstanceCarrier {

        // Workaround to be able to have a lazy singleton inside of an interface.
        // Otherwise load() would be calling the reflective constructor every time, which is expensive.
        private static volatile TimefoldSolverEnterpriseService INSTANCE;

    }

    String COMMUNITY_NAME = SolverVersionUtils.COMMUNITY_NAME;
    String ENTERPRISE_NAME = SolverVersionUtils.ENTERPRISE_NAME;
    String ENTERPRISE_COORDINATES = "ai.timefold.solver.enterprise:timefold-solver-enterprise-core";

    /**
     * Returns the solver edition and version string without a git ref, for backward compatibility.
     * Any {@code -rc-N} suffix is stripped from the version number.
     *
     * @return e.g. {@code "Timefold Solver Community Edition v1.2.3"} or
     *         {@code "Timefold Solver Community Edition Development Snapshot"}
     */
    static String identifySolverVersion() {
        TimefoldSolverEnterpriseService service = null;
        try {
            service = load();
        } catch (Exception e) {
            // No enterprise edition on the classpath.
        }
        var editionName = service == null ? COMMUNITY_NAME : ENTERPRISE_NAME;
        return SolverVersionUtils.banner(editionName, SolverVersionUtils.CORE_GIT_PROPERTIES, SolverFactory.class);
    }

    /**
     * Returns the solver edition, version, and short Git commit SHA(s).
     * Any {@code -rc-N} suffix is stripped from the version number.
     *
     * @return e.g. {@code "Timefold Solver Community Edition v1.2.3 (a1b2c3d)"} or
     *         {@code "Timefold Solver Enterprise Edition v1.2.3 (core a1b2c3d, enterprise e4f5g6h)"}
     */
    static String identifySolverVersionWithGitRef() {
        var coreRef = SolverVersionUtils.gitRefOf(SolverVersionUtils.CORE_GIT_PROPERTIES);
        TimefoldSolverEnterpriseService service = null;
        try {
            service = load();
        } catch (Exception e) {
            // No enterprise edition on the classpath.
        }
        if (service == null) {
            return SolverVersionUtils.communityBannerWithGitRef(SolverVersionUtils.CORE_GIT_PROPERTIES, SolverFactory.class,
                    coreRef);
        } else {
            return SolverVersionUtils.enterpriseBannerWithGitRef(SolverVersionUtils.CORE_GIT_PROPERTIES, SolverFactory.class,
                    coreRef, service.getGitRef());
        }
    }

    /**
     * Returns the short Git commit SHA of the enterprise JAR, or {@code null} if unavailable.
     * Overridden by the enterprise implementation.
     */
    default @Nullable String getGitRef() {
        throw new UnsupportedOperationException();
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
                    var ctor = clz.getDeclaredConstructor();
                    InstanceCarrier.INSTANCE = ctor.newInstance();
                }
            }
        }
        return InstanceCarrier.INSTANCE;
    }

    static TimefoldSolverEnterpriseService loadOrFail(Feature feature) {
        try {
            return load();
        } catch (EnterpriseLicenseException cause) {
            throw new IllegalStateException("""
                    No valid Timefold License was found.
                    Please contact Timefold to obtain a valid license,
                    or if you believe that this message was given in error.""", cause);
        } catch (EnterpriseProductException cause) {
            throw new IllegalStateException("""
                    Valid Timefold License was found, but it does not entitle you to run "%s".
                    Maybe %s.
                    Please contact Timefold to obtain an applicable license,
                    or if you believe that this message was given in error."""
                    .formatted(feature.getName(), feature.getWorkaround()), cause);
        } catch (Exception cause) {
            throw new IllegalStateException("""
                    A commercial feature "%s" was requested but it could not be loaded.
                    Maybe add the %s dependency, or %s.
                    Please contact Timefold to obtain an applicable license,
                    or if you believe that this message was given in error."""
                    .formatted(feature.getName(), ENTERPRISE_COORDINATES, feature.getWorkaround()),
                    cause);
        }
    }

    static <T> T loadOrDefault(Function<TimefoldSolverEnterpriseService, T> builder, Supplier<T> defaultValue) {
        try {
            return builder.apply(load());
        } catch (Exception e) {
            return defaultValue.get();
        }
    }

    static <T> @Nullable T loadOrNull(Function<TimefoldSolverEnterpriseService, T> builder) {
        try {
            return builder.apply(load());
        } catch (Exception e) {
            return null;
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

    <Solution_, Score_ extends Score<Score_>, State_ extends SolutionState<Solution_, Score_>>
            EvolutionaryDecider<Solution_, Score_> buildHybridGeneticSearch(HeuristicConfigPolicy<Solution_> solverConfigPolicy,
                    int workerCount, int populationSize, int generationSize, int eliteGroupSize, int populationRestartCount,
                    List<HybridGeneticSearchWorkerContext<Solution_, Score_, State_>> workerContextList,
                    PhaseTermination<Solution_> phaseTermination, BestSolutionRecaller<Solution_> bestSolutionRecaller);

    <Solution_> EntitySelector<Solution_> applyNearbySelection(EntitySelectorConfig entitySelectorConfig,
            HeuristicConfigPolicy<Solution_> configPolicy, NearbySelectionConfig nearbySelectionConfig,
            SelectionCacheType minimumCacheType, SelectionOrder resolvedSelectionOrder,
            EntitySelector<Solution_> entitySelector);

    <Solution_> ValueSelector<Solution_> applyNearbySelection(ValueSelectorConfig valueSelectorConfig,
            HeuristicConfigPolicy<Solution_> configPolicy, EntityDescriptor<Solution_> entityDescriptor,
            SelectionCacheType minimumCacheType, SelectionOrder resolvedSelectionOrder, ValueSelector<Solution_> valueSelector);

    <Solution_> SubListSelector<Solution_> applyNearbySelection(SubListSelectorConfig subListSelectorConfig,
            HeuristicConfigPolicy<Solution_> configPolicy, SelectionCacheType minimumCacheType,
            SelectionOrder resolvedSelectionOrder, SubListSelector<Solution_> subListSelector);

    <Solution_> DestinationSelector<Solution_> applyNearbySelection(DestinationSelectorConfig destinationSelectorConfig,
            HeuristicConfigPolicy<Solution_> configPolicy, SelectionCacheType minimumCacheType,
            SelectionOrder resolvedSelectionOrder, ElementDestinationSelector<Solution_> destinationSelector);

    <Solution_> AbstractMoveSelectorFactory<Solution_, MultistageMoveSelectorConfig>
            buildBasicMultistageMoveSelectorFactory(MultistageMoveSelectorConfig moveSelectorConfig);

    <Solution_> AbstractMoveSelectorFactory<Solution_, ListMultistageMoveSelectorConfig>
            buildListMultistageMoveSelectorFactory(ListMultistageMoveSelectorConfig moveSelectorConfig);

    InnerConstraintProfiler buildConstraintProfiler();

    <Score_ extends Score<Score_>> ScoreAnalysis<Score_> analyze(InnerScore<Score_> state,
            Map<ConstraintRef, ConstraintMatchTotal<Score_>> constraintMatchTotalMap, ScoreAnalysisFetchPolicy fetchPolicy);

    <Solution_> PlanningSolutionDiff<Solution_> solutionDiff(PlanningSolutionMetaModel<Solution_> metaModel,
            Solution_ oldSolution, Solution_ newSolution);

    <Solution_, Score_ extends Score<Score_>, In_, Out_>
            Function<InnerScoreDirector<Solution_, Score_>, List<RecommendedAssignment<Out_, Score_>>> buildRecommender(
                    DefaultSolverFactory<Solution_> solverFactory, Solution_ solution, In_ evaluatedEntityOrElement,
                    Function<In_, @Nullable Out_> propositionFunction,
                    ScoreAnalysisFetchPolicy fetchPolicy);

    enum Feature {
        MULTITHREADED_SOLVING("Multi-threaded solving", "remove moveThreadCount from solver configuration"),
        PARTITIONED_SEARCH("Partitioned search", "remove partitioned search phase from solver configuration"),
        NEARBY_SELECTION("Nearby selection", "remove nearby selection from solver configuration"),
        AUTOMATIC_NODE_SHARING("Automatic node sharing", "remove automatic node sharing from solver configuration"),
        MULTISTAGE_MOVE("Multistage move selector",
                "remove multistageMoveSelector and/or listMultistageMoveSelector from the solver configuration"),
        CONSTRAINT_PROFILING("Constraint profiling", "remove constraintStreamProfilingEnabled from the solver configuration"),
        SCORE_ANALYSIS("Score analysis", "do not use SolutionManager's analyze() method"),
        RECOMMENDATIONS("Recommendations", "do not use SolutionManager's recommendAssignment() method"),
        EVOLUTIONARY_ALGORITHM("Evolutionary Algorithm",
                "remove the worker count property from the evolutionary algorithm configuration");

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

    final class EnterpriseProductException extends RuntimeException {

        public EnterpriseProductException(String message) {
            super(message);
        }

        public EnterpriseProductException(String message, Throwable cause) {
            super(message, cause);
        }

    }

}
