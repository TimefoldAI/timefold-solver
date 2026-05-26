package ai.timefold.solver.model.worker.impl.audit;

import jakarta.enterprise.context.ApplicationScoped;

import ai.timefold.solver.model.definition.api.SolvingStatus;
import ai.timefold.solver.model.definition.internal.events.BestSolutionEvent;
import ai.timefold.solver.model.definition.internal.events.DatasetComputedEvent;
import ai.timefold.solver.model.definition.internal.events.DatasetCreatedEvent;
import ai.timefold.solver.model.definition.internal.events.DatasetValidatedEvent;
import ai.timefold.solver.model.definition.internal.events.FailedSolutionEvent;
import ai.timefold.solver.model.definition.internal.events.FinalBestSolutionEvent;
import ai.timefold.solver.model.definition.internal.events.InitSolutionEvent;
import ai.timefold.solver.model.definition.internal.events.ItemCompleted;
import ai.timefold.solver.model.definition.internal.events.ItemFailed;
import ai.timefold.solver.model.definition.internal.events.ItemStarted;
import ai.timefold.solver.model.definition.internal.events.ItemTerminated;
import ai.timefold.solver.model.definition.internal.events.SolverChannels;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class AuditListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditListener.class);

    @Incoming(SolverChannels.DATASET_CREATED)
    public void logDatasetPosted(DatasetCreatedEvent datasetPostedEvent) {
        LOGGER.info("Dataset with id ({}) posted", datasetPostedEvent.getId());
    }

    @Incoming(SolverChannels.DATASET_VALIDATED)
    public void logDatasetPosted(DatasetValidatedEvent datasetValidatedEvent) {
        LOGGER.info("Dataset with id ({}) validated", datasetValidatedEvent.getId());
    }

    @Incoming(SolverChannels.DATASET_COMPUTED)
    public void logDatasetOutputComputed(DatasetComputedEvent datasetComputedEvent) {
        LOGGER.info("Outputs for a posted dataset with id ({}) computed", datasetComputedEvent.getId());
    }

    @Incoming(SolverChannels.STARTED)
    public void logStarted(ItemStarted scheduleEvent) {
        if (scheduleEvent.getMetadata().getSolverStatus() == SolvingStatus.SOLVING_ACTIVE) {
            LOGGER.info("Solving with id ({}) active", scheduleEvent.getId());
        } else {
            LOGGER.info("Starting solving schedule ({})", scheduleEvent.getId());
        }
    }

    @Incoming(SolverChannels.COMPLETED)
    public void logCompleted(ItemCompleted scheduleEvent) {
        LOGGER.info("Completed solving schedule ({})", scheduleEvent.getId());
    }

    @Incoming(SolverChannels.TERMINATED)
    public void logTerminated(ItemTerminated scheduleEvent) {
        LOGGER.info("Terminated solving schedule ({})", scheduleEvent.getId());
    }

    @Incoming(SolverChannels.FAILED)
    public void logFailed(ItemFailed scheduleEvent) {
        LOGGER.error("Solving with id ({}) failed: message ({})", scheduleEvent.getId(),
                scheduleEvent.getCause().getMessage(), scheduleEvent.getCause());
    }

    @Incoming(SolverChannels.INIT_SOLUTION)
    public void logInitSolution(InitSolutionEvent event) {
        LOGGER.debug("Init solution found for solving schedule ({})", event.getId());
    }

    @Incoming(SolverChannels.BEST_SOLUTION)
    public void logBestSolution(BestSolutionEvent event) {
        LOGGER.debug("New best solution found for solving schedule ({})", event.getId());
    }

    @Incoming(SolverChannels.FINAL_BEST_SOLUTION)
    public void logFinalBestSolution(FinalBestSolutionEvent event) {
        LOGGER.debug("Final best solution found for solving schedule ({})", event.getId());
    }

    @Incoming(SolverChannels.FAILED_SOLUTION)
    public void logFailedSolution(FailedSolutionEvent event) {
        LOGGER.debug("Failed solution for solving schedule ({}): message ({})", event.getId(), event.getCause().getMessage());
    }
}
