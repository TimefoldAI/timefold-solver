package ai.timefold.solver.quarkus;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.api.score.Score;
import ai.timefold.solver.core.api.solver.SolutionManager;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.SolverManagerConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.quarkus.config.SolverRuntimeConfig;
import ai.timefold.solver.quarkus.config.TimefoldRuntimeConfig;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class TimefoldRecorder {

    public Supplier<SolverConfig> solverConfigSupplier(final String solverName,
            final SolverConfig solverConfig,
            final TimefoldRuntimeConfig timefoldRuntimeConfig,
            Map<String, RuntimeValue<MemberAccessor>> generatedGizmoMemberAccessorMap,
            Map<String, RuntimeValue<SolutionCloner>> generatedGizmoSolutionClonerMap) {
        return () -> {
            updateSolverConfigWithRuntimeProperties(solverName, solverConfig, timefoldRuntimeConfig);
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

    public Supplier<SolverManagerConfig> solverManagerConfig(final SolverManagerConfig solverManagerConfig,
            final TimefoldRuntimeConfig timefoldRuntimeConfig) {
        return () -> {
            updateSolverManagerConfigWithRuntimeProperties(solverManagerConfig, timefoldRuntimeConfig);
            return solverManagerConfig;
        };
    }

    public <Solution_> Supplier<SolverFactory<Solution_>> solverFactory(final String solverConfigName) {
        return () -> {
            SolverConfig solverConfig = (SolverConfig) Arc.container().instance(solverConfigName).get();
            return (SolverFactory<Solution_>) SolverFactory.create(solverConfig);
        };
    }

    public <Solution_, ProblemId_> Supplier<SolverManager<Solution_, ProblemId_>> solverManager(final String solverFactoryName,
            final SolverManagerConfig solverManagerConfig) {
        return () -> {
            SolverFactory<Solution_> solverFactory =
                    (SolverFactory<Solution_>) Arc.container().instance(solverFactoryName).get();
            return (SolverManager<Solution_, ProblemId_>) SolverManager.create(solverFactory, solverManagerConfig);
        };
    }

    public <Solution_, Score_ extends Score<Score_>> Supplier<SolutionManager<Solution_, Score_>>
            solutionManager(final String solverFactoryName) {
        return () -> {
            SolverFactory<Solution_> solverFactory =
                    (SolverFactory<Solution_>) Arc.container().instance(solverFactoryName).get();
            return (SolutionManager<Solution_, Score_>) SolutionManager.create(solverFactory);
        };
    }

    private void updateSolverConfigWithRuntimeProperties(String solverName, SolverConfig solverConfig,
            TimefoldRuntimeConfig timefoldRunTimeConfig) {
        TerminationConfig terminationConfig = solverConfig.getTerminationConfig();
        if (terminationConfig == null) {
            terminationConfig = new TerminationConfig();
            solverConfig.setTerminationConfig(terminationConfig);
        }
        timefoldRunTimeConfig.getSolverRuntimeConfig(solverName).flatMap(config -> config.termination().spentLimit())
                .ifPresent(terminationConfig::setSpentLimit);
        timefoldRunTimeConfig.getSolverRuntimeConfig(solverName).flatMap(config -> config.termination().unimprovedSpentLimit())
                .ifPresent(terminationConfig::setUnimprovedSpentLimit);
        timefoldRunTimeConfig.getSolverRuntimeConfig(solverName).flatMap(config -> config.termination().bestScoreLimit())
                .ifPresent(terminationConfig::setBestScoreLimit);
        timefoldRunTimeConfig.getSolverRuntimeConfig(solverName).flatMap(SolverRuntimeConfig::moveThreadCount)
                .ifPresent(solverConfig::setMoveThreadCount);
    }

    private void updateSolverManagerConfigWithRuntimeProperties(SolverManagerConfig solverManagerConfig,
            TimefoldRuntimeConfig timefoldRunTimeConfig) {
        timefoldRunTimeConfig.solverManager().parallelSolverCount().ifPresent(solverManagerConfig::setParallelSolverCount);
    }

}
