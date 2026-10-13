package ai.timefold.solver.service.worker.impl.testutil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import ai.timefold.solver.core.api.solver.SolverManager;
import ai.timefold.solver.core.config.solver.termination.TerminationConfig;
import ai.timefold.solver.service.definition.api.ModelPostProcessor;
import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.definition.impl.storage.StorageObjectMapperWrapper;
import ai.timefold.solver.service.definition.internal.events.FailedSolutionEvent;
import ai.timefold.solver.service.definition.internal.events.ItemFailed;
import ai.timefold.solver.service.definition.internal.events.ItemTerminated;
import ai.timefold.solver.service.worker.impl.CompletionStatus;
import ai.timefold.solver.service.worker.impl.ShutdownOnTerminate;
import ai.timefold.solver.service.worker.impl.SolverWorker;
import ai.timefold.solver.service.worker.impl.termination.TerminationService;
import ai.timefold.solver.service.worker.impl.testdata.TestdataStorage;
import ai.timefold.solver.service.worker.impl.testdata.TestdataStorageService;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Builds a {@link SolverWorker} without a CDI container.
 * The storage is in memory, the events land in recording emitters,
 * and the solver manager and the termination service are Mockito mocks that a test stubs.
 */
public final class SolverWorkerBuilder {

    private final SolverManager<SolverModel> solverManager = mock(SolverManager.class);
    private final TerminationService terminationService = mock(TerminationService.class);
    private final CompletionStatus completionStatus = new CompletionStatus();
    private final RecordingEmitter<ItemTerminated> terminatedEmitter = new RecordingEmitter<>();
    private final RecordingEmitter<ItemFailed> failedEmitter = new RecordingEmitter<>();
    private final RecordingEmitter<FailedSolutionEvent> failedSolutionEmitter = new RecordingEmitter<>();
    private TestdataStorageService storageService = new TestdataStorageService(new TestdataStorage(), storageMapper());
    private List<ModelPostProcessor> modelPostProcessors = List.of();

    /**
     * The storage mapper with the Java time module, so the metadata timestamps round-trip.
     */
    public static StorageObjectMapperWrapper storageMapper() {
        return new StorageObjectMapperWrapper(new ObjectMapper().findAndRegisterModules());
    }

    public SolverWorkerBuilder withStorageService(TestdataStorageService storageService) {
        this.storageService = storageService;
        return this;
    }

    public SolverWorkerBuilder withModelPostProcessors(ModelPostProcessor... modelPostProcessors) {
        this.modelPostProcessors = List.of(modelPostProcessors);
        return this;
    }

    public SolverManager<SolverModel> getSolverManager() {
        return solverManager;
    }

    public CompletionStatus getCompletionStatus() {
        return completionStatus;
    }

    public TestdataStorageService getStorageService() {
        return storageService;
    }

    public RecordingEmitter<ItemTerminated> getTerminatedEmitter() {
        return terminatedEmitter;
    }

    public RecordingEmitter<ItemFailed> getFailedEmitter() {
        return failedEmitter;
    }

    public RecordingEmitter<FailedSolutionEvent> getFailedSolutionEmitter() {
        return failedSolutionEmitter;
    }

    public SolverWorker build() {
        when(terminationService.resolveTerminationConfig(any())).thenReturn(new TerminationConfig());
        return new SolverWorker(Optional.empty(), Optional.empty(), Optional.empty(), storageService, null,
                modelPostProcessors, solverManager, null, null, null, null, null, terminationService, null,
                new ShutdownOnTerminate(null), new UnresolvableInstance<>(), completionStatus, new RecordingEmitter<>(),
                new RecordingEmitter<>(), new RecordingEmitter<>(), terminatedEmitter, new RecordingEmitter<>(), failedEmitter,
                new RecordingEmitter<>(), new RecordingEmitter<>(), new RecordingEmitter<>(), failedSolutionEmitter);
    }
}
