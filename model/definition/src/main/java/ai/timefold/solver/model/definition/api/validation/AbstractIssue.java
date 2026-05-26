package ai.timefold.solver.model.definition.api.validation;

import java.util.Collections;
import java.util.List;

import ai.timefold.solver.model.definition.impl.validation.ValidationIssueTypeCatalog;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Base class for all validation issues.
 * <p>
 * Users should extend this class to define custom issue types for their model validators.
 * <p>
 * Subclasses are discovered automatically at build time via Quarkus and registered
 * in the {@link ValidationIssueTypeCatalog}.
 * Each concrete subclass must have a no-arg constructor so it can be instantiated
 * during the build-time discovery process.
 * <p>
 * Example:
 *
 * <pre>{@code
 * public class MyIssue extends AbstractIssue {
 *     public static final IssueCode CODE = new IssueCode("MY_ISSUE");
 *
 *     public MyIssue() {
 *         super(CODE, IssueSeverity.ERROR, List.of(new IssueMessage("Something went wrong.")));
 *     }
 * }
 * }</pre>
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public non-sealed abstract class AbstractIssue implements Issue {

    @Schema(description = "Issue code referring to an issue type.", implementation = String.class,
            required = true)
    private final IssueCode code;

    @Schema(description = "Issue severity", implementation = IssueSeverity.class, required = true)
    private final IssueSeverity severity;

    @JsonIgnore
    @Schema(hidden = true)
    private final List<IssueMetadata> metadata;

    public AbstractIssue(IssueCode code, IssueSeverity severity, List<IssueMetadata> metadata) {
        this.severity = severity;
        this.code = code;
        this.metadata = Collections.unmodifiableList(metadata);
    }

    @Override
    public IssueSeverity getSeverity() {
        return severity;
    }

    @Override
    public IssueCode getCode() {
        return code;
    }

    @Override
    public List<IssueMetadata> getMetadata() {
        return metadata;
    }

}
