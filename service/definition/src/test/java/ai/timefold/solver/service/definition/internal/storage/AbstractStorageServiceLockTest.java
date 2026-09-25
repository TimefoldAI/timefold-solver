package ai.timefold.solver.service.definition.internal.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import ai.timefold.solver.service.definition.api.ModelConfigOverrides;
import ai.timefold.solver.service.definition.api.ModelConstraintJustification;
import ai.timefold.solver.service.definition.api.ModelInput;
import ai.timefold.solver.service.definition.api.ModelOutput;
import ai.timefold.solver.service.definition.api.domain.Configuration;
import ai.timefold.solver.service.definition.api.metrics.ModelInputMetrics;
import ai.timefold.solver.service.definition.api.metrics.ModelOutputMetrics;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * The per data set lock is taken by the operations of the storage service, and the composite operations take it
 * around several of them, so it has to be reentrant without ever letting a second thread in.
 */
class AbstractStorageServiceLockTest {

    private final TestStorageService service = new TestStorageService();

    @Test
    void aNestedReleaseDoesNotHandTheLockToAnotherThread() throws Exception {
        service.acquireLock("a");
        service.acquireLock("a"); // as a composite operation does when it calls one of the locked operations
        service.releaseLock("a"); // the inner operation is done, the composite is not

        assertThat(acquiredWithin("a", 300)).as("the lock is still held by the outer operation").isFalse();

        service.releaseLock("a");

        assertThat(acquiredWithin("a", 5_000)).as("the lock is free once the outer operation released it").isTrue();
    }

    @Test
    void locksOfDifferentDataSetsDoNotBlockEachOther() throws Exception {
        service.acquireLock("a");
        try {
            assertThat(acquiredWithin("b", 5_000)).isTrue();
        } finally {
            service.releaseLock("a");
        }
    }

    @Test
    void theLockIsForgottenOnceNobodyUsesIt() {
        service.acquireLock("a");
        service.acquireLock("a");
        service.releaseLock("a");

        assertThat(locks()).as("still in use by the outer operation").containsKey("a");

        service.releaseLock("a");

        assertThat(locks()).as("nothing holds it any more").isEmpty();
    }

    /**
     * Takes the lock of given data set on another thread, and releases it again, reporting whether that succeeded
     * within given time.
     */
    private boolean acquiredWithin(String id, long millis) throws InterruptedException {
        CountDownLatch acquired = new CountDownLatch(1);
        Thread thread = new Thread(() -> {
            service.acquireLock(id);
            try {
                acquired.countDown();
            } finally {
                service.releaseLock(id);
            }
        });
        thread.setDaemon(true);
        thread.start();
        boolean result = acquired.await(millis, TimeUnit.MILLISECONDS);
        if (result) {
            thread.join(5_000);
        } else {
            thread.interrupt();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, ?> locks() {
        try {
            Field field = AbstractStorageService.class.getDeclaredField("locks");
            field.setAccessible(true);
            return (Map<String, ?>) field.get(service);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final class TestStorageService
            extends
            AbstractStorageService<ModelInput, ModelConfigOverrides, ModelInputMetrics, ModelOutputMetrics, ModelOutput, Object, ModelConstraintJustification> {

        @Override
        protected Class<?> getModelInputClass() {
            return ModelInput.class;
        }

        @Override
        protected Class<?> getModelOutputClass() {
            return ModelOutput.class;
        }

        @Override
        protected Class<?> getInputMetricsClass() {
            return ModelInputMetrics.class;
        }

        @Override
        protected Class<?> getOutputMetricsClass() {
            return ModelOutputMetrics.class;
        }

        @Override
        protected TypeReference<Configuration<ModelConfigOverrides>> getConfigurationClass() {
            return new TypeReference<>() {
            };
        }
    }
}
