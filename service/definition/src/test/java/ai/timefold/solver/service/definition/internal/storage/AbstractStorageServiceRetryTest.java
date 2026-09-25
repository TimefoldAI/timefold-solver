package ai.timefold.solver.service.definition.internal.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import ai.timefold.solver.service.definition.api.ModelConfigOverrides;
import ai.timefold.solver.service.definition.api.ModelConstraintJustification;
import ai.timefold.solver.service.definition.api.ModelInput;
import ai.timefold.solver.service.definition.api.ModelOutput;
import ai.timefold.solver.service.definition.api.domain.Configuration;
import ai.timefold.solver.service.definition.api.metrics.ModelInputMetrics;
import ai.timefold.solver.service.definition.api.metrics.ModelOutputMetrics;
import ai.timefold.solver.service.definition.impl.storage.CompressionUtils;
import ai.timefold.solver.service.definition.impl.storage.StorageObjectMapperWrapper;
import ai.timefold.solver.service.definition.internal.error.ErrorCodes;
import ai.timefold.solver.service.definition.internal.error.ItemNotFoundException;
import ai.timefold.solver.service.definition.internal.error.TimefoldRuntimeException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A data store, or the access service in front of it, can be briefly unavailable. The storage service still holds the
 * object at that point, so it can serialize it again and send it once more.
 */
class AbstractStorageServiceRetryTest {

    private FailingStorage storage;

    private TestStorageService service;

    @BeforeEach
    void setUp() {
        storage = new FailingStorage();
        service = new TestStorageService(storage, new StorageObjectMapperWrapper(new ObjectMapper()));
        service.setRetry(3, "PT0.01S", "PT0.02S");
    }

    @Test
    void aWriteThatFailedRecoverablyIsSentAgainInFull() {
        storage.failWrites(2, recoverable());

        service.storeModelOutput("run-1", new TestOutput("kept"));

        assertThat(storage.writeAttempts()).isEqualTo(3);
        assertThat(storage.storedBodies()).as("every attempt sends the whole content")
                .hasSize(3)
                .allSatisfy(body -> assertThat(body).isEqualTo(storage.storedBodies().getFirst()))
                .allSatisfy(body -> assertThat(body).isNotEmpty());
    }

    @Test
    void aReadThatFailedRecoverablyIsAttemptedAgain() {
        storage.content(compressed("{\"value\":\"kept\"}"));
        storage.failReads(1, recoverable());

        TestOutput output = service.getModelOutput("run-1");

        assertThat(output).isEqualTo(new TestOutput("kept"));
        assertThat(storage.readAttempts()).isEqualTo(2);
    }

    @Test
    void aFailureTheStorageDoesNotCallRecoverableIsNotAttemptedAgain() {
        storage.failWrites(1, new TimefoldRuntimeException(ErrorCodes.STORAGE_UNABLE_TO_WRITE, "rejected", false));

        assertThatThrownBy(() -> service.storeModelOutput("run-1", new TestOutput("kept")))
                .isInstanceOf(TimefoldRuntimeException.class)
                .hasMessage("rejected");

        assertThat(storage.writeAttempts()).isEqualTo(1);
    }

    @Test
    void aMissingDataSetIsNotAttemptedAgain() {
        storage.failReads(1, new ItemNotFoundException(ErrorCodes.STORAGE_NO_JOB_FOUND, "no such dataset"));

        assertThatThrownBy(() -> service.getModelOutput("run-1"))
                .isInstanceOf(ItemNotFoundException.class);

        assertThat(storage.readAttempts()).isEqualTo(1);
    }

    @Test
    void theLastFailureIsReportedOnceTheAttemptsAreUsedUp() {
        storage.failWrites(Integer.MAX_VALUE, recoverable());

        assertThatThrownBy(() -> service.storeModelOutput("run-1", new TestOutput("kept")))
                .isInstanceOf(TimefoldRuntimeException.class)
                .hasMessage("the data store is away");

        assertThat(storage.writeAttempts()).isEqualTo(3);
    }

    @Test
    void contentThatCannotBeReadAgainIsNotAttemptedAgain() {
        StorageContent forwarded = new StorageContent(new ByteArrayInputStream("body".getBytes(StandardCharsets.UTF_8)),
                4, Map.of());
        assertThat(forwarded.repeatable()).isFalse();
        AtomicInteger attempts = new AtomicInteger();

        assertThatThrownBy(() -> service.writeWithRetry("run-1", "forward the content", forwarded, () -> {
            attempts.incrementAndGet();
            throw recoverable();
        })).isInstanceOf(TimefoldRuntimeException.class);

        assertThat(attempts).hasValue(1);
    }

    private static TimefoldRuntimeException recoverable() {
        return new TimefoldRuntimeException(ErrorCodes.STORAGE_UNKNOWN, "the data store is away", true);
    }

    private static byte[] compressed(String json) {
        return CompressionUtils.compress(json.getBytes(StandardCharsets.UTF_8));
    }

    public record TestOutput(String value) implements ModelOutput {
    }

    private static final class TestStorageService
            extends
            AbstractStorageService<ModelInput, ModelConfigOverrides, ModelInputMetrics, ModelOutputMetrics, TestOutput, Object, ModelConstraintJustification> {

        private TestStorageService(Storage storage, StorageObjectMapperWrapper wrapper) {
            super(storage, wrapper);
        }

        @Override
        protected Class<?> getModelInputClass() {
            return ModelInput.class;
        }

        @Override
        protected Class<?> getModelOutputClass() {
            return TestOutput.class;
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

    /**
     * Storage that fails a given number of times before it starts working, recording what each attempt handed it.
     */
    private static final class FailingStorage implements Storage {

        private final List<byte[]> storedBodies = new ArrayList<>();
        private final AtomicInteger writeAttempts = new AtomicInteger();
        private final AtomicInteger readAttempts = new AtomicInteger();

        private int writeFailures;
        private int readFailures;
        private RuntimeException writeFailure;
        private RuntimeException readFailure;
        private byte[] content;

        void failWrites(int times, RuntimeException failure) {
            this.writeFailures = times;
            this.writeFailure = failure;
        }

        void failReads(int times, RuntimeException failure) {
            this.readFailures = times;
            this.readFailure = failure;
        }

        void content(byte[] content) {
            this.content = content;
        }

        int writeAttempts() {
            return writeAttempts.get();
        }

        int readAttempts() {
            return readAttempts.get();
        }

        List<byte[]> storedBodies() {
            return storedBodies;
        }

        @Override
        public void store(StorageAddress address, String id, StorageContent content) {
            writeAttempts.incrementAndGet();
            storedBodies.add(readAll(content));
            if (writeFailures-- > 0) {
                throw writeFailure;
            }
        }

        @Override
        public InputStream get(StorageAddress address, String id) {
            readAttempts.incrementAndGet();
            if (readFailures-- > 0) {
                throw readFailure;
            }
            return new ByteArrayInputStream(content);
        }

        private static byte[] readAll(StorageContent content) {
            try (InputStream stream = content.stream()) {
                return stream.readAllBytes();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void update(StorageAddress address, String id, StorageContent content) {
            store(address, id, content);
        }

        @Override
        public void complete(StorageAddress address, String id, StorageContent content) {
            store(address, id, content);
        }

        @Override
        public void delete(StorageAddress address, String id) {
        }

        @Override
        public void restore(StorageAddress address, String id) {
        }

        @Override
        public boolean exists(StorageAddress address, String id) {
            return content != null;
        }

        @Override
        public void storeSubModel(StorageAddress address, String id, SubModelKind kind, StorageContent content) {
            store(address, id, content);
        }

        @Override
        public void updateSubModel(StorageAddress address, String id, SubModelKind kind, StorageContent content) {
            store(address, id, content);
        }

        @Override
        public InputStream getSubModel(StorageAddress address, String id, SubModelKind kind) {
            return get(address, id);
        }

        @Override
        public boolean existsSubModel(StorageAddress address, String id, SubModelKind kind) {
            return content != null;
        }

        @Override
        public List<StorageItem> list(StorageAddress address, int pageNumber, int pageSize) {
            return List.of();
        }

        @Override
        public void clean(StorageAddress address) {
        }

        @Override
        public void create(String location, StorageConfiguration configuration) {
        }

        @Override
        public void reconfigure(String location, StorageConfiguration configuration) {
        }

        @Override
        public void destroy(String id) {
        }
    }
}
