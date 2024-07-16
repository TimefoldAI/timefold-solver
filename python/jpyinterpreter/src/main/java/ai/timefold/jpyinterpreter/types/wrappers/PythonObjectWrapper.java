package ai.timefold.jpyinterpreter.types.wrappers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.jpyinterpreter.CPythonBackedPythonInterpreter;
import ai.timefold.jpyinterpreter.PythonInterpreter;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.builtins.UnaryDunderBuiltin;
import ai.timefold.jpyinterpreter.types.CPythonBackedPythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.collections.PythonIterator;
import ai.timefold.jpyinterpreter.types.errors.NotImplementedError;
import ai.timefold.jpyinterpreter.types.numeric.PythonBoolean;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;

public class PythonObjectWrapper extends CPythonBackedPythonLikeObject
        implements PythonLikeObject,
        PythonLikeFunction, PythonIterator<PythonLikeObject>, Comparable<PythonObjectWrapper> {

    private final static PythonLikeType PYTHON_REFERENCE_TYPE =
            new PythonLikeType("python-reference", PythonObjectWrapper.class),
            $TYPE = PYTHON_REFERENCE_TYPE;
    private final Map<String, PythonLikeObject> cachedAttributeMap;

    private PythonLikeObject cachedNext = null;

    public PythonObjectWrapper(OpaquePythonReference pythonReference) {
        super(PythonInterpreter.DEFAULT, CPythonType.lookupTypeOfPythonObject(pythonReference), pythonReference);
        cachedAttributeMap = new HashMap<>();
    }

    public OpaquePythonReference getWrappedObject() {
        return $cpythonReference;
    }

    @Override
    public PythonLikeObject $getAttributeOrNull(String attributeName) {
        return cachedAttributeMap.computeIfAbsent(attributeName,
                key -> CPythonBackedPythonInterpreter.lookupAttributeOnPythonReference($cpythonReference,
                        attributeName, $instanceMap));
    }

    @Override
    public void $setAttribute(String attributeName, PythonLikeObject value) {
        cachedAttributeMap.put(attributeName, value);
        CPythonBackedPythonInterpreter.setAttributeOnPythonReference($cpythonReference, null, attributeName, value);
    }

    @Override
    public void $deleteAttribute(String attributeName) {
        cachedAttributeMap.remove(attributeName);
        CPythonBackedPythonInterpreter.deleteAttributeOnPythonReference($cpythonReference, attributeName);
    }

    @Override
    public PythonLikeObject $call(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments, PythonLikeObject callerInstance) {
        return CPythonBackedPythonInterpreter.callPythonReference($cpythonReference, positionalArguments, namedArguments);
    }

    @Override
    public int compareTo(PythonObjectWrapper other) {
        if (equals(other)) {
            return 0;
        }

        PythonLikeFunction lessThan = (PythonLikeFunction) $getType().$getAttributeOrError("__lt__");
        PythonLikeObject result = lessThan.$call(List.of(this, other), Map.of(), null);

        if (result instanceof PythonBoolean) {
            if (((PythonBoolean) result).getBooleanValue()) {
                return -1;
            } else {
                return 1;
            }
        } else {
            throw new NotImplementedError("Cannot compare " + this.$getType().getTypeName() +
                    " with " + other.$getType().getTypeName());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PythonObjectWrapper)) {
            return false;
        }
        PythonObjectWrapper other = (PythonObjectWrapper) o;
        Object maybeEquals = $getType().$getAttributeOrNull("__eq__");
        if (!(maybeEquals instanceof PythonLikeFunction)) {
            return super.equals(o);
        }
        PythonLikeFunction equals = (PythonLikeFunction) maybeEquals;
        PythonLikeObject result = equals.$call(List.of(this, other), Map.of(), null);
        if (result instanceof PythonBoolean) {
            return ((PythonBoolean) result).getBooleanValue();
        }
        return false;
    }

    @Override
    public int hashCode() {
        Object maybeHash = $getType().$getAttributeOrNull("__hash__");
        if (!(maybeHash instanceof PythonLikeFunction)) {
            return super.hashCode();
        }
        PythonLikeFunction hash = (PythonLikeFunction) maybeHash;
        PythonLikeObject result = hash.$call(List.of(this), Map.of(), null);
        if (result instanceof PythonInteger) {
            return ((PythonInteger) result).value.hashCode();
        } else {
            return System.identityHashCode(this);
        }
    }

    @Override
    public PythonInteger $method$__hash__() {
        return PythonInteger.valueOf(hashCode());
    }

    @Override
    public String toString() {
        Object maybeStr = $getType().$getAttributeOrNull("__str__");
        if (!(maybeStr instanceof PythonLikeFunction)) {
            return super.toString();
        }
        PythonLikeFunction str = (PythonLikeFunction) maybeStr;
        PythonLikeObject result = str.$call(List.of(this), Map.of(), null);
        return result.toString();
    }

    @Override
    public PythonLikeObject nextPythonItem() {
        if (cachedNext != null) {
            var out = cachedNext;
            cachedNext = null;
            return out;
        }
        return UnaryDunderBuiltin.NEXT.invoke(this);
    }

    @Override
    public PythonIterator<PythonLikeObject> getIterator() {
        return (PythonIterator<PythonLikeObject>) UnaryDunderBuiltin.ITERATOR.invoke(this);
    }

    @Override
    public boolean hasNext() {
        if (cachedNext != null) {
            return true;
        }
        try {
            cachedNext = nextPythonItem();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public PythonLikeObject next() {
        return nextPythonItem();
    }
}
