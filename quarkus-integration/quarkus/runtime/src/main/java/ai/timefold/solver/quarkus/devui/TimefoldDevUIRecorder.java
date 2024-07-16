package ai.timefold.solver.quarkus.devui;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.io.jaxb.SolverConfigIO;
import ai.timefold.solver.quarkus.config.SolverRuntimeConfig;
import ai.timefold.solver.quarkus.config.TimefoldRuntimeConfig;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class TimefoldDevUIRecorder {

    public Supplier<DevUISolverConfig> solverConfigSupplier(Map<String, SolverConfig> allSolverConfig,
            TimefoldRuntimeConfig timefoldRuntimeConfig,
            Map<String, RuntimeValue<MemberAccessor>> generatedGizmoMemberAccessorMap,
            Map<String, RuntimeValue<SolutionCloner>> generatedGizmoSolutionClonerMap) {
        return () -> {
            DevUISolverConfig uiSolverConfig = new DevUISolverConfig();
            allSolverConfig.forEach((solverName, solverConfig) -> {
                updateSolverConfigWithRuntimeProperties(solverName, solverConfig, timefoldRuntimeConfig);
                Map<String, MemberAccessor> memberAccessorMap = new HashMap<>();
                Map<String, SolutionCloner> solutionClonerMap = new HashMap<>();
                generatedGizmoMemberAccessorMap
                        .forEach((className, runtimeValue) -> memberAccessorMap.put(className, runtimeValue.getValue()));
                generatedGizmoSolutionClonerMap
                        .forEach((className, runtimeValue) -> solutionClonerMap.put(className, runtimeValue.getValue()));

                solverConfig.setGizmoMemberAccessorMap(memberAccessorMap);
                solverConfig.setGizmoSolutionClonerMap(solutionClonerMap);

                StringWriter effectiveSolverConfigWriter = new StringWriter();
                SolverConfigIO solverConfigIO = new SolverConfigIO();
                solverConfigIO.write(solverConfig, effectiveSolverConfigWriter);

                uiSolverConfig.setSolverConfigFile(solverName, effectiveSolverConfigWriter.toString());
                uiSolverConfig.setFactory(solverName, SolverFactory.create(solverConfig));
            });
            return uiSolverConfig;
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
}
