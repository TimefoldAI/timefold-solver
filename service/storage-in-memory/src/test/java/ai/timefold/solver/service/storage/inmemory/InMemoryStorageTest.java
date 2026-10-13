package ai.timefold.solver.service.storage.inmemory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ai.timefold.solver.service.definition.internal.error.ItemNotFoundException;
import ai.timefold.solver.service.definition.internal.storage.StorageContent;
import ai.timefold.solver.service.definition.internal.storage.StorageItem;
import ai.timefold.solver.service.definition.internal.storage.SubModelKind;

import org.junit.jupiter.api.Test;

class InMemoryStorageTest {

    private static final String ID = "dataset-1";

    private final InMemoryStorage storage = new InMemoryStorage();

    // dataset

    @Test
    void storedDatasetIsReadBack() throws IOException {
        storage.store(null, ID, content("output"));

        assertThat(read(storage.get(null, ID))).isEqualTo("output");
        assertThat(storage.exists(null, ID)).isTrue();
    }

    /**
     * The content is kept as it is handed over, compressed or not, the storage does not interpret it.
     */
    @Test
    void contentIsKeptByteForByte() throws IOException {
        byte[] gzipLike = { (byte) 0x1f, (byte) 0x8b, 0, (byte) 0xff, 42 };
        storage.store(null, ID, StorageContent.of(gzipLike));

        try (InputStream stored = storage.get(null, ID)) {
            assertThat(stored.readAllBytes()).containsExactly(gzipLike);
        }
    }

    @Test
    void updateAndCompleteReplaceTheDataset() throws IOException {
        storage.store(null, ID, content("first"));
        storage.update(null, ID, content("second"));
        assertThat(read(storage.get(null, ID))).isEqualTo("second");

        storage.complete(null, ID, content("final"));
        assertThat(read(storage.get(null, ID))).isEqualTo("final");
    }

    /**
     * Content backed by a stream somebody else opened can be read only once, which is all the storage needs.
     */
    @Test
    void contentThatCanBeReadOnlyOnceIsStored() throws IOException {
        byte[] bytes = "streamed".getBytes(StandardCharsets.UTF_8);
        storage.store(null, ID, new StorageContent(new ByteArrayInputStream(bytes), bytes.length, Map.of()));

        assertThat(read(storage.get(null, ID))).isEqualTo("streamed");
    }

    @Test
    void everyReadGetsItsOwnStream() throws IOException {
        storage.store(null, ID, content("output"));

        assertThat(read(storage.get(null, ID))).isEqualTo("output");
        assertThat(read(storage.get(null, ID))).isEqualTo("output");
    }

    @Test
    void readingAMissingDatasetFails() {
        assertThatThrownBy(() -> storage.get(null, ID))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining(ID);
        assertThat(storage.exists(null, ID)).isFalse();
    }

    // sub models

    @Test
    void storedSubModelIsReadBack() throws IOException {
        storage.storeSubModel(null, ID, SubModelKind.MODEL_INPUT, content("input"));

        assertThat(read(storage.getSubModel(null, ID, SubModelKind.MODEL_INPUT))).isEqualTo("input");
        assertThat(storage.existsSubModel(null, ID, SubModelKind.MODEL_INPUT)).isTrue();
    }

    @Test
    void missingSubModelIsNull() {
        assertThat(storage.getSubModel(null, ID, SubModelKind.MODEL_INPUT)).isNull();
        assertThat(storage.existsSubModel(null, ID, SubModelKind.MODEL_INPUT)).isFalse();
    }

    @Test
    void subModelsAreKeptPerKindAndPerDataset() throws IOException {
        storage.storeSubModel(null, ID, SubModelKind.MODEL_INPUT, content("input"));
        storage.storeSubModel(null, ID, SubModelKind.CONFIG, content("config"));
        storage.storeSubModel(null, "dataset-2", SubModelKind.MODEL_INPUT, content("other input"));

        assertThat(read(storage.getSubModel(null, ID, SubModelKind.MODEL_INPUT))).isEqualTo("input");
        assertThat(read(storage.getSubModel(null, ID, SubModelKind.CONFIG))).isEqualTo("config");
        assertThat(read(storage.getSubModel(null, "dataset-2", SubModelKind.MODEL_INPUT))).isEqualTo("other input");
        assertThat(storage.existsSubModel(null, "dataset-2", SubModelKind.CONFIG)).isFalse();
    }

    @Test
    void updateSubModelReplacesContentAndAttributes() throws IOException {
        storage.storeSubModel(null, ID, SubModelKind.METADATA, content("v1", Map.of("solverStatus", "SOLVING_ACTIVE")));
        storage.updateSubModel(null, ID, SubModelKind.METADATA,
                content("v2", Map.of("solverStatus", "SOLVING_COMPLETED")));

        assertThat(read(storage.getSubModel(null, ID, SubModelKind.METADATA))).isEqualTo("v2");
        assertThat(storage.list(null, 0, 10))
                .containsExactly(new StorageItem(ID, Map.of("solverStatus", "SOLVING_COMPLETED")));
    }

    // delete and restore

    @Test
    void deleteRemovesTheDatasetAndAllItsSubModels() {
        storeRun(ID);

        storage.delete(null, ID);

        assertThat(storage.exists(null, ID)).isFalse();
        assertThatThrownBy(() -> storage.get(null, ID)).isInstanceOf(ItemNotFoundException.class);
        for (SubModelKind kind : List.of(SubModelKind.METADATA, SubModelKind.MODEL_INPUT)) {
            assertThat(storage.existsSubModel(null, ID, kind)).isFalse();
            assertThat(storage.getSubModel(null, ID, kind)).isNull();
        }
        assertThat(storage.list(null, 0, 10)).isEmpty();
    }

    @Test
    void deleteLeavesOtherDatasetsAlone() throws IOException {
        storeRun(ID);
        storeRun("dataset-2");

        storage.delete(null, ID);

        assertThat(read(storage.get(null, "dataset-2"))).isEqualTo("output of dataset-2");
        assertThat(storage.list(null, 0, 10)).extracting(StorageItem::id).containsExactly("dataset-2");
    }

    /**
     * The storage service retries a delete whose response got lost, so deleting what is gone already is not an error.
     */
    @Test
    void deleteCanBeRepeated() {
        storeRun(ID);

        storage.delete(null, ID);
        storage.delete(null, ID);
        storage.delete(null, "never-stored");

        assertThat(storage.exists(null, ID)).isFalse();
    }

    @Test
    void restoreBringsBackTheDatasetSubModelsAndAttributes() throws IOException {
        storeRun(ID);
        storage.delete(null, ID);

        storage.restore(null, ID);

        assertThat(read(storage.get(null, ID))).isEqualTo("output of " + ID);
        assertThat(read(storage.getSubModel(null, ID, SubModelKind.MODEL_INPUT))).isEqualTo("input of " + ID);
        assertThat(read(storage.getSubModel(null, ID, SubModelKind.METADATA))).isEqualTo("metadata of " + ID);
        assertThat(storage.list(null, 0, 10))
                .containsExactly(new StorageItem(ID, Map.of("solverStatus", "SOLVING_COMPLETED")));
    }

    /**
     * A run deleted before its model output was stored only has sub models, which are still restored.
     */
    @Test
    void runDeletedBeforeItsModelOutputIsRestored() throws IOException {
        storage.storeSubModel(null, ID, SubModelKind.METADATA,
                content("metadata", Map.of("solverStatus", "DATASET_CREATED")));
        storage.storeSubModel(null, ID, SubModelKind.MODEL_INPUT, content("input"));
        storage.delete(null, ID);

        storage.restore(null, ID);

        assertThat(storage.exists(null, ID)).isFalse();
        assertThat(read(storage.getSubModel(null, ID, SubModelKind.MODEL_INPUT))).isEqualTo("input");
        assertThat(storage.list(null, 0, 10)).extracting(StorageItem::id).containsExactly(ID);
    }

    @Test
    void restoringWhatWasNeverDeletedFails() {
        storeRun(ID);

        assertThatThrownBy(() -> storage.restore(null, ID))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining(ID);
        assertThatThrownBy(() -> storage.restore(null, "never-stored"))
                .isInstanceOf(ItemNotFoundException.class);
    }

    /**
     * Restoring is not idempotent, which is why the storage service does not retry it.
     */
    @Test
    void restoringTwiceFails() {
        storeRun(ID);
        storage.delete(null, ID);
        storage.restore(null, ID);

        assertThatThrownBy(() -> storage.restore(null, ID)).isInstanceOf(ItemNotFoundException.class);
        assertThat(storage.exists(null, ID)).isTrue();
    }

    /**
     * Deleting a run again keeps only the latest deleted copy, which is what a restore brings back.
     */
    @Test
    void deleteRestoreCycleCanBeRepeated() throws IOException {
        storeRun(ID);
        storage.delete(null, ID);
        storage.restore(null, ID);
        storage.update(null, ID, content("changed output"));

        storage.delete(null, ID);
        storage.restore(null, ID);

        assertThat(read(storage.get(null, ID))).isEqualTo("changed output");
    }

    // listing

    @Test
    void listReturnsTheRunsWithMetadataAndTheirAttributes() {
        storage.storeSubModel(null, ID, SubModelKind.METADATA,
                content("metadata", Map.of("solverStatus", "SOLVING_ACTIVE")));
        storage.storeSubModel(null, ID, SubModelKind.MODEL_INPUT, content("input"));
        storage.storeSubModel(null, "input-only", SubModelKind.MODEL_INPUT, content("input"));
        storage.store(null, "output-only", content("output"));

        assertThat(storage.list(null, 0, 10))
                .containsExactly(new StorageItem(ID, Map.of("solverStatus", "SOLVING_ACTIVE")));
    }

    @Test
    void listReturnsNoAttributesWhenTheMetadataCameWithout() {
        storage.storeSubModel(null, ID, SubModelKind.METADATA, content("metadata"));

        assertThat(storage.list(null, 0, 10)).containsExactly(new StorageItem(ID, Map.of()));
    }

    /**
     * Only the suffix of the metadata sub model separates the id, so ids may contain the separator themselves.
     */
    @Test
    void listKeepsIdsThatContainTheSeparator() {
        storage.storeSubModel(null, "my_run_1", SubModelKind.METADATA, content("metadata"));

        assertThat(storage.list(null, 0, 10)).extracting(StorageItem::id).containsExactly("my_run_1");
    }

    @Test
    void listIsPaged() {
        for (int i = 0; i < 5; i++) {
            storage.storeSubModel(null, "run-" + i, SubModelKind.METADATA, content("metadata"));
        }

        List<String> seen = new ArrayList<>();
        seen.addAll(ids(storage.list(null, 0, 2)));
        seen.addAll(ids(storage.list(null, 1, 2)));
        assertThat(seen).hasSize(4);
        List<StorageItem> lastPage = storage.list(null, 2, 2);
        assertThat(lastPage).hasSize(1);
        seen.addAll(ids(lastPage));

        assertThat(seen).containsExactlyInAnyOrder("run-0", "run-1", "run-2", "run-3", "run-4");
        assertThat(storage.list(null, 3, 2)).isEmpty();
        assertThat(storage.list(null, 0, 0)).isEmpty();
    }

    // clean and life cycle

    @Test
    void cleanRemovesEverythingIncludingWhatWasDeleted() {
        storeRun(ID);
        storeRun("dataset-2");
        storage.delete(null, "dataset-2");

        storage.clean(null);

        assertThat(storage.exists(null, ID)).isFalse();
        assertThat(storage.existsSubModel(null, ID, SubModelKind.MODEL_INPUT)).isFalse();
        assertThat(storage.list(null, 0, 10)).isEmpty();
        assertThatThrownBy(() -> storage.restore(null, "dataset-2")).isInstanceOf(ItemNotFoundException.class);
    }

    /**
     * There is nothing to set up for memory, so the life cycle operations leave the content alone.
     */
    @Test
    void lifeCycleOperationsLeaveTheContentAlone() throws IOException {
        storeRun(ID);

        storage.create("location", null);
        storage.reconfigure("location", null);
        storage.destroy("location");

        assertThat(read(storage.get(null, ID))).isEqualTo("output of " + ID);
    }

    private void storeRun(String id) {
        storage.storeSubModel(null, id, SubModelKind.METADATA,
                content("metadata of " + id, Map.of("solverStatus", "SOLVING_COMPLETED")));
        storage.storeSubModel(null, id, SubModelKind.MODEL_INPUT, content("input of " + id));
        storage.store(null, id, content("output of " + id));
    }

    private static StorageContent content(String value) {
        return StorageContent.of(value.getBytes(StandardCharsets.UTF_8));
    }

    private static StorageContent content(String value, Map<String, String> attributes) {
        return StorageContent.of(value.getBytes(StandardCharsets.UTF_8), attributes);
    }

    private static String read(InputStream stream) throws IOException {
        try (stream) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static List<String> ids(List<StorageItem> items) {
        return items.stream().map(StorageItem::id).toList();
    }
}
