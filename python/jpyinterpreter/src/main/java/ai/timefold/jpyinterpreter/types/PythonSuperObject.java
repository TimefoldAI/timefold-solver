package ai.timefold.jpyinterpreter.types;

import java.util.List;
import java.util.Map;

import ai.timefold.jpyinterpreter.PythonClassTranslator;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.PythonTernaryOperator;
import ai.timefold.jpyinterpreter.builtins.TernaryDunderBuiltin;
import ai.timefold.jpyinterpreter.types.errors.AttributeError;

public class PythonSuperObject extends AbstractPythonLikeObject {
    public static final PythonLikeType $TYPE = BuiltinTypes.SUPER_TYPE;

    public final PythonLikeType previousType;
    public final PythonLikeObject instance;

    public PythonSuperObject(PythonLikeType previousType) {
        super(BuiltinTypes.SUPER_TYPE);
        this.previousType = previousType;
        this.instance = null;
    }

    public PythonSuperObject(PythonLikeType previousType, PythonLikeObject instance) {
        super(BuiltinTypes.SUPER_TYPE);
        this.previousType = previousType;
        this.instance = instance;
    }

    @Override
    public PythonLikeObject $method$__getattribute__(PythonString pythonName) {
        List<PythonLikeType> mro;
        if (instance != null) {
            if (instance instanceof PythonLikeType) {
                mro = ((PythonLikeType) instance).MRO;
            } else {
                mro = instance.$getType().MRO;
            }
        } else {
            mro = previousType.MRO;
        }

        String name = pythonName.value;
        for (int currentIndex = mro.indexOf(previousType) + 1; currentIndex < mro.size(); currentIndex++) {
            PythonLikeType candidate = mro.get(currentIndex);

            PythonLikeObject typeResult = candidate.$getAttributeOrNull(name);
            if (typeResult != null) {

                if (typeResult instanceof PythonLikeFunction && !(typeResult instanceof PythonLikeType)) {
                    try {
                        Object methodInstance =
                                candidate.getJavaClass().getField(PythonClassTranslator.getJavaMethodHolderName(name))
                                        .get(null);
                        typeResult = new GeneratedFunctionMethodReference(methodInstance,
                                methodInstance.getClass().getDeclaredMethods()[0],
                                Map.of(),
                                typeResult.$getType());
                    } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
                        // ignore
                    }
                }

                PythonLikeObject maybeDescriptor = typeResult.$getAttributeOrNull(PythonTernaryOperator.GET.dunderMethod);
                if (maybeDescriptor == null) {
                    maybeDescriptor = typeResult.$getType().$getAttributeOrNull(PythonTernaryOperator.GET.dunderMethod);
                }

                if (maybeDescriptor != null) {
                    if (!(maybeDescriptor instanceof PythonLikeFunction)) {
                        throw new UnsupportedOperationException("'" + maybeDescriptor.$getType() + "' is not callable");
                    }
                    return TernaryDunderBuiltin.GET_DESCRIPTOR.invoke(typeResult,
                            (instance != null) ? instance : PythonNone.INSTANCE,
                            candidate);
                }
                return typeResult;
            }
        }
        throw new AttributeError("Cannot find attribute " + pythonName.repr() + " in super(" + previousType + ").");
    }

}