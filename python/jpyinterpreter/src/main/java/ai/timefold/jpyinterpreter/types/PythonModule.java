package ai.timefold.jpyinterpreter.types;

import java.util.Map;

import ai.timefold.jpyinterpreter.CPythonBackedPythonInterpreter;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.wrappers.OpaquePythonReference;

public class PythonModule extends AbstractPythonLikeObject {
    public static PythonLikeType MODULE_TYPE = BuiltinTypes.MODULE_TYPE;
    public static PythonLikeType $TYPE = MODULE_TYPE;

    private OpaquePythonReference pythonReference;
    private Map<Number, PythonLikeObject> referenceMap;

    public PythonModule(Map<Number, PythonLikeObject> referenceMap) {
        super(MODULE_TYPE);
        this.referenceMap = referenceMap;
    }

    public void addItem(String itemName, PythonLikeObject itemValue) {
        $setAttribute(itemName, itemValue);
    }

    public OpaquePythonReference getPythonReference() {
        return pythonReference;
    }

    public void setPythonReference(OpaquePythonReference pythonReference) {
        this.pythonReference = pythonReference;
    }

    @Override
    public PythonLikeObject $getAttributeOrNull(String attributeName) {
        PythonLikeObject result = super.$getAttributeOrNull(attributeName);
        if (result == null) {
            PythonLikeObject actual = CPythonBackedPythonInterpreter.lookupAttributeOnPythonReference(pythonReference,
                    attributeName, referenceMap);
            $setAttribute(attributeName, actual);
            return actual;
        }
        return result;
    }
}
