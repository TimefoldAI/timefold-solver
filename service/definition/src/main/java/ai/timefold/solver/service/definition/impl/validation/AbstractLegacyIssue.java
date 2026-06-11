package ai.timefold.solver.service.definition.impl.validation;

import java.util.List;

import ai.timefold.solver.service.definition.api.validation.AbstractIssue;
import ai.timefold.solver.service.definition.api.validation.IssueCode;
import ai.timefold.solver.service.definition.api.validation.IssueMetadata;
import ai.timefold.solver.service.definition.api.validation.IssueSeverity;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * For full backward compatibility with legacy validation issues of existing models.
 */
public abstract class AbstractLegacyIssue extends AbstractIssue {

    @Schema(hidden = true)
    @JsonIgnore
    private final String legacyMessage;

    public AbstractLegacyIssue(IssueCode code, IssueSeverity severity, List<IssueMetadata> metadata, String legacyMessage) {
        super(code, severity, metadata);
        this.legacyMessage = legacyMessage;
    }

    @JsonProperty
    public String legacyMessage() {
        return legacyMessage;
    }
}
