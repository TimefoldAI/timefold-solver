package ai.timefold.solver.service.quarkus.deployment.testdata.validationduplicates;

import java.util.List;

import ai.timefold.solver.service.definition.api.dto.validation.issue.AbstractIssue;
import ai.timefold.solver.service.definition.api.dto.validation.issue.IssueCode;
import ai.timefold.solver.service.definition.api.dto.validation.issue.IssueSeverity;
import ai.timefold.solver.service.definition.api.dto.validation.issue.metadata.IssueMetadata;

public abstract class TestdataAbstractIssue extends AbstractIssue {

    public TestdataAbstractIssue(IssueCode code, IssueSeverity severity, List<IssueMetadata> metadata) {
        super(code, severity, metadata);
    }
}
