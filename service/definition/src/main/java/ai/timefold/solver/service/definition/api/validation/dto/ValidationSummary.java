package ai.timefold.solver.service.definition.api.validation.dto;

import java.util.Map;

import ai.timefold.solver.service.definition.api.validation.ValidationStatus;

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
}
