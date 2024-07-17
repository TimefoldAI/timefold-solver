package ai.timefold.jpyinterpreter.types.wrappers;

/**
 * An OpaquePythonReference represents an
 * arbitrary Python Object. No methods
 * should ever be called on it, including
 * hashCode and equals. To operate on
 * the Object, pass it to a Python function.
 */
public interface OpaquePythonReference {
    // intentionally empty
}
