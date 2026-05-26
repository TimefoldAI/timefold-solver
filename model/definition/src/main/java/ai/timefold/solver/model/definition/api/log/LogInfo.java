package ai.timefold.solver.model.definition.api.log;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;

@JsonIncludeProperties("details")
public class LogInfo {

    public static final String LOG_ENTRY_SEPARATOR = "\n======================================================\n";

    private String details;

    public LogInfo() {

    }

    public LogInfo(String details) {
        this.details = details;
    }

    public String getDetails() {
        return details;
    }

    public void appendPreviousLog(String details) {

        this.details = details + LOG_ENTRY_SEPARATOR + this.details;
    }
}
