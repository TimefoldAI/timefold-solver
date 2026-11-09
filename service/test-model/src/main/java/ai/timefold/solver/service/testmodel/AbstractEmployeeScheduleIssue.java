package ai.timefold.solver.service.testmodel;

import java.util.List;

import ai.timefold.solver.service.definition.api.dto.validation.AbstractIssue;
import ai.timefold.solver.service.definition.api.dto.validation.IssueCode;
import ai.timefold.solver.service.definition.api.dto.validation.IssueMetadata;
import ai.timefold.solver.service.definition.api.dto.validation.IssueSeverity;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(oneOf = ShiftEndBeforeStartIssue.class)
public abstract class AbstractEmployeeScheduleIssue extends AbstractIssue {

    public AbstractEmployeeScheduleIssue(IssueCode code, IssueSeverity severity, List<IssueMetadata> metadata) {
        super(code, severity, metadata);
    }
}
