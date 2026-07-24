package ai.timefold.solver.service.worker.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import ai.timefold.solver.service.definition.api.SolvingStatus;
import ai.timefold.solver.service.definition.api.domain.Configuration;
import ai.timefold.solver.service.definition.api.domain.Metadata;
import ai.timefold.solver.service.definition.api.domain.ModelInputPatchRequest;
import ai.timefold.solver.service.definition.api.rest.DatasetSelector;
import ai.timefold.solver.service.definition.internal.error.ItemNotFoundException;
import ai.timefold.solver.service.definition.internal.events.DatasetCreatedEvent;
import ai.timefold.solver.service.definition.internal.events.DatasetValidateComputeCommand;
import ai.timefold.solver.service.definition.internal.events.SolveStartCommand;
import ai.timefold.solver.service.definition.internal.events.SolveTerminateCommand;
import ai.timefold.solver.service.worker.impl.testdata.TestdataModelInput;
import ai.timefold.solver.service.worker.impl.testdata.TestdataStorage;
import ai.timefold.solver.service.worker.impl.testdata.TestdataStorageService;
import ai.timefold.solver.service.worker.impl.testutil.RecordingEmitter;
import ai.timefold.solver.service.worker.impl.testutil.RecordingMutinyEmitter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class DefaultSolverWorkerFacadeTest {

    private RecordingEmitter<DatasetCreatedEvent> datasetCreatedEmitter;
    private RecordingEmitter<DatasetValidateComputeCommand> validateComputeEmitter;
    private RecordingEmitter<SolveStartCommand> solveStartEmitter;
    private RecordingMutinyEmitter<SolveTerminateCommand> terminateEmitter;
    private TestdataStorageService storageService;

    private DefaultSolverWorkerFacade facade;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper();
        storageService = new TestdataStorageService(new TestdataStorage(mapper));

        datasetCreatedEmitter = new RecordingEmitter<>();
        validateComputeEmitter = new RecordingEmitter<>();
        solveStartEmitter = new RecordingEmitter<>();
        terminateEmitter = new RecordingMutinyEmitter<>();

        facade = new DefaultSolverWorkerFacade(
                storageService,
                datasetCreatedEmitter,
                validateComputeEmitter,
                solveStartEmitter,
                terminateEmitter,
                mapper);
    }

    @Test
    void createDataset_persistsAndEmitsValidateComputeWithoutSolve() {
        TestdataModelInput input = new TestdataModelInput("hello");
        String runName = uniqueRunName();

        Metadata<?> metadata = facade.createDataset(runName, Set.of("test-tag"), input, Configuration.empty());

        assertThat(metadata.getId()).isNotBlank();
        // dataset-created event emitted
        assertThat(datasetCreatedEmitter.size()).isEqualTo(1);
        assertThat(datasetCreatedEmitter.getLastMessage().getId()).isEqualTo(metadata.getId());
        // validate-compute with solve=false
        assertThat(validateComputeEmitter.size()).isEqualTo(1);
        DatasetValidateComputeCommand cmd = validateComputeEmitter.getLastMessage();
        assertThat(cmd.getId()).isEqualTo(metadata.getId());
        assertThat(cmd.solve()).isFalse();

        assertThat(metadata.getId()).isNotBlank();
        assertThat(metadata.getName()).isEqualTo(runName);
        assertThat(metadata.getSolverStatus()).isEqualTo(SolvingStatus.DATASET_CREATED);
        assertThat(metadata.getSubmitDateTime()).isNotNull();
        assertThat(metadata.getTags()).containsExactly("test-tag");
        assertThat(storageService.getModelInput(metadata.getId())).isEqualTo(input);
        assertThat(storageService.getMetadata(metadata.getId())).isNotNull()
                .extracting(Metadata::getId, Metadata::getName)
                .containsExactly(metadata.getId(), runName);
    }

    @Test
    void createAndSolveDataset_emitsValidateComputeWithSolveTrue() {
        String runName = uniqueRunName();
        Metadata<?> metadata =
                facade.createAndSolveDataset(runName, Set.of("test-tag"), new TestdataModelInput("x"),
                        Configuration.empty());

        assertThat(validateComputeEmitter.size()).isEqualTo(1);
        DatasetValidateComputeCommand cmd = validateComputeEmitter.getLastMessage();
        assertThat(cmd.solve()).isTrue();
        assertThat(cmd.getId()).isEqualTo(metadata.getId());

        assertThat(metadata.getTags()).containsExactly("test-tag");
        assertThat(storageService.getModelInput(metadata.getId()))
                .isEqualTo(new TestdataModelInput("x"));
        assertThat(storageService.getMetadata(metadata.getId()).getName()).isEqualTo(runName);
    }

    @Test
    void solveDataset_sendsStartCommand() {
        Metadata<?> parent = facade.createDataset(
                uniqueRunName(), Set.of("test-tag"), new TestdataModelInput("x"), Configuration.empty());

        facade.solveDataset(parent.getId());

        assertThat(solveStartEmitter.size()).isEqualTo(1);
        assertThat(solveStartEmitter.getLastMessage().getId()).isEqualTo(parent.getId());

        var metadata = facade.getMetadata(parent.getId());
        assertThat(metadata.getId()).isEqualTo(parent.getId());
        assertThat(metadata.getName()).isEqualTo(parent.getName());
        assertThat(metadata.getTags()).containsExactly("test-tag");
    }

    @Test
    void solveDataset_unknownId_throwsItemNotFound() {
        assertThatThrownBy(() -> facade.solveDataset("does-not-exist"))
                .isInstanceOf(ItemNotFoundException.class);
        assertThat(solveStartEmitter.size()).isEqualTo(0);
    }

    @Test
    void createDataset_fromExistingDataset_missingParent_throwsItemNotFound() {
        assertThatThrownBy(() -> facade.createDataset("missing", DatasetSelector.UNSOLVED, uniqueRunName(), Set.of(),
                Configuration.empty()))
                .isInstanceOf(ItemNotFoundException.class);
        assertThat(validateComputeEmitter.size()).isEqualTo(0);
    }

    @Test
    void createDataset_fromExistingDataset_emitsValidateComputeWithoutSolve() {
        Metadata<?> parent = facade.createDataset(
                uniqueRunName(), Set.of(), new TestdataModelInput("orig"), Configuration.empty());
        datasetCreatedEmitter.clear();
        validateComputeEmitter.clear();
        String childRunName = uniqueRunName();

        Metadata<?> child = facade.createDataset(parent.getId(), DatasetSelector.UNSOLVED, childRunName, Set.of("t2"),
                Configuration.empty());

        // emitter assertions
        assertThat(datasetCreatedEmitter.size()).isEqualTo(1);
        assertThat(datasetCreatedEmitter.getLastMessage().getId()).isEqualTo(child.getId());
        assertThat(validateComputeEmitter.size()).isEqualTo(1);
        assertThat(validateComputeEmitter.getLastMessage().solve()).isFalse();

        // metadata assertions — status was set exactly once, not reset to null nor thrown on double-set
        assertThat(child.getSolverStatus()).isEqualTo(SolvingStatus.DATASET_CREATED);
        assertThat(child.getName()).isEqualTo(childRunName);
        assertThat(child.getTags()).containsExactly("t2");
        assertThat(child.getParentId()).isEqualTo(parent.getId());
        assertThat(child.getOriginId()).isEqualTo(parent.getOriginId());

        // storage assertions — child dataset is stored independently with parent linkage
        assertThat(storageService.getMetadata(child.getId())).isNotNull()
                .extracting(Metadata::getParentId, Metadata::getOriginId)
                .containsExactly(parent.getId(), parent.getOriginId());
        assertThat(storageService.getModelInput(child.getId()))
                .isEqualTo(new TestdataModelInput("orig"));
        // parent input must not be affected
        assertThat(storageService.getModelInput(parent.getId()))
                .isEqualTo(new TestdataModelInput("orig"));
    }

    @Test
    void createAndSolveDataset_fromExistingDataset_emitsValidateComputeWithSolveTrue() {
        Metadata<?> parent = facade.createDataset(
                uniqueRunName(), Set.of(), new TestdataModelInput("orig"), Configuration.empty());
        validateComputeEmitter.clear();

        Metadata<?> child = facade.createAndSolveDataset(parent.getId(), DatasetSelector.UNSOLVED, uniqueRunName(),
                Set.of("test-child-tag"), Configuration.empty());

        assertThat(validateComputeEmitter.size()).isEqualTo(1);
        assertThat(validateComputeEmitter.getLastMessage().solve()).isTrue();
        assertThat(child.getSolverStatus()).isEqualTo(SolvingStatus.DATASET_CREATED);
        assertThat(child.getTags()).containsExactly("test-child-tag");
        assertThat(child.getParentId()).isEqualTo(parent.getId());
        assertThat(child.getOriginId()).isEqualTo(parent.getOriginId());
    }

    @Test
    void patchDataset_missingParent_throwsItemNotFound() {
        ModelInputPatchRequest<?> patch = new ModelInputPatchRequest<>(null, List.of());

        assertThatThrownBy(() -> facade.patchDataset("missing", DatasetSelector.UNSOLVED, uniqueRunName(), patch))
                .isInstanceOf(ItemNotFoundException.class);
        assertThat(validateComputeEmitter.size()).isEqualTo(0);
    }

    @Test
    void patchDataset_emitsValidateComputeWithoutSolve() {
        Metadata<?> parent = facade.createDataset(
                uniqueRunName(), Set.of(), new TestdataModelInput("orig"), Configuration.empty());
        validateComputeEmitter.clear();
        String childRunName = uniqueRunName();

        ModelInputPatchRequest<?> patch = new ModelInputPatchRequest<>(null, List.of());
        Metadata<?> child = facade.patchDataset(parent.getId(), DatasetSelector.UNSOLVED, childRunName, patch);

        // emitter assertions
        assertThat(validateComputeEmitter.size()).isEqualTo(1);
        assertThat(validateComputeEmitter.getLastMessage().solve()).isFalse();

        // storage assertions — child dataset is stored independently with parent linkage
        assertThat(child.getParentId()).isEqualTo(parent.getId());
        assertThat(child.getOriginId()).isEqualTo(parent.getOriginId());
        assertThat(storageService.getMetadata(child.getId())).isNotNull()
                .extracting(Metadata::getParentId, Metadata::getOriginId)
                .containsExactly(parent.getId(), parent.getOriginId());
        // the empty patch leaves the input unchanged
        assertThat(storageService.getModelInput(child.getId()))
                .isEqualTo(new TestdataModelInput("orig"));
        // parent input must not be affected
        assertThat(storageService.getModelInput(parent.getId()))
                .isEqualTo(new TestdataModelInput("orig"));
    }

    @Test
    void patchAndSolveDataset_emitsValidateComputeWithSolveTrue() {
        Metadata<?> parent = facade.createDataset(
                uniqueRunName(), Set.of(), new TestdataModelInput("orig"), Configuration.empty());
        validateComputeEmitter.clear();

        ModelInputPatchRequest<?> patch = new ModelInputPatchRequest<>(null, List.of());
        Metadata<?> child = facade.patchAndSolveDataset(
                parent.getId(), DatasetSelector.UNSOLVED, uniqueRunName(), patch);

        // emitter assertions
        assertThat(validateComputeEmitter.size()).isEqualTo(1);
        assertThat(validateComputeEmitter.getLastMessage().solve()).isTrue();

        // storage assertions
        assertThat(child.getParentId()).isEqualTo(parent.getId());
        assertThat(child.getOriginId()).isEqualTo(parent.getOriginId());
        assertThat(storageService.getMetadata(child.getId())).isNotNull();
        assertThat(storageService.getModelInput(child.getId()))
                .isEqualTo(new TestdataModelInput("orig"));
    }

    private static String uniqueRunName() {
        return "run-" + UUID.randomUUID();
    }
}