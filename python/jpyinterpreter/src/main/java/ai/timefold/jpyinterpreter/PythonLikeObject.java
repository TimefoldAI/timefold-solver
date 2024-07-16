package ai.timefold.jpyinterpreter;

import java.util.Objects;

import ai.timefold.jpyinterpreter.builtins.TernaryDunderBuiltin;
import ai.timefold.jpyinterpreter.types.CPythonBackedPythonLikeObject;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonNone;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.errors.AttributeError;
import ai.timefold.jpyinterpreter.types.errors.TypeError;
import ai.timefold.jpyinterpreter.types.numeric.PythonBoolean;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;

/**
 * Represents an Object that can be interacted with like a Python Object.
 * A PythonLikeObject can refer to a Java Object, a CPython Object, or
 * be an Object constructed by the Java Python Interpreter.
 */
public interface PythonLikeObject {

    /**
     * Gets an attribute by name.
     *
     * @param attributeName Name of the attribute to get
     * @return The attribute of the object that corresponds with attributeName
     * @throws AttributeError if the attribute does not exist
     */
    PythonLikeObject $getAttributeOrNull(String attributeName);

    /**
     * Gets an attribute by name.
     *
     * @param attributeName Name of the attribute to get
     * @return The attribute of the object that corresponds with attributeName
     * @throws AttributeError if the attribute does not exist
     */
    default PythonLikeObject $getAttributeOrError(String attributeName) {
        PythonLikeObject out = this.$getAttributeOrNull(attributeName);
        if (out == null) {
            throw new AttributeError("object '" + this + "' does not have attribute '" + attributeName + "'");
        }
        return out;
    }

    /**
     * Sets an attribute by name.
     *
     * @param attributeName Name of the attribute to set
     * @param value Value to set the attribute to
     */
    void $setAttribute(String attributeName, PythonLikeObject value);

    /**
     * Delete an attribute by name.
     *
     * @param attributeName Name of the attribute to delete
     */
    void $deleteAttribute(String attributeName);

    /**
     * Returns the type describing the object
     *
     * @return the type describing the object
     */
    PythonLikeType $getType();

    /**
     * Return a generic version of {@link PythonLikeObject#$getType()}. This is used in bytecode
     * generation and not at runtime. For example, for a list of integers, this return
     * list[int], while getType returns list. Both methods are needed so type([1,2,3]) is type(['a', 'b', 'c'])
     * return True.
     *
     * @return the generic version of this object's type. Must not be used in identity checks.
     */
    default PythonLikeType $getGenericType() {
        return $getType();
    }

    default PythonLikeObject $method$__getattribute__(PythonString pythonName) {
        String name = pythonName.value;
        PythonLikeObject objectResult = $getAttributeOrNull(name);
        if (objectResult != null) {
            return objectResult;
        }

        PythonLikeType type = $getType();
        PythonLikeObject typeResult = type.$getAttributeOrNull(name);
        if (typeResult != null) {
            PythonLikeObject maybeDescriptor = typeResult.$getAttributeOrNull(PythonTernaryOperator.GET.dunderMethod);
            if (maybeDescriptor == null) {
                maybeDescriptor = typeResult.$getType().$getAttributeOrNull(PythonTernaryOperator.GET.dunderMethod);
            }

            if (maybeDescriptor != null) {
                if (!(maybeDescriptor instanceof PythonLikeFunction)) {
                    throw new UnsupportedOperationException("'" + maybeDescriptor.$getType() + "' is not callable");
                }
                return TernaryDunderBuiltin.GET_DESCRIPTOR.invoke(typeResult, this, type);
            }
            return typeResult;
        }

        throw new AttributeError("object '" + this + "' does not have attribute '" + name + "'");
    }

    default PythonLikeObject $method$__setattr__(PythonString pythonName, PythonLikeObject value) {
        String name = pythonName.value;
        $setAttribute(name, value);
        return PythonNone.INSTANCE;
    }

    default PythonLikeObject $method$__delattr__(PythonString pythonName) {
        String name = pythonName.value;
        $deleteAttribute(name);
        return PythonNone.INSTANCE;
    }

    default PythonLikeObject $method$__eq__(PythonLikeObject other) {
        return PythonBoolean.valueOf(Objects.equals(this, other));
    }

    default PythonLikeObject $method$__ne__(PythonLikeObject other) {
        return PythonBoolean.valueOf(!Objects.equals(this, other));
    }

    default PythonString $method$__str__() {
        return PythonString.valueOf(this.getClass().getSimpleName() + "@" + System.identityHashCode(this));
    }

    default PythonLikeObject $method$__repr__() {
        String position;
        if (this instanceof CPythonBackedPythonLikeObject) {
            PythonInteger id = ((CPythonBackedPythonLikeObject) this).$cpythonId;
            if (id != null) {
                position = id.toString();
            } else {
                position = String.valueOf(System.identityHashCode(this));
            }
        } else {
            position = String.valueOf(System.identityHashCode(this));
        }
        return PythonString.valueOf("<" + $getType().getTypeName() + " object at " + position + ">");
    }

    default PythonLikeObject $method$__format__() {
        return $method$__format__(PythonNone.INSTANCE);
    }

    default PythonLikeObject $method$__format__(PythonLikeObject formatString) {
        return $method$__str__().$method$__format__(formatString);
    }

    default PythonLikeObject $method$__hash__() {
        throw new TypeError("unhashable type: '" + $getType().getTypeName() + "'");
    }
}
