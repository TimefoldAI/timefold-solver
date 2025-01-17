package ai.timefold.solver.quarkus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.inject.Named;

import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.constructionheuristic.ConstructionHeuristicPhaseConfig;
import ai.timefold.solver.core.config.localsearch.LocalSearchPhaseConfig;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.config.solver.termination.DiminishedReturnsTerminationConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.quarkus.config.DiminishedReturnsRuntimeConfig;
import ai.timefold.solver.quarkus.config.SolverRuntimeConfig;
import ai.timefold.solver.quarkus.config.TimefoldRuntimeConfig;

import org.jspecify.annotations.Nullable;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class TimefoldRecorder {
    final TimefoldRuntimeConfig timefoldRuntimeConfig;

    public TimefoldRecorder(final TimefoldRuntimeConfig timefoldRuntimeConfig) {
        this.timefoldRuntimeConfig = timefoldRuntimeConfig;
    }

    public static void assertNoUnmatchedProperties(Set<String> expectedNames, Set<String> actualNames) {
        var allExpectedNames = new HashSet<>(expectedNames);
        allExpectedNames.add(TimefoldRuntimeConfig.DEFAULT_SOLVER_NAME);

        if (!allExpectedNames.containsAll(actualNames)) {
            var expectedNamesSorted = expectedNames.stream()
                    .sorted()
                    .toList();
            var unmatchedNamesSorted = actualNames.stream()
                    .filter(Predicate.not(allExpectedNames::contains))
                    .sorted()
                    .toList();
            throw new IllegalStateException("""
                    Some names defined in properties (%s) do not have \
                    a corresponding @%s injection point (%s). Maybe you \
                    misspelled them?
                    """.formatted(unmatchedNamesSorted, Named.class.getSimpleName(),
                    expectedNamesSorted));
        }
    }

    public void assertNoUnmatchedRuntimeProperties(Set<String> names) {
        assertNoUnmatchedProperties(names, timefoldRuntimeConfig.solver().keySet());
    }

    public Supplier<SolverConfig> solverConfigSupplier(final String solverName,
            final SolverConfig solverConfig,
            Map<String, RuntimeValue<MemberAccessor>> generatedGizmoMemberAccessorMap,
            Map<String, RuntimeValue<SolutionCloner>> generatedGizmoSolutionClonerMap) {
        return () -> {
            updateSolverConfigWithRuntimeProperties(solverName, solverConfig);
            Map<String, MemberAccessor> memberAccessorMap = new HashMap<>();
            Map<String, SolutionCloner> solutionClonerMap = new HashMap<>();
            generatedGizmoMemberAccessorMap
                    .forEach((className, runtimeValue) -> memberAccessorMap.put(className, runtimeValue.getValue()));
            generatedGizmoSolutionClonerMap
                    .forEach((className, runtimeValue) -> solutionClonerMap.put(className, runtimeValue.getValue()));

            solverConfig.setGizmoMemberAccessorMap(memberAccessorMap);
            solverConfig.setGizmoSolutionClonerMap(solutionClonerMap);
            return solverConfig;
        };
    }

    public Supplier<SolverManagerConfig> solverManagerConfig(final SolverManagerConfig solverManagerConfig) {
        return () -> {
            updateSolverManagerConfigWithRuntimeProperties(solverManagerConfig);
            return solverManagerConfig;
        };
    }

    public <Solution_, ProblemId_> Supplier<SolverManager<Solution_, ProblemId_>> solverManager(final String solverName,
            final SolverConfig solverConfig,
            Map<String, RuntimeValue<MemberAccessor>> generatedGizmoMemberAccessorMap,
            Map<String, RuntimeValue<SolutionCloner>> generatedGizmoSolutionClonerMap) {
        return () -> {
            updateSolverConfigWithRuntimeProperties(solverName, solverConfig);
            Map<String, MemberAccessor> memberAccessorMap = new HashMap<>();
            Map<String, SolutionCloner> solutionClonerMap = new HashMap<>();
            generatedGizmoMemberAccessorMap
                    .forEach((className, runtimeValue) -> memberAccessorMap.put(className, runtimeValue.getValue()));
            generatedGizmoSolutionClonerMap
                    .forEach((className, runtimeValue) -> solutionClonerMap.put(className, runtimeValue.getValue()));

            solverConfig.setGizmoMemberAccessorMap(memberAccessorMap);
            solverConfig.setGizmoSolutionClonerMap(solutionClonerMap);

            SolverManagerConfig solverManagerConfig = new SolverManagerConfig();
            updateSolverManagerConfigWithRuntimeProperties(solverManagerConfig);

            SolverFactory<Solution_> solverFactory = SolverFactory.create(solverConfig);

            return (SolverManager<Solution_, ProblemId_>) SolverManager.create(solverFactory, solverManagerConfig);
        };
    }

    private void updateSolverConfigWithRuntimeProperties(String solverName, SolverConfig solverConfig) {
        updateSolverConfigWithRuntimeProperties(solverConfig, timefoldRuntimeConfig
                .getSolverRuntimeConfig(solverName).orElse(null));
    }

    public static void updateSolverConfigWithRuntimeProperties(SolverConfig solverConfig,
            @Nullable SolverRuntimeConfig solverRuntimeConfig) {
        TerminationConfig terminationConfig = solverConfig.getTerminationConfig();
        if (terminationConfig == null) {
            terminationConfig = new TerminationConfig();
            solverConfig.setTerminationConfig(terminationConfig);
        }
        var maybeSolverRuntimeConfig = Optional.ofNullable(solverRuntimeConfig);
        maybeSolverRuntimeConfig.flatMap(config -> config.termination().spentLimit())
                .ifPresent(terminationConfig::setSpentLimit);
        maybeSolverRuntimeConfig.flatMap(config -> config.termination().unimprovedSpentLimit())
                .ifPresent(terminationConfig::setUnimprovedSpentLimit);
        maybeSolverRuntimeConfig.flatMap(config -> config.termination().bestScoreLimit())
                .ifPresent(terminationConfig::setBestScoreLimit);
        maybeSolverRuntimeConfig.flatMap(SolverRuntimeConfig::environmentMode)
                .ifPresent(solverConfig::setEnvironmentMode);
        maybeSolverRuntimeConfig.flatMap(SolverRuntimeConfig::daemon)
                .ifPresent(solverConfig::setDaemon);
        maybeSolverRuntimeConfig.flatMap(SolverRuntimeConfig::moveThreadCount)
                .ifPresent(solverConfig::setMoveThreadCount);
        maybeSolverRuntimeConfig.flatMap(config -> config.termination().diminishedReturns())
                .ifPresent(diminishedReturnsConfig -> setDiminishedReturns(solverConfig, diminishedReturnsConfig));
    }

    private static void setDiminishedReturns(SolverConfig solverConfig,
            DiminishedReturnsRuntimeConfig diminishedReturnsRuntimeConfig) {
        // If we are here, at least one of enabled, sliding-window, or minimum-improvement-ratio
        // is set.
        if (!diminishedReturnsRuntimeConfig.enabled().orElse(
                diminishedReturnsRuntimeConfig.minimumImprovementRatio().isPresent() ||
                        diminishedReturnsRuntimeConfig.slidingWindowDuration().isPresent())) {
            return;
        }
        if (solverConfig.getPhaseConfigList() != null) {
            throw new IllegalArgumentException("%s properties cannot be used when phases are configured."
                    .formatted("quarkus.timefold.solver.termination.diminished-returns"));
        }
        var diminishedReturnsConfig = new DiminishedReturnsTerminationConfig();
        diminishedReturnsRuntimeConfig.slidingWindowDuration().ifPresent(
                diminishedReturnsConfig::setSlidingWindowDuration);
        diminishedReturnsRuntimeConfig.minimumImprovementRatio().ifPresent(
                diminishedReturnsConfig::setMinimumImprovementRatio);
        solverConfig.setPhaseConfigList(List.of(
                new ConstructionHeuristicPhaseConfig(),
                new LocalSearchPhaseConfig().withTerminationConfig(
                        new TerminationConfig()
                                .withDiminishedReturnsConfig(diminishedReturnsConfig))));
    }

    private void updateSolverManagerConfigWithRuntimeProperties(SolverManagerConfig solverManagerConfig) {
        timefoldRuntimeConfig.solverManager().parallelSolverCount().ifPresent(solverManagerConfig::setParallelSolverCount);
    }

}
