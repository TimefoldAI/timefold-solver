package ai.timefold.solver.service.definition.api.validation;

import java.util.ArrayList;
import java.util.List;

import ai.timefold.solver.service.definition.api.validation.dto.ValidationResult;
import ai.timefold.solver.service.definition.api.validation.metadata.IssueMessage;
import ai.timefold.solver.service.definition.impl.validation.AbstractLegacyIssue;

/**
 * Registers individual validation issues to later build the {@link ValidationResult} instance.
 */
public final class ValidationBuilder {

    private final List<AbstractIssue> issues = new ArrayList<>();

    private ValidationStatus result = ValidationStatus.OK;

    /**
     * Adds a new validation issue instance.
     *
     * @param issue {@link Issue} issue to register with this {@link ValidationBuilder} instance
     * @return instance of this builder
     */
    public ValidationBuilder addIssue(Issue issue) {
        result = determineNewSummary(result, issue.getSeverity());
        issues.add((AbstractIssue) issue);
        return this;
    }

    /**
     * Marks validation as unsupported on the model.
     */
    public ValidationBuilder unsupported() {
        result = ValidationStatus.VALIDATION_NOT_SUPPORTED;
        return this;
    }

    /**
     * Determines a new {@link ValidationStatus}
     * based on its previous value and the {@link IssueSeverity}.
     */
    private ValidationStatus determineNewSummary(ValidationStatus previousStatus, IssueSeverity issueSeverity) {
        if (previousStatus == ValidationStatus.VALIDATION_NOT_SUPPORTED) {
            throw new IllegalStateException("Validation is not supported");
        }
        if (issueSeverity == IssueSeverity.ERROR) {
            return ValidationStatus.ERRORS;
        }

        if (previousStatus == ValidationStatus.OK) {
            return ValidationStatus.WARNINGS;
        }

        return previousStatus;
    }

    /**
     * Determines if the validation issues registered so far do not contain errors.
     *
     * @return {@code true} if the registered validation issues do not contain an issue with {@link IssueSeverity#ERROR};
     *         {@code false} otherwise.
     */
    public boolean isValid() {
        return result != ValidationStatus.ERRORS;
    }

    /**
     * Builds {@link ValidationResult} from individual issues registered to its instance.
     */
    public <T extends Issue> ValidationResult<T> build() {
        return new ValidationResult<>(result, (List<T>) issues);
    }

    /**
     * Builds {@link LegacyValidationResult} from individual issues registered to its instance.
     * <p>
     * Keeps backward compatibility with the previous validation result format.
     */
    public LegacyValidationResult buildLegacyValidationResult() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (var issue : issues) {
            String legacyMessage = toLegacyMessage(issue);
            if (issue.getSeverity() == IssueSeverity.ERROR) {
                errors.add(legacyMessage);
            } else {
                warnings.add(legacyMessage);
            }
        }

        return new LegacyValidationResult(result, errors, warnings);
    }

    private String toLegacyMessage(AbstractIssue issue) {
        if (issue instanceof AbstractLegacyIssue legacyIssue) {
            return legacyIssue.legacyMessage();
        }

        // If no direct legacy message was provided, search for the right metadata.
        for (var issueTypeMetadata : issue.getMetadata()) {
            if (issueTypeMetadata instanceof IssueMessage(String message)) {
                return message;
            }
        }

        // As a last resort, create a generic message.
        String errorOrWarning = issue.getSeverity() == IssueSeverity.ERROR ? "error" : "warning";
        return "Validation %s of code (%s) occurred.".formatted(errorOrWarning, issue.getCode());
    }
}
