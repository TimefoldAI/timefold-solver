package ai.timefold.jpyinterpreter.types.wrappers;

import java.util.List;
import java.util.Map;

import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonString;

/**
 * A class used as a delegator to another PythonLikeFunction.
 * Used to handle recursion correctly when translating functions.
 */
public final class PythonLikeFunctionWrapper implements PythonLikeFunction {
    PythonLikeFunction wrapped;

    public PythonLikeFunctionWrapper() {
        this.wrapped = null;
    }

    public PythonLikeFunction getWrapped() {
        return wrapped;
    }

    public void setWrapped(PythonLikeFunction wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public PythonLikeObject $call(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments, PythonLikeObject callerInstance) {
        return wrapped.$call(positionalArguments, namedArguments, callerInstance);
    }

    public PythonLikeObject $getAttributeOrNull(String attributeName) {
        return wrapped.$getAttributeOrNull(attributeName);
    }

    public PythonLikeObject $getAttributeOrError(String attributeName) {
        return wrapped.$getAttributeOrError(attributeName);
    }

    public void $setAttribute(String attributeName, PythonLikeObject value) {
        wrapped.$setAttribute(attributeName, value);
    }

    public void $deleteAttribute(String attributeName) {
        wrapped.$deleteAttribute(attributeName);
    }

    public PythonLikeType $getType() {
        return wrapped.$getType();
    }

    public PythonLikeObject $method$__getattribute__(PythonString pythonName) {
        return wrapped.$method$__getattribute__(pythonName);
    }

    public PythonLikeObject $method$__setattr__(PythonString pythonName, PythonLikeObject value) {
        return wrapped.$method$__setattr__(pythonName, value);
    }

    public PythonLikeObject $method$__delattr__(PythonString pythonName) {
        return wrapped.$method$__delattr__(pythonName);
    }

    public PythonLikeObject $method$__eq__(PythonLikeObject other) {
        return wrapped.$method$__eq__(other);
    }

    public PythonLikeObject $method$__ne__(PythonLikeObject other) {
        return wrapped.$method$__ne__(other);
    }

    public PythonString $method$__str__() {
        return wrapped.$method$__str__();
    }

    public PythonLikeObject $method$__repr__() {
        return wrapped.$method$__repr__();
    }

    public PythonLikeObject $method$__format__() {
        return wrapped.$method$__format__();
    }

    public PythonLikeObject $method$__format__(PythonLikeObject formatString) {
        return wrapped.$method$__format__(formatString);
    }

    public PythonLikeObject $method$__hash__() {
        return wrapped.$method$__hash__();
    }
}
