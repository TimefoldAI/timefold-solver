package ai.timefold.solver.service.definition.api.dto.validation.issue.metadata;

import io.quarkus.runtime.annotations.RecordableConstructor;

/**
 * Represents a human-readable message related to an {@link ai.timefold.solver.service.definition.api.dto.validation.issue.IssueType}.
 */
public record IssueMessage(String message) implements IssueMetadata {

    public static final String TYPE = "Message";

    @RecordableConstructor
    public IssueMessage {
    }

    @Override
    public String getType() {
        return TYPE;
    }
}
