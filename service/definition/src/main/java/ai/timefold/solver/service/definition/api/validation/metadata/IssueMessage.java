package ai.timefold.solver.service.definition.api.validation.metadata;

import ai.timefold.solver.service.definition.api.validation.IssueMetadata;

import io.quarkus.runtime.annotations.RecordableConstructor;

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
