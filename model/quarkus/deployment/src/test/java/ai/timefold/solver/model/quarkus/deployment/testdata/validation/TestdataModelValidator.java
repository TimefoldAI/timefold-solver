package ai.timefold.solver.model.quarkus.deployment.testdata.validation;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import ai.timefold.solver.model.definition.api.domain.ModelConfig;
import ai.timefold.solver.model.definition.api.validation.AbstractIssue;
import ai.timefold.solver.model.definition.api.validation.IssueCode;
import ai.timefold.solver.model.definition.api.validation.IssueMetadata;
import ai.timefold.solver.model.definition.api.validation.IssueSeverity;
import ai.timefold.solver.model.definition.api.validation.ModelValidator;
import ai.timefold.solver.model.definition.api.validation.ValidationBuilder;
import ai.timefold.solver.model.definition.api.validation.metadata.IssueMessage;
import ai.timefold.solver.model.quarkus.deployment.defaults.EmptyModelConfigOverrides;

@ApplicationScoped
public class TestdataModelValidator
        implements ModelValidator<TestdataSolution, EmptyModelConfigOverrides> {

    public static final IssueCode TEST_ISSUE_CODE = IssueCode.of("MalformedEntityIssue");
    public static final IssueSeverity TEST_ISSUE_SEVERITY = IssueSeverity.ERROR;
    public static final List<IssueMetadata> TEST_ISSUE_METADATA = List.of(new IssueMessage("description"));

    @Override
    public void validate(ValidationBuilder validationBuilder, TestdataSolution modelInput,
            ModelConfig<EmptyModelConfigOverrides> modelConfig) {
        validationBuilder.addIssue(new TestIssue("test-id"));
    }

    public static class TestIssue extends AbstractIssue {

        private String id;

        public TestIssue() {
            super(TEST_ISSUE_CODE, TEST_ISSUE_SEVERITY, TEST_ISSUE_METADATA);
        }

        private TestIssue(String id) {
            this();
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }
}
