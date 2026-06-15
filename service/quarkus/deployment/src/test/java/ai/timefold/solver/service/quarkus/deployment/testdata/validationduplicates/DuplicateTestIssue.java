package ai.timefold.solver.service.quarkus.deployment.testdata.validationduplicates;

import java.util.List;

import ai.timefold.solver.service.definition.api.validation.IssueSeverity;

public class DuplicateTestIssue extends TestdataAbstractIssue {

    public DuplicateTestIssue() {
        super(TestIssue.ISSUE_CODE, IssueSeverity.ERROR, List.of());
    }
}
