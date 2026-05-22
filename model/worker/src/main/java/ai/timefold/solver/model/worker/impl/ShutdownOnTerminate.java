package ai.timefold.solver.model.worker.impl;

import java.time.Duration;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import ai.timefold.solver.model.definition.internal.error.TimefoldRuntimeException;
import ai.timefold.solver.model.definition.internal.platform.EnvironmentVars;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class ShutdownOnTerminate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownOnTerminate.class);

    private ShutdownExecutor shutdownExecutor;

    @Inject
    public ShutdownOnTerminate(ShutdownExecutor shutdownExecutor) {
        this.shutdownExecutor = shutdownExecutor;
    }

    public void process(String id) {

        if (System.getenv(EnvironmentVars.ENV_TIMEFOLD_JOB_ID) != null) {
            shutDownOrSchedule(id, 0);
        }
    }

    public void processFailed(String id, Throwable error) {
        if (System.getenv(EnvironmentVars.ENV_TIMEFOLD_JOB_ID) != null) {
            LOGGER.error("Solving with id ({}) failed: message ({}), shutting down", id,
                    error.getMessage(), error.getCause());
            shutDownOrSchedule(id, determineStatusBasedOnException(error));
        }
    }

    private void shutDownOrSchedule(String id, int status) {
        String delayDurationEnv = System.getenv(EnvironmentVars.ENV_TIMEFOLD_RUNTIME_TTL);
        if (delayDurationEnv == null) {

            LOGGER.info("Execution ended ({}), shutting down directly with status {}", id, status);
            // shutdown with fixed delay to let to finish responding to terminate early rest calls
            shutdownExecutor.scheduleShutdown(Duration.ofSeconds(2), status);
        } else {

            LOGGER.info("Execution ended ({}) with status {}, delaying shut down for {}", id, status, delayDurationEnv);

            Duration delayDuration = Duration.parse(delayDurationEnv);
            shutdownExecutor.scheduleShutdown(delayDuration, status);
        }
    }

    /**
     * Determines if the error received should be considered recoverable (restarting failed container) or not.
     * Only explicitly marked as non recoverable should shutdown with 0 return code which means successful shutdown
     * All other errors should shut down with non zero error code which means it should be restarted
     *
     * @param error error thrown during execution
     * @return return code to shut down container with
     */
    private int determineStatusBasedOnException(Throwable error) {
        if (error instanceof TimefoldRuntimeException) {
            return ((TimefoldRuntimeException) error).isRecoverable() ? 1 : 0;
        }

        return 1;
    }

}
