package ai.timefold.jpyinterpreter;

import java.util.List;
import java.util.Map;

import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.errors.PythonTraceback;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.jpyinterpreter.types.wrappers.OpaquePythonReference;

public interface PythonInterpreter {
    PythonInterpreter DEFAULT = new CPythonBackedPythonInterpreter();

    boolean hasValidPythonReference(PythonLikeObject instance);

    void setPythonReference(PythonLikeObject instance, OpaquePythonReference reference);

    PythonLikeObject getGlobal(Map<String, PythonLikeObject> globalsMap, String name);

    void setGlobal(Map<String, PythonLikeObject> globalsMap, String name, PythonLikeObject value);

    void deleteGlobal(Map<String, PythonLikeObject> globalsMap, String name);

    PythonLikeObject importModule(PythonInteger level, List<PythonString> fromList, Map<String, PythonLikeObject> globalsMap,
            Map<String, PythonLikeObject> localsMap, String moduleName);

    /**
     * Writes output without a trailing newline to standard output
     *
     * @param output the text to write
     */
    void write(String output);

    /**
     * Reads a line from standard input
     *
     * @return A line read from standard input
     */
    String readLine();

    PythonTraceback getTraceback();
}
