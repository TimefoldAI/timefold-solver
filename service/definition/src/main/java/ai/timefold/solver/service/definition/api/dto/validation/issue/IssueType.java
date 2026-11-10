package ai.timefold.solver.service.definition.api.dto.validation.issue;

import java.util.List;

import ai.timefold.solver.service.definition.api.dto.validation.issue.metadata.IssueMessage;

import ai.timefold.solver.service.definition.api.dto.validation.issue.metadata.IssueMetadata;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import io.quarkus.runtime.annotations.RecordableConstructor;

public record IssueType(
        @Schema(description = "Unique case-sensitive code of the issue type.", required = true) IssueCode code,
        @Schema(description = "Issue severity", required = true) IssueSeverity severity,
        List<IssueMetadata> metadata) {

    @RecordableConstructor
    public IssueType {
    }

    public IssueType(IssueCode code, IssueSeverity severity, String message) {
        this(code, severity, List.of(new IssueMessage(message)));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IssueType that))
            return false;

        return code.equals(that.code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    @Override
    public String toString() {
        return code.toString();
    }
}
