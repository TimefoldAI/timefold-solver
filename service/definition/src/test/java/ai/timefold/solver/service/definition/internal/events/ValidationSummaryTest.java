package ai.timefold.solver.service.definition.internal.events;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import ai.timefold.solver.service.definition.api.validation.AbstractIssue;
import ai.timefold.solver.service.definition.api.validation.IssueCode;
import ai.timefold.solver.service.definition.api.validation.IssueSeverity;
import ai.timefold.solver.service.definition.api.validation.ValidationStatus;
import ai.timefold.solver.service.definition.api.validation.dto.ValidationResult;

import org.junit.jupiter.api.Test;

class ValidationSummaryTest {

    @Test
    void ofCountsIssuesByCodeAndSeverity() {
        ValidationResult<TestIssue> result = new ValidationResult<>(ValidationStatus.ERRORS,
                List.of(new TestIssue("MISSING_SKILL", IssueSeverity.ERROR),
                        new TestIssue("MISSING_SKILL", IssueSeverity.ERROR),
                        new TestIssue("UNUSED_VEHICLE", IssueSeverity.WARNING)));

        ValidationSummary summary = ValidationSummary.of(result);

        assertThat(summary.status()).isEqualTo(ValidationStatus.ERRORS);
        assertThat(summary.errorCount()).isEqualTo(2);
        assertThat(summary.warningCount()).isEqualTo(1);
        assertThat(summary.errorCountsByCode()).containsExactlyEntriesOf(Map.of("MISSING_SKILL", 2));
        assertThat(summary.warningCountsByCode()).containsExactlyEntriesOf(Map.of("UNUSED_VEHICLE", 1));
    }

    @Test
    void ofWithNoIssuesReportsZeroCounts() {
        ValidationSummary summary = ValidationSummary.of(new ValidationResult<>(ValidationStatus.OK, List.of()));

        assertThat(summary.status()).isEqualTo(ValidationStatus.OK);
        assertThat(summary.errorCount()).isZero();
        assertThat(summary.warningCount()).isZero();
        assertThat(summary.errorCountsByCode()).isEmpty();
        assertThat(summary.warningCountsByCode()).isEmpty();
    }

    @Test
    void ofWhenValidationNotSupportedCarriesStatusOnly() {
        ValidationSummary summary =
                ValidationSummary.of(new ValidationResult<>(ValidationStatus.VALIDATION_NOT_SUPPORTED, List.of()));

        assertThat(summary.status()).isEqualTo(ValidationStatus.VALIDATION_NOT_SUPPORTED);
        assertThat(summary.errorCount()).isNull();
        assertThat(summary.warningCount()).isNull();
        assertThat(summary.errorCountsByCode()).isNull();
        assertThat(summary.warningCountsByCode()).isNull();
    }

    private static final class TestIssue extends AbstractIssue {
        TestIssue(String code, IssueSeverity severity) {
            super(IssueCode.of(code), severity, List.of());
        }
    }
}
