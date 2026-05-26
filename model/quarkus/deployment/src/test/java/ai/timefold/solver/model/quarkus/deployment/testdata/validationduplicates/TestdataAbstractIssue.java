package ai.timefold.solver.model.quarkus.deployment.testdata.validationduplicates;

import java.util.List;

import ai.timefold.solver.model.definition.api.validation.AbstractIssue;
import ai.timefold.solver.model.definition.api.validation.IssueCode;
import ai.timefold.solver.model.definition.api.validation.IssueMetadata;
import ai.timefold.solver.model.definition.api.validation.IssueSeverity;

public abstract class TestdataAbstractIssue extends AbstractIssue {

    public TestdataAbstractIssue(IssueCode code, IssueSeverity severity, List<IssueMetadata> metadata) {
        super(code, severity, metadata);
    }
}
