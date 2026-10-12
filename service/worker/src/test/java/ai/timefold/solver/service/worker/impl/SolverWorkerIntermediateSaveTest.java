package ai.timefold.solver.service.worker.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;

import ai.timefold.solver.service.definition.internal.error.ErrorCodes;
import ai.timefold.solver.service.definition.internal.error.ItemNotFoundException;
import ai.timefold.solver.service.definition.internal.error.TimefoldRuntimeException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Intermediate solutions are superseded by later solutions.
 * Thus, transient storage failures can lose an intermediate solution without failing the run.
 */
class SolverWorkerIntermediateSaveTest {

    private static final String ID = "run-1";

    @Test
    void storedSolutionIsReportedAsStored() {
        var saves = new AtomicInteger();

        assertThat(SolverWorker.saveIntermediateSolution(ID, saves::incrementAndGet)).isTrue();
        assertThat(saves).hasValue(1);
    }

    @ParameterizedTest
    @ValueSource(strings = { ErrorCodes.STORAGE_UNABLE_TO_READ, ErrorCodes.STORAGE_UNABLE_TO_WRITE,
            ErrorCodes.STORAGE_UNKNOWN })
    void solutionIsSkippedWhenTheStorageFailsInAWayThatMayPass(String code) {
        assertThat(SolverWorker.saveIntermediateSolution(ID, () -> {
            throw new TimefoldRuntimeException(code, "storage unavailable", true);
        })).isFalse();
    }

    @Test
    void permanentStorageFailureStillFailsTheRun() {
        var failure = new TimefoldRuntimeException(ErrorCodes.STORAGE_UNABLE_TO_WRITE, "cannot be serialized", false);

        assertThatThrownBy(() -> SolverWorker.saveIntermediateSolution(ID, () -> {
            throw failure;
        })).isSameAs(failure);
    }

    @Test
    void dataSetThatIsGoneStillFailsTheRun() {
        var failure = new ItemNotFoundException(ErrorCodes.STORAGE_NO_JOB_FOUND, "deleted");

        assertThatThrownBy(() -> SolverWorker.saveIntermediateSolution(ID, () -> {
            throw failure;
        })).isSameAs(failure);
    }

    /**
     * Only storage failures are skipped; anything else that went wrong while handling the solution is not hidden.
     */
    @Test
    void failureOtherThanTheStorageStillFailsTheRun() {
        var recoverableButNotStorage = new TimefoldRuntimeException(ErrorCodes.INVALID_DATA, "invalid", true);
        assertThatThrownBy(() -> SolverWorker.saveIntermediateSolution(ID, () -> {
            throw recoverableButNotStorage;
        })).isSameAs(recoverableButNotStorage);

        var unexpected = new IllegalStateException("bug");
        assertThatThrownBy(() -> SolverWorker.saveIntermediateSolution(ID, () -> {
            throw unexpected;
        })).isSameAs(unexpected);
    }

    @Test
    void readReturnsWhatTheStorageReturnedOrNullWhenSkipped() {
        assertThat(SolverWorker.skipOnTransientStorageFailure(ID, () -> "metadata")).isEqualTo("metadata");
        assertThat(SolverWorker.<String> skipOnTransientStorageFailure(ID, () -> {
            throw new TimefoldRuntimeException(ErrorCodes.STORAGE_UNABLE_TO_READ, "timed out", true);
        })).isNull();
    }
}
