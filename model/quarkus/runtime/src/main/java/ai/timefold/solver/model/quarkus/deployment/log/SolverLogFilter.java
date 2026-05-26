package ai.timefold.solver.model.quarkus.deployment.log;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

import io.quarkus.logging.LoggingFilter;

/**
 * Dedicated log filter used by solver worker to log solver relevant and user facing logs
 */
@LoggingFilter(name = "solver-log-filter")
public final class SolverLogFilter implements Filter {

    @Override
    public boolean isLoggable(LogRecord record) {
        if (record.getLoggerName() != null && record.getLoggerName().contains(".insights.")) {
            // don't log anything related to insights
            return false;
        }
        if (record.getLoggerName() != null && record.getLoggerName().endsWith("ExceptionMapper")) {
            // don't log any messages from ExceptionMappers as they are technical details
            return false;
        }
        if (record.getLoggerName() != null
                && record.getLoggerName().equals("ai.timefold.solver.enterprise.core.LicenseChecker")) {
            return false;
        }
        return true;
    }
}
