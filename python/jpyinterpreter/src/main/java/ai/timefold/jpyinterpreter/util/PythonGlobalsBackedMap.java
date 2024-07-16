package ai.timefold.jpyinterpreter.util;

import java.util.HashMap;

import ai.timefold.jpyinterpreter.PythonLikeObject;

public class PythonGlobalsBackedMap extends HashMap<String, PythonLikeObject> {
    private final long pythonGlobalsId;

    public PythonGlobalsBackedMap(long pythonGlobalsId) {
        this.pythonGlobalsId = pythonGlobalsId;
    }

    public long getPythonGlobalsId() {
        return pythonGlobalsId;
    }
}
