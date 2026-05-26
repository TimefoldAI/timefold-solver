package ai.timefold.solver.model.worker.impl.log;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import ai.timefold.solver.core.api.score.stream.ConstraintJustification;
import ai.timefold.solver.model.definition.api.ModelOutput;
import ai.timefold.solver.model.definition.api.ModelPostProcessor;
import ai.timefold.solver.model.definition.api.SolverModel;
import ai.timefold.solver.model.definition.api.log.LogInfo;
import ai.timefold.solver.model.definition.impl.log.LoggingConstants;
import ai.timefold.solver.model.definition.internal.platform.EnvironmentVars;
import ai.timefold.solver.model.definition.internal.storage.AbstractStorageService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quarkus.runtime.ShutdownEvent;

@ApplicationScoped
public class LogsPostProcessor implements ModelPostProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogsPostProcessor.class);
    private final AbstractStorageService<?, ?, ?, ?, ?, ?, ? extends ConstraintJustification> storageService;
    private final AtomicBoolean logsStored = new AtomicBoolean(false);

    @Inject
    public LogsPostProcessor(
            AbstractStorageService<?, ?, ?, ?, ?, ?, ? extends ConstraintJustification> storageService) {
        this.storageService = storageService;
    }

    @Override
    public void processComputed(ModelOutput modelOutput, SolverModel<?> solverModel, String id) {
        // no op
    }

    @Override
    public void process(ModelOutput modelOutput, SolverModel<?> solverModel, String id) {
        if (logsStored.get()) {
            return;
        }
        logsStored.set(true);

        storeLogsWithStacktrace(id, null);
    }

    @Override
    public void processFailed(String id, Throwable error) {
        if (logsStored.get()) {
            return;
        }
        logsStored.set(true);

        StringWriter stackTraceCollector = new StringWriter();
        PrintWriter writer = new PrintWriter(stackTraceCollector);
        error.printStackTrace(writer);

        storeLogsWithStacktrace(id, stackTraceCollector.toString());
    }

    public void onShutdown(@Observes ShutdownEvent event) {
        if (logsStored.get()) {
            return;
        }
        logsStored.set(true);

        String id = System.getenv(EnvironmentVars.ENV_TIMEFOLD_JOB_ID);
        if (id != null) {
            try {
                storeLogsWithStacktrace(id, null);
            } catch (Throwable e) {
                LOGGER.warn("Unable to store logs on shutdown for run with id {} due to {}", id, e.getMessage());
            }
        }
    }

    private void storeLogsWithStacktrace(String id, String stacktrace) {
        Path solverLogPath = Paths.get(LoggingConstants.SOLVER_LOG_PATH);
        if (Files.exists(solverLogPath)) {
            try {
                // collect logs of this execution
                LogInfo logsObject = new LogInfo(
                        Files.readAllLines(solverLogPath).stream().collect(Collectors.joining(System.lineSeparator())));

                // append stacktrace if there is any
                if (stacktrace != null) {
                    logsObject.appendPreviousLog(stacktrace);
                }

                // avoid overriding the logs that where previously stored as this could be restarted run
                LogInfo alreadyStored = storageService.getLogs(id);
                if (alreadyStored != null) {
                    logsObject.appendPreviousLog(alreadyStored.getDetails());
                }

                storageService.storeLogs(id, logsObject);
            } catch (IOException e) {
                LOGGER.warn("Unable to store logs for run with id {} due to {}", id, e.getMessage());
            }
        }

    }
}
