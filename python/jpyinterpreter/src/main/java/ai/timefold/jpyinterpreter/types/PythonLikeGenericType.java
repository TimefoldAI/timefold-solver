package ai.timefold.jpyinterpreter.types;

import java.util.Optional;

import ai.timefold.jpyinterpreter.FieldDescriptor;
import ai.timefold.jpyinterpreter.PythonClassTranslator;

public class PythonLikeGenericType extends PythonLikeType {
    final PythonLikeType origin;

    public PythonLikeGenericType(PythonLikeType origin) {
        super(BuiltinTypes.TYPE_TYPE.getTypeName(), PythonLikeType.class);
        this.origin = origin;
    }

    public PythonLikeType getOrigin() {
        return origin;
    }

    @Override
    public Optional<FieldDescriptor> getInstanceFieldDescriptor(String fieldName) {
        Optional<PythonKnownFunctionType> maybeMethodType = getMethodType(fieldName);
        if (maybeMethodType.isEmpty()) {
            return BuiltinTypes.TYPE_TYPE.getInstanceFieldDescriptor(fieldName);
        } else {
            PythonKnownFunctionType knownFunctionType = maybeMethodType.get();
            FieldDescriptor out = new FieldDescriptor(fieldName,
                    PythonClassTranslator.getJavaMethodName(fieldName),
                    origin.getJavaTypeInternalName(),
                    knownFunctionType.getJavaTypeDescriptor(),
                    knownFunctionType, false, false);
            return Optional.of(out);
        }
    }

    @Override
    public Optional<PythonKnownFunctionType> getMethodType(String methodName) {
        Optional<PythonKnownFunctionType> originKnownFunctionType = origin.getMethodType(methodName);
        if (originKnownFunctionType.isEmpty()) {
            return originKnownFunctionType;
        }

        PythonKnownFunctionType knownFunctionType = originKnownFunctionType.get();
        if (knownFunctionType.isStaticMethod() || knownFunctionType.isClassMethod()) {
            return originKnownFunctionType;
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<PythonClassTranslator.PythonMethodKind> getMethodKind(String methodName) {
        Optional<PythonClassTranslator.PythonMethodKind> originMethodKind = origin.getMethodKind(methodName);
        if (originMethodKind.isEmpty()) {
            return originMethodKind;
        }

        switch (originMethodKind.get()) {
            case STATIC_METHOD:
            case CLASS_METHOD:
                return originMethodKind;
            default:
                return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return "<class type[" + origin.getTypeName() + "]>";
    }
}
