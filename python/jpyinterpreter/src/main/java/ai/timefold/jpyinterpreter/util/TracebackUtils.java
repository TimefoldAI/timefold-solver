package ai.timefold.jpyinterpreter.util;

import java.io.PrintWriter;
import java.io.StringWriter;

public class TracebackUtils {
    private TracebackUtils() {

    }

    public static String getTraceback(Throwable t) {
        var output = new StringWriter();
        PrintWriter printWriter = new PrintWriter(output);
        t.printStackTrace(printWriter);
        return output.toString();
    }
}
