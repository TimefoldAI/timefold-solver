package ai.timefold.solver.service.worker.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import ai.timefold.solver.core.api.score.HardSoftScore;
import ai.timefold.solver.service.definition.api.ModelOutput;
import ai.timefold.solver.service.definition.api.ModelPostProcessor;
import ai.timefold.solver.service.definition.api.SolverModel;
import ai.timefold.solver.service.definition.api.domain.Metadata;
import ai.timefold.solver.service.definition.internal.events.ItemFailed;
import ai.timefold.solver.service.worker.impl.testdata.TestdataStorage;
import ai.timefold.solver.service.worker.impl.testdata.TestdataStorageService;
import ai.timefold.solver.service.worker.impl.testutil.RecordingEmitter;
import ai.timefold.solver.service.worker.impl.testutil.UnresolvableInstance;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class SolverWorkerTest {

    private static final String RUN_ID = "missing-run";

    private final RecordingEmitter<ItemFailed> failedEmitter = new RecordingEmitter<>();

    private final FailureRecordingPostProcessor postProcessor = new FailureRecordingPostProcessor();

    @Test
    void sendsFailedWithTheRunIdWhenTheRunHasNoMetadata() {
        var solverWorker = newSolverWorker(new TestdataStorageService(new TestdataStorage(new ObjectMapper())));

        solverWorker.notifyOnFailure(RUN_ID, new IllegalStateException("Solver failed."));

        Assertions.assertThat(failedEmitter.getMessages()).extracting(ItemFailed::getId).containsExactly(RUN_ID);
        Assertions.assertThat(postProcessor.failedRunIdList).containsExactly(RUN_ID);
    }

    @Test
    void sendsFailedWithTheRunIdWhenTheMetadataCannotBeRead() {
        var storageService = new TestdataStorageService(new TestdataStorage(new ObjectMapper())) {
            @Override
            public Metadata<HardSoftScore> getMetadata(String id) {
                throw new IllegalStateException("Storage is unavailable.");
            }
        };
        var solverWorker = newSolverWorker(storageService);

        Assertions.assertThatThrownBy(() -> solverWorker.notifyOnFailure(RUN_ID, new IllegalStateException("Solver failed.")))
                .hasMessage("Storage is unavailable.");
        Assertions.assertThat(failedEmitter.getMessages()).extracting(ItemFailed::getId).containsExactly(RUN_ID);
        Assertions.assertThat(postProcessor.failedRunIdList).containsExactly(RUN_ID);
    }

    private SolverWorker newSolverWorker(TestdataStorageService storageService) {
        return new SolverWorker(Optional.empty(), Optional.empty(), Optional.empty(), storageService, null,
                List.of(postProcessor), null, null, null, null, null, null, null, null, new ShutdownOnTerminate(null),
                new UnresolvableInstance<>(), new CompletionStatus(), null, null, null, null, null, failedEmitter, null,
                null, null, new RecordingEmitter<>());
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
