package ai.timefold.jpyinterpreter.types;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.jpyinterpreter.CPythonBackedPythonInterpreter;
import ai.timefold.jpyinterpreter.PythonInterpreter;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.jpyinterpreter.types.wrappers.OpaquePythonReference;

public class CPythonBackedPythonLikeObject extends AbstractPythonLikeObject implements PythonLikeFunction {
    public static final PythonLikeType CPYTHON_BACKED_OBJECT_TYPE =
            new PythonLikeType("object", CPythonBackedPythonLikeObject.class);

    private final PythonInterpreter interpreter;

    public OpaquePythonReference $cpythonReference;

    public PythonInteger $cpythonId;

    public Map<Number, PythonLikeObject> $instanceMap;

    public CPythonBackedPythonLikeObject(PythonInterpreter interpreter,
            PythonLikeType __type__) {
        this(interpreter, __type__, (OpaquePythonReference) null);
    }

    public CPythonBackedPythonLikeObject(PythonInterpreter interpreter,
            PythonLikeType __type__, Map<String, PythonLikeObject> __dir__) {
        this(interpreter, __type__, __dir__, null);
    }

    public CPythonBackedPythonLikeObject(PythonInterpreter interpreter,
            PythonLikeType __type__,
            OpaquePythonReference reference) {
        super(__type__);
        this.interpreter = interpreter;
        this.$cpythonReference = reference;
        $instanceMap = new HashMap<>();
    }

    public CPythonBackedPythonLikeObject(PythonInterpreter interpreter,
            PythonLikeType __type__,
            Map<String, PythonLikeObject> __dir__,
            OpaquePythonReference reference) {
        super(__type__, __dir__);
        this.interpreter = interpreter;
        this.$cpythonReference = reference;
        $instanceMap = new HashMap<>();
    }

    public OpaquePythonReference $getCPythonReference() {
        return $cpythonReference;
    }

    public void $setCPythonReference(OpaquePythonReference pythonReference) {
        interpreter.setPythonReference(this, pythonReference);
    }

    public PythonInteger $getCPythonId() {
        return $cpythonId;
    }

    public Map<Number, PythonLikeObject> $getInstanceMap() {
        return $instanceMap;
    }

    public void $setInstanceMap(Map<Number, PythonLikeObject> $instanceMap) {
        this.$instanceMap = $instanceMap;
    }

    public boolean $shouldCreateNewInstance() {
        return !interpreter.hasValidPythonReference(this);
    }

    public void $readFieldsFromCPythonReference() {
    }

    public void $writeFieldsToCPythonReference(OpaquePythonReference cloneMap) {
        for (var attributeEntry : getExtraAttributeMap().entrySet()) {
            CPythonBackedPythonInterpreter.setAttributeOnPythonReference($cpythonReference, cloneMap, attributeEntry.getKey(),
                    attributeEntry.getValue());
        }
    }

    @Override
    public PythonLikeObject $call(List<PythonLikeObject> positionalArguments,
            Map<PythonString, PythonLikeObject> namedArguments, PythonLikeObject callerInstance) {
        return PythonNone.INSTANCE;
    }
}
