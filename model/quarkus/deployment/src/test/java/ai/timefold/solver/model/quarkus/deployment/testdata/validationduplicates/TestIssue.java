package ai.timefold.solver.model.quarkus.deployment.testdata.validationduplicates;

import java.util.List;

import ai.timefold.solver.model.definition.api.validation.AbstractIssue;
import ai.timefold.solver.model.definition.api.validation.IssueCode;
import ai.timefold.solver.model.definition.api.validation.IssueSeverity;

public class TestIssue extends AbstractIssue {

    public static final IssueCode ISSUE_CODE = new IssueCode("DuplicateIssueCode");

    public TestIssue() {
        super(ISSUE_CODE, IssueSeverity.ERROR, List.of());
    }
}
