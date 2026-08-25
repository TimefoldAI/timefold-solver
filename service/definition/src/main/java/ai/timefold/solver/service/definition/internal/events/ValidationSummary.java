package ai.timefold.solver.service.definition.internal.events;

import java.util.HashMap;
import java.util.Map;

import ai.timefold.solver.service.definition.api.validation.Issue;
import ai.timefold.solver.service.definition.api.validation.IssueSeverity;
import ai.timefold.solver.service.definition.api.validation.ValidationStatus;
import ai.timefold.solver.service.definition.api.validation.dto.ValidationResult;

/**
 * A compact summary of a dataset validation, carrying the overall {@link ValidationStatus} and issue counts by
 * {@code IssueCode} split by severity.
 * <p>
 * When the model does not support validation, {@code status} is {@link ValidationStatus#VALIDATION_NOT_SUPPORTED} and
 * the counts are {@code null}, so they can be omitted downstream rather than reported as zero.
 */
public record ValidationSummary(
        ValidationStatus status,
        Integer errorCount, Integer warningCount,
        Map<String, Integer> errorCountsByCode,
        Map<String, Integer> warningCountsByCode) {

    public static ValidationSummary of(ValidationResult<?> validationResult) {
        ValidationStatus status = validationResult.status();
        if (status == ValidationStatus.VALIDATION_NOT_SUPPORTED) {
            return new ValidationSummary(status, null, null, null, null);
        }
        Map<String, Integer> errorCountsByCode = new HashMap<>();
        Map<String, Integer> warningCountsByCode = new HashMap<>();
        for (Issue issue : validationResult.issues()) {
            var countsByCode = issue.getSeverity() == IssueSeverity.ERROR ? errorCountsByCode : warningCountsByCode;
            countsByCode.merge(issue.getCode().value(), 1, Integer::sum);
        }
        int errorCount = errorCountsByCode.values().stream().mapToInt(Integer::intValue).sum();
        int warningCount = warningCountsByCode.values().stream().mapToInt(Integer::intValue).sum();
        return new ValidationSummary(status, errorCount, warningCount, errorCountsByCode, warningCountsByCode);
    }
}
