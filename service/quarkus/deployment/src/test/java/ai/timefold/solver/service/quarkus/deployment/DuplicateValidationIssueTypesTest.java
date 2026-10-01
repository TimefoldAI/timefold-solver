package ai.timefold.solver.service.quarkus.deployment;

import ai.timefold.solver.service.definition.api.validation.ValidationBuilder;
import ai.timefold.solver.service.quarkus.deployment.testdata.validationduplicates.DuplicateTestIssue;
import ai.timefold.solver.service.quarkus.deployment.testdata.validationduplicates.TestIssue;
import ai.timefold.solver.service.quarkus.deployment.testdata.validationduplicates.TestdataAbstractIssue;
import ai.timefold.solver.service.quarkus.deployment.testdata.validationduplicates.TestdataConstraintProvider;
import ai.timefold.solver.service.quarkus.deployment.testdata.validationduplicates.TestdataEntity;
import ai.timefold.solver.service.quarkus.deployment.testdata.validationduplicates.TestdataModelValidator;
import ai.timefold.solver.service.quarkus.deployment.testdata.validationduplicates.TestdataRest;
import ai.timefold.solver.service.quarkus.deployment.testdata.validationduplicates.TestdataSolution;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class DuplicateValidationIssueTypesTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = ExtensionTestUtil
            .createDeploymentWithMandatoryConfig(TestdataEntity.class, TestdataSolution.class, TestdataConstraintProvider.class,
                    TestdataModelValidator.class, ValidationBuilder.class, TestdataRest.class,
                    TestdataAbstractIssue.class, TestIssue.class, DuplicateTestIssue.class)
            .assertException((throwable -> {
                Assertions.assertThat(throwable).isInstanceOf(IllegalStateException.class);
                Assertions.assertThat(throwable.getMessage())
                        .contains("IssueCode (%s) was found in 2 classes".formatted(TestIssue.ISSUE_CODE.toString()));
            }));

    @Test
    void validationIssueTypesDuplicateDetected() {
        Assertions.fail("The test app is supposed to fail fast at build time.");
    }
}
