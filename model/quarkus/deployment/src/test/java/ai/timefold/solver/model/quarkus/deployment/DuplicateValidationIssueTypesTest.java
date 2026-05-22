package ai.timefold.solver.model.quarkus.deployment;

import static ai.timefold.solver.model.quarkus.deployment.DefaultConfigProfileProcessor.MODEL_CONFIG_TERMINATION_SPENT_LIMIT;
import static ai.timefold.solver.model.worker.impl.termination.TerminationConfigParams.TERMINATION_SPENT_LIMIT;

import ai.timefold.solver.model.definition.api.validation.ValidationBuilder;
import ai.timefold.solver.model.quarkus.deployment.testdata.validationduplicates.DuplicateTestIssue;
import ai.timefold.solver.model.quarkus.deployment.testdata.validationduplicates.TestIssue;
import ai.timefold.solver.model.quarkus.deployment.testdata.validationduplicates.TestdataConstraintProvider;
import ai.timefold.solver.model.quarkus.deployment.testdata.validationduplicates.TestdataEntity;
import ai.timefold.solver.model.quarkus.deployment.testdata.validationduplicates.TestdataModelValidator;
import ai.timefold.solver.model.quarkus.deployment.testdata.validationduplicates.TestdataRest;
import ai.timefold.solver.model.quarkus.deployment.testdata.validationduplicates.TestdataSolution;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class DuplicateValidationIssueTypesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .assertException((throwable -> {
                Assertions.assertThat(throwable).isInstanceOf(IllegalStateException.class);
                Assertions.assertThat(throwable.getMessage())
                        .contains("IssueCode (%s) was found in 2 classes".formatted(TestIssue.ISSUE_CODE.toString()));
            }))
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestdataEntity.class, TestdataSolution.class, TestdataConstraintProvider.class,
                            TestdataModelValidator.class, ValidationBuilder.class, TestdataRest.class,
                            TestIssue.class, DuplicateTestIssue.class))
            .overrideConfigKey(MODEL_CONFIG_TERMINATION_SPENT_LIMIT, "PT1S")
            .overrideConfigKey(TERMINATION_SPENT_LIMIT, "PT1S");

    @Test
    void validationIssueTypesDuplicateDetected() {
        Assertions.fail("The test app is supposed to fail fast at build time.");
    }
}
