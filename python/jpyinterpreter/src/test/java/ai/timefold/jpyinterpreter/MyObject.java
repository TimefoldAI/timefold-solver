package ai.timefold.jpyinterpreter;

import ai.timefold.jpyinterpreter.types.PythonLikeFunction;

public class MyObject {
    public String name;
    public PythonLikeFunction attributeFunction;

    public String concatToName(String other) {
        return name + other;
    }
}
