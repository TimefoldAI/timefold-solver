package ai.timefold.solver.quarkus.devui;

import java.io.StringWriter;
import java.util.Map;
import java.util.function.Supplier;

import ai.timefold.solver.core.api.solver.SolverFactory;
import ai.timefold.solver.core.config.solver.SolverConfig;
import ai.timefold.solver.core.impl.domain.common.DomainAccessType;
import ai.timefold.solver.core.impl.io.jaxb.SolverConfigIO;
import ai.timefold.solver.quarkus.TimefoldRecorder;
import ai.timefold.solver.quarkus.config.TimefoldRuntimeConfig;

import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class TimefoldDevUIRecorder {
    final RuntimeValue<TimefoldRuntimeConfig> timefoldRuntimeConfig;

    public TimefoldDevUIRecorder(final RuntimeValue<TimefoldRuntimeConfig> timefoldRuntimeConfig) {
        this.timefoldRuntimeConfig = timefoldRuntimeConfig;
    }

    public Supplier<DevUISolverConfig> solverConfigSupplier(Map<String, SolverConfig> allSolverConfig) {
        return () -> {
            DevUISolverConfig uiSolverConfig = new DevUISolverConfig();
            allSolverConfig.forEach((solverName, solverConfig) -> {
                updateSolverConfigWithRuntimeProperties(solverName, solverConfig);
                solverConfig.setDomainAccessType(DomainAccessType.FORCE_REFLECTION);

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
                timefoldRuntimeConfig.getValue().getSolverRuntimeConfig(solverName).orElse(null));
    }
}
