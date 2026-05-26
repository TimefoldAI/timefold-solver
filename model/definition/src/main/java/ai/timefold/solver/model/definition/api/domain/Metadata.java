package ai.timefold.solver.model.definition.api.domain;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import ai.timefold.solver.model.definition.api.SolvingStatus;
import ai.timefold.solver.model.definition.api.Status;
import ai.timefold.solver.model.definition.api.validation.LegacyValidationResult;
import ai.timefold.solver.model.definition.api.validation.ModelValidator;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;

public final class Metadata<Score_> implements Status<Score_> {

    private String id;

    @Schema(nullable = true, description = "The id of the parent data set this was created from")
    private String parentId;

    @Schema(nullable = true, description = "The id of the origin (root) data set this initially originates from")
    private String originId;

    @Schema(nullable = true)
    private String name;

    @Schema(nullable = true, description = "The moment the run is submitted")
    @JsonAlias({ "submitdatetime" })
    private OffsetDateTime submitDateTime;

    @Schema(nullable = true, description = "The moment the run begins initializing")
    @JsonAlias({ "startdatetime" })
    private OffsetDateTime startDateTime;

    @Schema(nullable = true, description = "The moment the solving phase begins")
    @JsonAlias({ "activedatetime" })
    private OffsetDateTime activeDateTime;

    @Schema(nullable = true, description = "The moment the solving phase concludes")
    @JsonAlias({ "completedatetime" })
    private OffsetDateTime completeDateTime;

    @Schema(nullable = true, description = "The moment the post-processing phase finishes")
    @JsonAlias({ "shutdowndatetime" })
    private OffsetDateTime shutdownDateTime;

    @Schema(nullable = true)
    @JsonAlias({ "solverstatus" })
    private SolvingStatus solverStatus;

    // TODO workaround, remove if fixed: https://github.com/quarkusio/quarkus/issues/32568
    @Schema(name = "score", type = SchemaType.STRING, implementation = String.class, nullable = true)
    private Score_ score;

    private Set<String> tags;

    private LegacyValidationResult validationResult;

    @Schema(nullable = true,
            description = "The message describing the reason of failure, in case of solverStatus=SOLVING_FAILED.")
    @JsonAlias({ "failuremessage" })
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String failureMessage;

    public Metadata() {
        this((String) null);
    }

    public Metadata(String name) {
        this(name, null);
    }

    public Metadata(String name, Score_ score) {
        this.id = UUID.randomUUID().toString();
        this.originId = this.id;
        this.submitDateTime = OffsetDateTime.now();
        this.name = name != null ? name : defaultJobName() + "-" + submitDateTime.toString();
        this.score = score;
        this.solverStatus = null; // The status needs to be set explicitly after creation.
    }

    public Metadata(Metadata<Score_> metadata) {
        this.id = metadata.id;
        this.name = metadata.name;
        this.submitDateTime = metadata.submitDateTime;
        this.startDateTime = metadata.startDateTime;
        this.completeDateTime = metadata.completeDateTime;
        this.solverStatus = metadata.solverStatus;
        this.score = metadata.score;
        this.tags = metadata.tags;
        this.validationResult = metadata.validationResult;
        this.shutdownDateTime = metadata.shutdownDateTime;
        this.activeDateTime = metadata.activeDateTime;
        this.parentId = metadata.parentId;
        this.originId = metadata.originId;
        this.failureMessage = metadata.failureMessage;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public String getOriginId() {
        return originId;
    }

    public void setOriginId(String originId) {
        this.originId = originId;
    }

    public SolvingStatus getSolverStatus() {
        return solverStatus;
    }

    public void setSolverStatus(SolvingStatus solverStatus) {
        this.solverStatus = solverStatus;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public OffsetDateTime getSubmitDateTime() {
        return submitDateTime;
    }

    public void setSubmitDateTime(OffsetDateTime submitDateTime) {
        this.submitDateTime = submitDateTime;
    }

    public OffsetDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(OffsetDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public OffsetDateTime getCompleteDateTime() {
        return completeDateTime;
    }

    public void setCompleteDateTime(OffsetDateTime completeDateTime) {
        this.completeDateTime = completeDateTime;
    }

    public OffsetDateTime getShutdownDateTime() {
        return shutdownDateTime;
    }

    public void setShutdownDateTime(OffsetDateTime shutdownDateTime) {
        this.shutdownDateTime = shutdownDateTime;
    }

    public OffsetDateTime getActiveDateTime() {
        return activeDateTime;
    }

    public void setActiveDateTime(OffsetDateTime activeDateTime) {
        this.activeDateTime = activeDateTime;
    }

    public Score_ getScore() {
        return score;
    }

    public void setScore(Score_ score) {
        this.score = score;
    }

    public Set<String> getTags() {
        return tags;
    }

    public void setTags(Set<String> tags) {
        this.tags = tags;
    }

    public LegacyValidationResult getValidationResult() {
        return validationResult;
    }

    public void setValidationResult(LegacyValidationResult validationResult) {
        this.validationResult = validationResult;
    }

    public void datasetCreated() {
        if (solverStatus != null) {
            throw new IllegalStateException(
                    "Dataset already has a status (%s).".formatted(solverStatus));
        }
        this.solverStatus = SolvingStatus.DATASET_CREATED;
    }

    /**
     * Sets the validation result and updates the solving status accordingly.
     * <p>
     * Do note that the method does not perform any validation itself, it is expected that the validation has already
     * been performed by a {@link ModelValidator} and the result is passed to this method.
     *
     * @param validationResult validation result
     */
    public void datasetValidated(LegacyValidationResult validationResult) {
        if (solverStatus == SolvingStatus.DATASET_CREATED ||
                solverStatus == SolvingStatus.SOLVING_SCHEDULED) {
            this.validationResult = Objects.requireNonNull(validationResult);

            if (validationResult.isValid()) {
                this.solverStatus = SolvingStatus.DATASET_VALIDATED;
            } else {
                this.solverStatus = SolvingStatus.DATASET_INVALID;
            }
        } else {
            throw new IllegalStateException(
                    "Expected dataset status to be %s or %s, but was %s.".formatted(SolvingStatus.DATASET_CREATED,
                            SolvingStatus.SOLVING_SCHEDULED, solverStatus));
        }
    }

    /**
     * Changes the status of the dataset as computed.
     */
    public void datasetComputed() {
        if (solverStatus != SolvingStatus.DATASET_VALIDATED) {
            throw new IllegalStateException(
                    "Expected dataset status %s, but was %s.".formatted(SolvingStatus.DATASET_VALIDATED, solverStatus));
        }
        this.solverStatus = SolvingStatus.DATASET_COMPUTED;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    @Override
    public void solvingStarted() {
        if (solverStatus != SolvingStatus.DATASET_COMPUTED
                && solverStatus != SolvingStatus.SOLVING_SCHEDULED) {
            throw new IllegalStateException(
                    "Expected dataset status %s or %s, but was %s.".formatted(SolvingStatus.DATASET_COMPUTED,
                            SolvingStatus.SOLVING_SCHEDULED, solverStatus));
        }
        this.solverStatus = SolvingStatus.SOLVING_STARTED;
        this.startDateTime = OffsetDateTime.now();
        this.completeDateTime = null;
        this.shutdownDateTime = null;
        this.activeDateTime = null;
        this.failureMessage = null;
    }

    @Override
    public void solvingActive() {
        if (solverStatus != SolvingStatus.SOLVING_STARTED) {
            throw new IllegalStateException(
                    "Expected dataset status %s, but was %s.".formatted(SolvingStatus.SOLVING_STARTED, solverStatus));
        }
        this.solverStatus = SolvingStatus.SOLVING_ACTIVE;
        this.activeDateTime = OffsetDateTime.now();
    }

    @Override
    public void updateStatusOnSave(SolvingStatus solverStatus, Score_ score) {
        this.solverStatus = solverStatus;
        this.score = score;
    }

    @Override
    public void updateStatusOnComplete(SolvingStatus solverStatus, Score_ score) {
        this.solverStatus = solverStatus;
        this.score = score;
        this.completeDateTime = OffsetDateTime.now();
    }

    @Override
    public void updateStatusOnFailure(String failureMessage) {
        this.solverStatus = SolvingStatus.SOLVING_FAILED;
        this.score = null;
        this.failureMessage = failureMessage;
        this.completeDateTime = OffsetDateTime.now();
    }

    public void shutdown() {
        this.shutdownDateTime = OffsetDateTime.now();
    }

    @Override
    public Map<String, String> asMap() {
        Map<String, String> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("submitDateTime", submitDateTime.format(DateTimeFormatter.ISO_DATE_TIME));
        if (startDateTime != null) {
            map.put("startDateTime", startDateTime.format(DateTimeFormatter.ISO_DATE_TIME));
        }
        if (activeDateTime != null) {
            map.put("activeDateTime", activeDateTime.format(DateTimeFormatter.ISO_DATE_TIME));
        }
        if (completeDateTime != null) {
            map.put("completeDateTime", completeDateTime.format(DateTimeFormatter.ISO_DATE_TIME));
        }
        if (shutdownDateTime != null) {
            map.put("shutdownDateTime", shutdownDateTime.format(DateTimeFormatter.ISO_DATE_TIME));
        }
        map.put("solverStatus", solverStatus == null ? SolvingStatus.DATASET_CREATED.name() : solverStatus.name());
        if (score != null) {
            map.put("score", score.toString());
        }
        return map;
    }

    @Override
    public String defaultJobName() {
        return "Dataset";
    }
}
