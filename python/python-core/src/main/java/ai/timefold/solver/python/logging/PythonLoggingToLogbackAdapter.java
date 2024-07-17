package ai.timefold.solver.python.logging;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;

public class PythonLoggingToLogbackAdapter {
    private static final Logger LOGGER = ((LoggerContext) LoggerFactory.getILoggerFactory()).getLogger("ai.timefold");

    public static void setLevel(int logLevel) {
        PythonLogLevel pythonLogLevel = PythonLogLevel.fromPythonLevelNumber(logLevel);
        LOGGER.setLevel(pythonLogLevel.getJavaLogLevel());
    }
}
