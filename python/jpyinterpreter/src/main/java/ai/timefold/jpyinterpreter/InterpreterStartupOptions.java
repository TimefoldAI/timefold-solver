package ai.timefold.jpyinterpreter;

import java.nio.file.Path;

/**
 * A class that holds startup options for the interpreter that are used when the JVM starts
 */
public final class InterpreterStartupOptions {

    /**
     * Where to output class files; defaults to null (which cause not class files to not be written)
     */
    public static Path classOutputRootPath = null;
}
