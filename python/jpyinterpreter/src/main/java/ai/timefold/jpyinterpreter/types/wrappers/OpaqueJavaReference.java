package ai.timefold.jpyinterpreter.types.wrappers;

import ai.timefold.jpyinterpreter.PythonLikeObject;

/**
 * An interface used to indicate a Java Object
 * should not be interacted with directly and should
 * be proxied (even if it implements {@link PythonLikeObject}).
 */
public interface OpaqueJavaReference {
    /**
     * Creates a proxy of the OpaqueJavaReference, which
     * can be interacted with directly.
     */
    PythonLikeObject proxy();
}
