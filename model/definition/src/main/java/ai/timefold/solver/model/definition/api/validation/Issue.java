package ai.timefold.solver.model.definition.api.validation;

import java.util.List;

import org.jspecify.annotations.NonNull;

/**
 * Represents a single validation issue discovered during model validation.
 * <p>
 * Each issue has a {@link IssueCode code} that uniquely identifies its {@link IssueType type},
 * a {@link IssueSeverity severity} indicating how critical the issue is,
 * and optional {@link IssueMetadata metadata} providing additional context such as human-readable messages.
 * <p>
 * This is a sealed interface; all implementations must extend {@link AbstractIssue}.
 */
public sealed interface Issue permits AbstractIssue {

    /**
     * Returns the severity of this issue, indicating how critical it is.
     *
     * @return the {@link IssueSeverity}
     */
    @NonNull
    IssueSeverity getSeverity();

    /**
     * Returns the unique code identifying the type of this issue.
     *
     * @return the {@link IssueCode}
     */
    @NonNull
    IssueCode getCode();

    /**
     * Returns additional metadata associated with this issue, such as human-readable messages.
     *
     * @return an unmodifiable list of {@link IssueMetadata}; may be empty
     */
    @NonNull
    List<IssueMetadata> getMetadata();
}
