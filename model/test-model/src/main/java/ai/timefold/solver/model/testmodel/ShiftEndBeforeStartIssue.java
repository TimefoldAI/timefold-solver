package ai.timefold.solver.model.testmodel;

import java.time.OffsetDateTime;
import java.util.List;

import ai.timefold.solver.model.definition.api.validation.IssueCode;
import ai.timefold.solver.model.definition.api.validation.IssueSeverity;
import ai.timefold.solver.model.definition.api.validation.metadata.IssueMessage;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(allOf = { AbstractEmployeeScheduleIssue.class })
public class ShiftEndBeforeStartIssue extends AbstractEmployeeScheduleIssue {

    public static final IssueCode ISSUE_CODE = IssueCode.of("SHIFT_END_BEFORE_START");
    public static final IssueMessage ISSUE_MESSAGE = new IssueMessage("Shift has its end time before its start time.");

    private String shiftId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    public ShiftEndBeforeStartIssue() {
        this(null, null, null);
    }

    public ShiftEndBeforeStartIssue(String shiftId, OffsetDateTime startTime, OffsetDateTime endTime) {
        super(ISSUE_CODE, IssueSeverity.ERROR, List.of(ISSUE_MESSAGE));
        this.shiftId = shiftId;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public String getShiftId() {
        return shiftId;
    }

    public OffsetDateTime getStartTime() {
        return startTime;
    }

    public OffsetDateTime getEndTime() {
        return endTime;
    }
}
