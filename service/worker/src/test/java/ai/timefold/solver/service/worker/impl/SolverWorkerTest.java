package ai.timefold.solver.service.worker.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.core.api.solver.SolverJobBuilder;
import ai.timefold.solver.core.api.solver.SolverStatus;
import ai.timefold.solver.service.definition.api.ModelOutput;
import ai.timefold.solver.service.definition.api.ModelPostProcessor;
import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.definition.api.SolvingStatus;
import ai.timefold.solver.service.definition.api.domain.Configuration;
import ai.timefold.solver.service.definition.api.domain.Metadata;
import ai.timefold.solver.service.definition.api.validation.LegacyValidationResult;
import ai.timefold.solver.service.definition.internal.events.ItemFailed;
import ai.timefold.solver.service.definition.internal.events.ItemTerminated;
import ai.timefold.solver.service.definition.internal.events.SolveStartCommand;
import ai.timefold.solver.service.definition.internal.events.SolveTerminateCommand;
import ai.timefold.solver.service.worker.impl.testdata.TestdataModelInput;
import ai.timefold.solver.service.worker.impl.testdata.TestdataStorage;
import ai.timefold.solver.service.worker.impl.testdata.TestdataStorageService;
import ai.timefold.solver.service.worker.impl.testutil.SolverWorkerBuilder;

import org.junit.jupiter.api.Test;

class SolverWorkerTest {

    private static final String RUN_ID = "run-1";

    private final FailureRecordingPostProcessor postProcessor = new FailureRecordingPostProcessor();

    private final SolverWorkerBuilder builder = new SolverWorkerBuilder().withModelPostProcessors(postProcessor);

    @Test
    void startsASolverJobForAStoredRun() {
        var solveBuilder = mock(SolverJobBuilder.class, RETURNS_SELF);
        when(builder.getSolverManager().solveBuilder()).thenReturn(solveBuilder);
        builder.getStorageService().storeProblem(RUN_ID, new TestdataModelInput("hello"), storedMetadata(),
                Configuration.empty(), Configuration.empty());
        var solverWorker = builder.build();
        var forwardedMetadataList = new ArrayList<Metadata<?>>();
        solverWorker.forwardLifeCycleEvent().subscribe().with(forwardedMetadataList::add);

        solverWorker.onSolveStartCommand(new SolveStartCommand(RUN_ID));

        verify(solveBuilder).withProblemId(RUN_ID);
        verify(solveBuilder).run();
        assertThat(forwardedMetadataList).extracting(Metadata::getId).containsExactly(RUN_ID);
        assertThat(builder.getFailedEmitter().getMessages()).isEmpty();
    }

    @Test
    void doesNotStartASolverJobWhenTheModelInputIsMissing() {
        builder.getStorageService().storeMetadata(RUN_ID, storedMetadata());
        var solverWorker = builder.build();

        solverWorker.onSolveStartCommand(new SolveStartCommand(RUN_ID));

        verify(builder.getSolverManager(), never()).solveBuilder();
        assertThat(builder.getFailedEmitter().getMessages()).isEmpty();
    }

    @Test
    void terminatesASolvingRunAndSendsTerminated() {
        var solverManager = builder.getSolverManager();
        when(solverManager.getSolverStatus(RUN_ID)).thenReturn(SolverStatus.SOLVING_ACTIVE);
        doAnswer(invocation -> {
            builder.getCompletionStatus().completed(RUN_ID);
            return null;
        }).when(solverManager).terminateEarly(RUN_ID);
        var solverWorker = builder.build();

        solverWorker.onSolveTerminateCommand(new SolveTerminateCommand(RUN_ID));

        verify(solverManager).terminateEarly(RUN_ID);
        assertThat(builder.getTerminatedEmitter().getMessages()).extracting(ItemTerminated::getId).containsExactly(RUN_ID);
    }

    @Test
    void ignoresATerminateCommandForARunThatIsNotSolving() {
        var solverManager = builder.getSolverManager();
        when(solverManager.getSolverStatus(RUN_ID)).thenReturn(SolverStatus.NOT_SOLVING);
        var solverWorker = builder.build();

        solverWorker.onSolveTerminateCommand(new SolveTerminateCommand(RUN_ID));

        verify(solverManager, never()).terminateEarly(any());
        assertThat(builder.getTerminatedEmitter().getMessages()).isEmpty();
    }

    @Test
    void marksTheRunFailedAndSendsFailedWithItsMetadata() {
        builder.getStorageService().storeMetadata(RUN_ID, storedMetadata());
        var solverWorker = builder.build();

        solverWorker.notifyOnFailure(RUN_ID, new IllegalStateException("Solver failed."));

        var storedMetadata = builder.getStorageService().getMetadata(RUN_ID);
        assertThat(storedMetadata.getSolverStatus()).isEqualTo(SolvingStatus.SOLVING_FAILED);
        assertThat(storedMetadata.getFailureMessage()).isEqualTo("Solver failed.");
        assertThat(builder.getFailedSolutionEmitter().getMessages()).hasSize(1);
        assertThat(builder.getFailedEmitter().getMessages()).singleElement()
                .satisfies(itemFailed -> assertThat(itemFailed.getMetadata().getId()).isEqualTo(RUN_ID));
        assertThat(postProcessor.failedRunIdList).containsExactly(RUN_ID);
    }

    @Test
    void sendsFailedWithTheRunIdWhenTheRunHasNoMetadata() {
        var solverWorker = builder.build();

        solverWorker.notifyOnFailure(RUN_ID, new IllegalStateException("Solver failed."));

        assertThat(builder.getFailedSolutionEmitter().getMessages()).isEmpty();
        assertThat(builder.getFailedEmitter().getMessages()).extracting(ItemFailed::getId).containsExactly(RUN_ID);
        assertThat(postProcessor.failedRunIdList).containsExactly(RUN_ID);
    }

    @Test
    void sendsFailedWithTheRunIdWhenTheMetadataCannotBeRead() {
        var storageService =
                new TestdataStorageService(new TestdataStorage(), SolverWorkerBuilder.storageMapper()) {
                    @Override
                    public Metadata<HardSoftScore> getMetadata(String id) {
                        throw new IllegalStateException("Storage is unavailable.");
                    }
                };
        var solverWorker = builder.withStorageService(storageService).build();

        assertThatThrownBy(() -> solverWorker.notifyOnFailure(RUN_ID, new IllegalStateException("Solver failed.")))
                .hasMessage("Storage is unavailable.");
        assertThat(builder.getFailedEmitter().getMessages()).extracting(ItemFailed::getId).containsExactly(RUN_ID);
        assertThat(postProcessor.failedRunIdList).containsExactly(RUN_ID);
    }

    private static Metadata<HardSoftScore> storedMetadata() {
        var metadata = new Metadata<HardSoftScore>("stored run");
        metadata.setId(RUN_ID);
        metadata.datasetCreated();
        metadata.datasetValidated(LegacyValidationResult.successful());
        metadata.datasetComputed();
        return metadata;
    }

    private static final class FailureRecordingPostProcessor implements ModelPostProcessor {

        private final List<String> failedRunIdList = new ArrayList<>();

        @Override
        public void process(ModelOutput modelOutput, SolverModel<?> solverModel, String id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void processFailed(String id, Throwable error) {
            failedRunIdList.add(id);
        }
    }
}
