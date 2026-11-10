package ai.timefold.solver.service.definition.api.dto.validation;

import java.util.Collection;

import ai.timefold.solver.service.definition.api.dto.validation.issue.IssueType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

public record ValidationIssueTypes(
        @Schema(description = "List of all supported validation issue types together with their metadata.",
                required = true) Collection<IssueType> issueTypes) {
}
