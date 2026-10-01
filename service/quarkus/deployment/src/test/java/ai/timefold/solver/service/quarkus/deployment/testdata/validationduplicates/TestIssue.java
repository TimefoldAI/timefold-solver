package ai.timefold.solver.service.quarkus.deployment.testdata.validationduplicates;

import java.util.List;

import ai.timefold.solver.service.definition.api.validation.IssueCode;
import ai.timefold.solver.service.definition.api.validation.IssueSeverity;

public class TestIssue extends TestdataAbstractIssue {

    public static final IssueCode ISSUE_CODE = new IssueCode("DuplicateIssueCode");

    public TestIssue() {
        super(ISSUE_CODE, IssueSeverity.ERROR, List.of());
    }
}
