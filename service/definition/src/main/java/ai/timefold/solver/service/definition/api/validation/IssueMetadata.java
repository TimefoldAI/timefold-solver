package ai.timefold.solver.service.definition.api.validation;

import ai.timefold.solver.service.definition.api.validation.metadata.IssueMessage;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = IssueMessage.class, name = IssueMessage.TYPE)
})
@Schema(oneOf = { IssueMessage.class })
public interface IssueMetadata {

    @Schema(description = "The type of the issue type detail.", implementation = String.class, readOnly = true)
    @JsonProperty(value = "type", access = JsonProperty.Access.READ_ONLY)
    String getType();
}
