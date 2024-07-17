package ai.timefold.jpyinterpreter.types.collections;

import java.util.Iterator;

import ai.timefold.jpyinterpreter.PythonLikeObject;

public interface PythonIterator<T> extends PythonLikeObject, Iterator<T> {
    PythonLikeObject nextPythonItem();

    PythonIterator<T> getIterator();
}
