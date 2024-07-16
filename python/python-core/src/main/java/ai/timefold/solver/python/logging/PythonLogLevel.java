package ai.timefold.solver.python.logging;

import ch.qos.logback.classic.Level;

public enum PythonLogLevel {
    CRITICAL(50, Level.ERROR),
    ERROR(40, Level.ERROR),
    WARNING(30, Level.WARN),
    INFO(20, Level.INFO),
    DEBUG(10, Level.DEBUG),
    TRACE(5, Level.TRACE),
    NOTSET(0, Level.INFO);

    final int pythonLevelNumber;
    final Level javaLogLevel;

    PythonLogLevel(int pythonLevelNumber, Level javaLogLevel) {
        this.pythonLevelNumber = pythonLevelNumber;
        this.javaLogLevel = javaLogLevel;
    }

    public int getPythonLevelNumber() {
        return pythonLevelNumber;
    }

    public Level getJavaLogLevel() {
        return javaLogLevel;
    }

    public static PythonLogLevel fromJavaLevel(Level level) {
        // Check INFO and ERROR first, since they have multiple corresponding
        // Python levels
        if (level.equals(Level.INFO)) {
            return INFO;
        } else if (level.equals(Level.ERROR)) {
            return ERROR;
        } else {
            int levelNumber = level.toInt();
            for (PythonLogLevel pythonLogLevel : PythonLogLevel.values()) {
                if (pythonLogLevel.getJavaLogLevel().toInt() == levelNumber) {
                    return pythonLogLevel;
                }
            }
            throw new IllegalStateException("Unmatched log level (" + level + ") with level number (" + level.toInt() + ").");
        }
    }

    public static PythonLogLevel fromPythonLevelNumber(int levelNumber) {
        PythonLogLevel bestMatch = PythonLogLevel.CRITICAL;
        int bestMatchLevelNumber = 50;
        for (PythonLogLevel pythonLogLevel : PythonLogLevel.values()) {
            if (pythonLogLevel.pythonLevelNumber >= levelNumber && pythonLogLevel.pythonLevelNumber < bestMatchLevelNumber) {
                bestMatch = pythonLogLevel;
            }
        }
        return bestMatch;
    }
}
