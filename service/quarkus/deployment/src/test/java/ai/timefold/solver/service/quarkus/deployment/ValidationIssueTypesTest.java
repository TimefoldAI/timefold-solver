package ai.timefold.solver.service.quarkus.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import jakarta.inject.Inject;

import ai.timefold.solver.service.definition.api.domain.ModelConfig;
import ai.timefold.solver.service.definition.api.validation.IssueType;
import ai.timefold.solver.service.definition.api.validation.ValidationBuilder;
import ai.timefold.solver.service.definition.impl.validation.ValidationIssueTypeCatalog;
import ai.timefold.solver.service.quarkus.deployment.testdata.validation.TestdataConstraintProvider;
import ai.timefold.solver.service.quarkus.deployment.testdata.validation.TestdataEntity;
import ai.timefold.solver.service.quarkus.deployment.testdata.validation.TestdataModelValidator;
import ai.timefold.solver.service.quarkus.deployment.testdata.validation.TestdataRest;
import ai.timefold.solver.service.quarkus.deployment.testdata.validation.TestdataSolution;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.quarkus.test.QuarkusExtensionTest;

public class ValidationIssueTypesTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = ExtensionTestUtil.createDeploymentWithMandatoryConfig(TestdataEntity.class,
            TestdataSolution.class, TestdataConstraintProvider.class, TestdataModelValidator.class,
            ValidationBuilder.class, IssueType.class, TestdataRest.class, TestdataModelValidator.TestIssue.class);

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ValidationIssueTypeCatalog validationIssueTypeCatalog;

    @Inject
    TestdataModelValidator validator;

    @Test
    void validate() {
        ValidationBuilder validationBuilder = new ValidationBuilder();
        validator.validate(validationBuilder, new TestdataSolution(), ModelConfig.empty());

        var validationResult = validationBuilder.build();
        assertThat(validationResult.isValid()).isFalse();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(validationResult.issues()).hasSize(1);
            var issue = validationResult.issues().iterator().next();
            softly.assertThat(issue.getCode()).isEqualTo(TestdataModelValidator.TEST_ISSUE_CODE);
            softly.assertThat(issue.getSeverity()).isEqualTo(TestdataModelValidator.TEST_ISSUE_SEVERITY);
            softly.assertThat(((TestdataModelValidator.TestIssue) issue).getId()).isEqualTo("test-id");
        });
    }

    @Test
    void validationIssueTypeCatalogBeanGenerated() {
        Collection<IssueType> issueTypes = validationIssueTypeCatalog.getIssueTypes();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(issueTypes).hasSize(1);
            var issueType = issueTypes.iterator().next();
            softly.assertThat(issueType.code()).isEqualTo(TestdataModelValidator.TEST_ISSUE_CODE);
            softly.assertThat(issueType.severity()).isEqualTo(TestdataModelValidator.TEST_ISSUE_SEVERITY);
            softly.assertThat(issueType.metadata()).isEqualTo(TestdataModelValidator.TEST_ISSUE_METADATA);
        });

    }

    @Test
    void validationIssueTypesWrittenInDescriptor() throws IOException {
        File validationIssueTypesFile =
                new File("target/timefold/timefold-solver-service-quarkus-deployment/validation/validation-issue-types.json");

        List<IssueType> issueTypes = objectMapper.readValue(validationIssueTypesFile,
                TypeFactory.defaultInstance().constructCollectionType(List.class, IssueType.class));

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(issueTypes).hasSize(1);
            var issueType = issueTypes.getFirst();
            softly.assertThat(issueType.code()).isEqualTo(TestdataModelValidator.TEST_ISSUE_CODE);
            softly.assertThat(issueType.severity()).isEqualTo(TestdataModelValidator.TEST_ISSUE_SEVERITY);
            softly.assertThat(issueType.metadata()).isEqualTo(TestdataModelValidator.TEST_ISSUE_METADATA);
        });
    }
}
