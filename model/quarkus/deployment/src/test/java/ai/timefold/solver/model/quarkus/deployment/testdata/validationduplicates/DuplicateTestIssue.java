package ai.timefold.solver.model.quarkus.deployment.testdata.validationduplicates;

import java.util.List;

import ai.timefold.solver.model.definition.api.validation.AbstractIssue;
import ai.timefold.solver.model.definition.api.validation.IssueSeverity;

public class DuplicateTestIssue extends AbstractIssue {

    public DuplicateTestIssue() {
        super(TestIssue.ISSUE_CODE, IssueSeverity.ERROR, List.of());
    }
}
