package ai.timefold.solver.model.quarkus.deployment;

import ai.timefold.solver.model.definition.impl.log.LoggingConstants;

import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;

public class TimefoldLoggingConfigProcessor {

    @BuildStep
    void confiugreSolverLogging(CombinedIndexBuildItem combinedIndex,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> runtimeConfigBuildProducer) {

        runtimeConfigBuildProducer
                .produce(new RunTimeConfigurationDefaultBuildItem("quarkus.log.handler.file.solver.enable", "true"));
        runtimeConfigBuildProducer
                .produce(new RunTimeConfigurationDefaultBuildItem("quarkus.log.handler.file.solver.path",
                        LoggingConstants.SOLVER_LOG_PATH));
        runtimeConfigBuildProducer.produce(new RunTimeConfigurationDefaultBuildItem("quarkus.log.handler.file.solver.format",
                "%d{HH:mm:ss.SSS} %5p %m%n"));
        runtimeConfigBuildProducer.produce(
                new RunTimeConfigurationDefaultBuildItem("quarkus.log.handler.file.solver.filter", "solver-log-filter"));
        runtimeConfigBuildProducer.produce(
                new RunTimeConfigurationDefaultBuildItem("quarkus.log.category.\"ai.timefold.solver\".handlers", "solver"));
        runtimeConfigBuildProducer.produce(
                new RunTimeConfigurationDefaultBuildItem("quarkus.log.category.\"ai.timefold.solver.model\".handlers",
                        "solver"));
    }
}
