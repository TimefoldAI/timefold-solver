package ai.timefold.solver.quarkus.devui;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.domain.solution.cloner.SolutionCloner;
import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.domain.common.accessor.MemberAccessor;
import ai.timefold.solver.core.impl.io.jaxb.SolverConfigIO;
import ai.timefold.solver.quarkus.TimefoldRecorder;
import ai.timefold.solver.quarkus.config.TimefoldRuntimeConfig;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class TimefoldDevUIRecorder {
    final TimefoldRuntimeConfig timefoldRuntimeConfig;

    public TimefoldDevUIRecorder(final TimefoldRuntimeConfig timefoldRuntimeConfig) {
        this.timefoldRuntimeConfig = timefoldRuntimeConfig;
    }

    public Supplier<DevUISolverConfig> solverConfigSupplier(Map<String, SolverConfig> allSolverConfig,
            Map<String, RuntimeValue<MemberAccessor>> generatedGizmoMemberAccessorMap,
            Map<String, RuntimeValue<SolutionCloner>> generatedGizmoSolutionClonerMap) {
        return () -> {
            DevUISolverConfig uiSolverConfig = new DevUISolverConfig();
            allSolverConfig.forEach((solverName, solverConfig) -> {
                updateSolverConfigWithRuntimeProperties(solverName, solverConfig);
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

    private void updateSolverConfigWithRuntimeProperties(String solverName, SolverConfig solverConfig) {
        TimefoldRecorder.updateSolverConfigWithRuntimeProperties(solverConfig,
                timefoldRuntimeConfig.getSolverRuntimeConfig(solverName).orElse(null));
    }
}
