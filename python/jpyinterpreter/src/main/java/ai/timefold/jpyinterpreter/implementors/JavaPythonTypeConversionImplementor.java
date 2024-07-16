package ai.timefold.jpyinterpreter.implementors;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ai.timefold.jpyinterpreter.LocalVariableHelper;
import ai.timefold.jpyinterpreter.MethodDescriptor;
import ai.timefold.jpyinterpreter.PythonClassTranslator;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.Coercible;
import ai.timefold.jpyinterpreter.types.PythonByteArray;
import ai.timefold.jpyinterpreter.types.PythonBytes;
import ai.timefold.jpyinterpreter.types.PythonCode;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.types.PythonNone;
import ai.timefold.jpyinterpreter.types.PythonString;
import ai.timefold.jpyinterpreter.types.collections.DelegatePythonIterator;
import ai.timefold.jpyinterpreter.types.collections.PythonIterator;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeDict;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeFrozenSet;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeList;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeSet;
import ai.timefold.jpyinterpreter.types.collections.PythonLikeTuple;
import ai.timefold.jpyinterpreter.types.errors.TypeError;
import ai.timefold.jpyinterpreter.types.numeric.PythonBoolean;
import ai.timefold.jpyinterpreter.types.numeric.PythonDecimal;
import ai.timefold.jpyinterpreter.types.numeric.PythonFloat;
import ai.timefold.jpyinterpreter.types.numeric.PythonInteger;
import ai.timefold.jpyinterpreter.types.numeric.PythonNumber;
import ai.timefold.jpyinterpreter.types.wrappers.JavaObjectWrapper;
import ai.timefold.jpyinterpreter.types.wrappers.OpaqueJavaReference;
import ai.timefold.jpyinterpreter.types.wrappers.OpaquePythonReference;
import ai.timefold.jpyinterpreter.types.wrappers.PythonObjectWrapper;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Implementations of opcodes and operations that require Java to Python or Python to Java conversions.
 */
public class JavaPythonTypeConversionImplementor {

    /**
     * Wraps {@code object} to a PythonLikeObject.
     */
    public static PythonLikeObject wrapJavaObject(Object object) {
        return wrapJavaObject(object, new IdentityHashMap<>());
    }

    public static PythonLikeObject wrapJavaObject(Object object, Map<Object, PythonLikeObject> createdObjectMap) {
        if (object == null) {
            return PythonNone.INSTANCE;
        }

        var existingObject = createdObjectMap.get(object);
        if (existingObject != null) {
            return existingObject;
        }

        if (object instanceof OpaqueJavaReference opaqueJavaReference) {
            return opaqueJavaReference.proxy();
        }

        if (object instanceof PythonLikeObject instance) {
            // Object already a PythonLikeObject; need to do nothing
            return instance;
        }

        if (object instanceof Byte || object instanceof Short || object instanceof Integer || object instanceof Long) {
            return PythonInteger.valueOf(((Number) object).longValue());
        }

        if (object instanceof BigInteger integer) {
            return PythonInteger.valueOf(integer);
        }

        if (object instanceof BigDecimal decimal) {
            return new PythonDecimal(decimal);
        }

        if (object instanceof Float || object instanceof Double) {
            return PythonFloat.valueOf(((Number) object).doubleValue());
        }

        if (object instanceof Boolean booleanValue) {
            return PythonBoolean.valueOf(booleanValue);
        }

        if (object instanceof String string) {
            return PythonString.valueOf(string);
        }

        if (object instanceof Iterator<?> iterator) {
            return new DelegatePythonIterator<>(iterator);
        }

        if (object instanceof List<?> list) {
            PythonLikeList<?> out = new PythonLikeList<>();
            createdObjectMap.put(object, out);
            for (Object item : list) {
                out.add(wrapJavaObject(item));
            }
            return out;
        }

        if (object instanceof Set<?> set) {
            PythonLikeSet<?> out = new PythonLikeSet<>();
            createdObjectMap.put(object, out);
            for (Object item : set) {
                out.add(wrapJavaObject(item));
            }
            return out;
        }

        if (object instanceof Map<?, ?> map) {
            PythonLikeDict<?, ?> out = new PythonLikeDict<>();
            createdObjectMap.put(object, out);
            var entrySet = map.entrySet();
            for (var entry : entrySet) {
                out.put(wrapJavaObject(entry.getKey()), wrapJavaObject(entry.getValue()));
            }
            return out;
        }

        if (object instanceof Class<?> maybeFunctionClass &&
                Set.of(maybeFunctionClass.getInterfaces()).contains(PythonLikeFunction.class)) {
            return new PythonCode((Class<? extends PythonLikeFunction>) maybeFunctionClass);
        }

        if (object instanceof OpaquePythonReference opaquePythonReference) {
            return new PythonObjectWrapper(opaquePythonReference);
        }

        // Default: return a JavaObjectWrapper
        return new JavaObjectWrapper(object, createdObjectMap);
    }

    /**
     * Get the {@link PythonLikeType} of a java {@link Class}.
     */
    public static PythonLikeType getPythonLikeType(Class<?> javaClass) {
        if (PythonNone.class.equals(javaClass)) {
            return BuiltinTypes.NONE_TYPE;
        }

        if (PythonLikeObject.class.equals(javaClass)) {
            return BuiltinTypes.BASE_TYPE;
        }

        if (byte.class.equals(javaClass) || short.class.equals(javaClass) || int.class.equals(javaClass)
                || long.class.equals(javaClass) ||
                Byte.class.equals(javaClass) || Short.class.equals(javaClass) || Integer.class.equals(javaClass)
                || Long.class.equals(javaClass) || BigInteger.class.equals(javaClass) ||
                PythonInteger.class.equals(javaClass)) {
            return BuiltinTypes.INT_TYPE;
        }

        if (BigDecimal.class.equals(javaClass) || PythonDecimal.class.equals(javaClass)) {
            return BuiltinTypes.DECIMAL_TYPE;
        }

        if (float.class.equals(javaClass) || double.class.equals(javaClass) ||
                Float.class.equals(javaClass) || Double.class.equals(javaClass) ||
                PythonFloat.class.equals(javaClass)) {
            return BuiltinTypes.FLOAT_TYPE;
        }

        if (PythonNumber.class.equals(javaClass)) {
            return BuiltinTypes.NUMBER_TYPE;
        }

        if (boolean.class.equals(javaClass) ||
                Boolean.class.equals(javaClass) ||
                PythonBoolean.class.equals(javaClass)) {
            return BuiltinTypes.BOOLEAN_TYPE;
        }

        if (String.class.equals(javaClass) ||
                PythonString.class.equals(javaClass)) {
            return BuiltinTypes.STRING_TYPE;
        }

        if (PythonBytes.class.equals(javaClass)) {
            return BuiltinTypes.BYTES_TYPE;
        }

        if (PythonByteArray.class.equals(javaClass)) {
            return BuiltinTypes.BYTE_ARRAY_TYPE;
        }

        if (Iterator.class.equals(javaClass) ||
                PythonIterator.class.equals(javaClass)) {
            return BuiltinTypes.ITERATOR_TYPE;
        }

        if (List.class.equals(javaClass) ||
                PythonLikeList.class.equals(javaClass)) {
            return BuiltinTypes.LIST_TYPE;
        }

        if (PythonLikeTuple.class.equals(javaClass)) {
            return BuiltinTypes.TUPLE_TYPE;
        }

        if (Set.class.equals(javaClass) ||
                PythonLikeSet.class.equals(javaClass)) {
            return BuiltinTypes.SET_TYPE;
        }

        if (PythonLikeFrozenSet.class.equals(javaClass)) {
            return BuiltinTypes.FROZEN_SET_TYPE;
        }

        if (Map.class.equals(javaClass) ||
                PythonLikeDict.class.equals(javaClass)) {
            return BuiltinTypes.DICT_TYPE;
        }

        if (PythonLikeType.class.equals(javaClass)) {
            return BuiltinTypes.TYPE_TYPE;
        }

        try {
            Field typeField = javaClass.getField(PythonClassTranslator.TYPE_FIELD_NAME);
            Object maybeType = typeField.get(null);
            if (maybeType instanceof PythonLikeType) {
                return (PythonLikeType) maybeType;
            }
            if (PythonLikeFunction.class.isAssignableFrom(javaClass)) {
                return PythonLikeFunction.getFunctionType();
            }
            return JavaObjectWrapper.getPythonTypeForClass(javaClass);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            if (PythonLikeFunction.class.isAssignableFrom(javaClass)) {
                return PythonLikeFunction.getFunctionType();
            }
            return JavaObjectWrapper.getPythonTypeForClass(javaClass);
        }
    }

    /**
     * Converts a {@code PythonLikeObject} to the given {@code type}.
     */
    @SuppressWarnings("unchecked")
    public static <T> T convertPythonObjectToJavaType(Class<? extends T> type, PythonLikeObject object) {
        if (object == null || type.isAssignableFrom(object.getClass())) {
            // Can directly assign; no modification needed
            return (T) object;
        }

        if (object instanceof PythonNone) {
            return null;
        }

        if (object instanceof JavaObjectWrapper wrappedObject) {
            Object javaObject = wrappedObject.getWrappedObject();
            if (!type.isAssignableFrom(javaObject.getClass())) {
                throw new TypeError("Cannot convert from (" + getPythonLikeType(javaObject.getClass()) + ") to ("
                        + getPythonLikeType(type) + ").");
            }
            return (T) javaObject;
        }

        if (type.equals(byte.class) || type.equals(short.class) || type.equals(int.class) || type.equals(long.class) ||
                type.equals(float.class) || type.equals(double.class) || Number.class.isAssignableFrom(type)) {
            if (!(object instanceof PythonNumber pythonNumber)) {
                throw new TypeError("Cannot convert from (" + getPythonLikeType(object.getClass()) + ") to ("
                        + getPythonLikeType(type) + ").");
            }
            Number value = pythonNumber.getValue();

            if (type.equals(BigInteger.class) || type.equals(BigDecimal.class)) {
                return (T) value;
            }

            if (type.equals(byte.class) || type.equals(Byte.class)) {
                return (T) (Byte) value.byteValue();
            }

            if (type.equals(short.class) || type.equals(Short.class)) {
                return (T) (Short) value.shortValue();
            }

            if (type.equals(int.class) || type.equals(Integer.class)) {
                return (T) (Integer) value.intValue();
            }

            if (type.equals(long.class) || type.equals(Long.class)) {
                return (T) (Long) value.longValue();
            }

            if (type.equals(float.class) || type.equals(Float.class)) {
                return (T) (Float) value.floatValue();
            }

            if (type.equals(double.class) || type.equals(Double.class)) {
                return (T) (Double) value.doubleValue();
            }
        }

        if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            if (!(object instanceof PythonBoolean pythonBoolean)) {
                throw new TypeError("Cannot convert from (" + getPythonLikeType(object.getClass()) + ") to ("
                        + getPythonLikeType(type) + ").");
            }
            return (T) (Boolean) pythonBoolean.getBooleanValue();
        }

        if (type.equals(String.class)) {
            PythonString pythonString = (PythonString) object;
            return (T) pythonString.getValue();
        }

        // TODO: List, Map, Set

        throw new TypeError(
                "Cannot convert from (" + getPythonLikeType(object.getClass()) + ") to (" + getPythonLikeType(type) + ").");
    }

    /**
     * Loads a String and push it onto the stack
     *
     * @param name The name to load
     */
    public static void loadName(MethodVisitor methodVisitor, String name) {
        methodVisitor.visitLdcInsn(name);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(PythonString.class),
                "valueOf",
                Type.getMethodDescriptor(Type.getType(PythonString.class), Type.getType(PythonString.class)),
                false);
    }

    private record ReturnValueOpDescriptor(
            String wrapperClassName,
            String methodName,
            String methodDescriptor,
            int opcode,
            boolean noConversionNeeded) {
        public static ReturnValueOpDescriptor noConversion() {
            return new ReturnValueOpDescriptor("", "", "",
                    Opcodes.ARETURN, true);
        }

        public static ReturnValueOpDescriptor forNumeric(String methodName,
                String methodDescriptor,
                int opcode) {
            return new ReturnValueOpDescriptor(Type.getInternalName(Number.class), methodName, methodDescriptor, opcode,
                    false);
        }
    }

    private static final Map<Type, ReturnValueOpDescriptor> numericReturnValueOpDescriptorMap = Map.of(
            Type.BYTE_TYPE, ReturnValueOpDescriptor.forNumeric(
                    "byteValue",
                    Type.getMethodDescriptor(Type.BYTE_TYPE),
                    Opcodes.IRETURN),
            Type.SHORT_TYPE, ReturnValueOpDescriptor.forNumeric(
                    "shortValue",
                    Type.getMethodDescriptor(Type.SHORT_TYPE),
                    Opcodes.IRETURN),
            Type.INT_TYPE, ReturnValueOpDescriptor.forNumeric(
                    "intValue",
                    Type.getMethodDescriptor(Type.INT_TYPE),
                    Opcodes.IRETURN),
            Type.LONG_TYPE, ReturnValueOpDescriptor.forNumeric(
                    "longValue",
                    Type.getMethodDescriptor(Type.LONG_TYPE),
                    Opcodes.LRETURN),
            Type.FLOAT_TYPE, ReturnValueOpDescriptor.forNumeric(
                    "floatValue",
                    Type.getMethodDescriptor(Type.FLOAT_TYPE),
                    Opcodes.FRETURN),
            Type.DOUBLE_TYPE, ReturnValueOpDescriptor.forNumeric(
                    "doubleValue",
                    Type.getMethodDescriptor(Type.DOUBLE_TYPE),
                    Opcodes.DRETURN),
            Type.getType(BigInteger.class), ReturnValueOpDescriptor.noConversion(),
            Type.getType(BigDecimal.class), ReturnValueOpDescriptor.noConversion());

    /**
     * If {@code method} return type is not void, convert TOS into its Java equivalent and return it.
     * If {@code method} return type is void, immediately return.
     *
     * @param method The method that is being implemented.
     */
    public static void returnValue(MethodVisitor methodVisitor, MethodDescriptor method, StackMetadata stackMetadata) {
        Type returnAsmType = method.getReturnType();

        if (Type.CHAR_TYPE.equals(returnAsmType)) {
            throw new IllegalStateException("Unhandled case for primitive type (char).");
        }

        if (Type.VOID_TYPE.equals(returnAsmType)) {
            methodVisitor.visitInsn(Opcodes.RETURN);
            return;
        }

        if (numericReturnValueOpDescriptorMap.containsKey(returnAsmType)) {
            var returnValueOpDescriptor = numericReturnValueOpDescriptorMap.get(returnAsmType);
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonNumber.class));
            methodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                    Type.getInternalName(PythonNumber.class),
                    "getValue",
                    Type.getMethodDescriptor(Type.getType(Number.class)),
                    true);

            if (returnValueOpDescriptor.noConversionNeeded) {
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, returnAsmType.getInternalName());
                methodVisitor.visitInsn(Opcodes.ARETURN);
                return;
            }

            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    returnValueOpDescriptor.wrapperClassName,
                    returnValueOpDescriptor.methodName,
                    returnValueOpDescriptor.methodDescriptor,
                    false);
            methodVisitor.visitInsn(returnValueOpDescriptor.opcode);
            return;
        }

        if (Type.BOOLEAN_TYPE.equals(returnAsmType)) {
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonBoolean.class));
            String wrapperClassName = Type.getInternalName(PythonBoolean.class);
            String methodName = "getBooleanValue";
            String methodDescriptor = Type.getMethodDescriptor(Type.BOOLEAN_TYPE);
            int returnOpcode = Opcodes.IRETURN;
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    wrapperClassName, methodName, methodDescriptor,
                    false);
            methodVisitor.visitInsn(returnOpcode);
            return;
        }

        try {
            Class<?> returnTypeClass =
                    Class.forName(returnAsmType.getClassName(), true, BuiltinTypes.asmClassLoader);

            if (stackMetadata.getTOSType() == null) {
                throw new IllegalStateException("Cannot return a deleted or undefined value");
            }
            Class<?> tosTypeClass = stackMetadata.getTOSType().getJavaClass();
            if (returnTypeClass.isAssignableFrom(tosTypeClass)) {
                methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, returnAsmType.getInternalName());
                methodVisitor.visitInsn(Opcodes.ARETURN);
                return;
            }
        } catch (ClassNotFoundException e) {
            // Do nothing; default case is below
        }

        methodVisitor.visitLdcInsn(returnAsmType);
        methodVisitor.visitInsn(Opcodes.SWAP);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(JavaPythonTypeConversionImplementor.class),
                "convertPythonObjectToJavaType",
                Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Class.class),
                        Type.getType(PythonLikeObject.class)),
                false);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, returnAsmType.getInternalName());
        methodVisitor.visitInsn(Opcodes.ARETURN);
    }

    /**
     * Coerce a value to a given type
     */
    public static <T> T coerceToType(PythonLikeObject value, Class<T> type) {
        if (value == null) {
            return null;
        }

        if (type.isAssignableFrom(value.getClass())) {
            return (T) value;
        }

        if (value instanceof Coercible coercible) {
            var out = coercible.coerce(type);
            if (out == null) {
                throw new TypeError("%s cannot be coerced to %s."
                        .formatted(value.$getType(), type));
            }
            return out;
        }

        throw new TypeError("%s cannot be coerced to %s."
                .formatted(value.$getType(), type));
    }

    /**
     * Convert the {@code parameterIndex} Java parameter to its Python equivalent and store it into
     * the corresponding Python parameter local variable slot.
     */
    public static void copyParameter(MethodVisitor methodVisitor, LocalVariableHelper localVariableHelper,
            int parameterIndex) {
        Type parameterType = localVariableHelper.parameters[parameterIndex];
        if (parameterType.getSort() != Type.OBJECT && parameterType.getSort() != Type.ARRAY) {
            int loadOpcode;
            String valueOfOwner;
            String valueOfDescriptor;

            if (Type.BOOLEAN_TYPE.equals(parameterType)) {
                loadOpcode = Opcodes.ILOAD;
                valueOfOwner = Type.getInternalName(PythonBoolean.class);
                valueOfDescriptor = Type.getMethodDescriptor(Type.getType(PythonBoolean.class), Type.getType(boolean.class));
            } else if (Type.CHAR_TYPE.equals(parameterType)) {
                loadOpcode = Opcodes.ILOAD;
                throw new IllegalStateException("Unhandled case for primitive type (" + parameterType + ").");
            } else if (Type.BYTE_TYPE.equals(parameterType)) {
                loadOpcode = Opcodes.ILOAD;
                valueOfOwner = Type.getInternalName(PythonInteger.class);
                valueOfDescriptor = Type.getMethodDescriptor(Type.getType(PythonInteger.class), Type.getType(byte.class));
            } else if (Type.SHORT_TYPE.equals(parameterType)) {
                loadOpcode = Opcodes.ILOAD;
                valueOfOwner = Type.getInternalName(PythonInteger.class);
                valueOfDescriptor = Type.getMethodDescriptor(Type.getType(PythonInteger.class), Type.getType(short.class));
            } else if (Type.INT_TYPE.equals(parameterType)) {
                loadOpcode = Opcodes.ILOAD;
                valueOfOwner = Type.getInternalName(PythonInteger.class);
                valueOfDescriptor = Type.getMethodDescriptor(Type.getType(PythonInteger.class), Type.getType(int.class));
            } else if (Type.FLOAT_TYPE.equals(parameterType)) {
                loadOpcode = Opcodes.FLOAD;
                valueOfOwner = Type.getInternalName(PythonFloat.class);
                valueOfDescriptor = Type.getMethodDescriptor(Type.getType(PythonFloat.class), Type.getType(float.class));
            } else if (Type.LONG_TYPE.equals(parameterType)) {
                loadOpcode = Opcodes.LLOAD;
                valueOfOwner = Type.getInternalName(PythonInteger.class);
                valueOfDescriptor = Type.getMethodDescriptor(Type.getType(PythonInteger.class), Type.getType(long.class));
            } else if (Type.DOUBLE_TYPE.equals(parameterType)) {
                loadOpcode = Opcodes.DLOAD;
                valueOfOwner = Type.getInternalName(PythonFloat.class);
                valueOfDescriptor = Type.getMethodDescriptor(Type.getType(PythonFloat.class), Type.getType(double.class));
            } else {
                throw new IllegalStateException("Unhandled case for primitive type (" + parameterType + ").");
            }

            methodVisitor.visitVarInsn(loadOpcode, localVariableHelper.getParameterSlot(parameterIndex));
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, valueOfOwner, "valueOf",
                    valueOfDescriptor, false);
            localVariableHelper.writeLocal(methodVisitor, parameterIndex);
        } else {
            try {
                Class<?> typeClass = Class.forName(parameterType.getClassName(), false,
                        BuiltinTypes.asmClassLoader);
                if (!PythonLikeObject.class.isAssignableFrom(typeClass)) {
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, localVariableHelper.getParameterSlot(parameterIndex));
                    methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                            Type.getInternalName(JavaPythonTypeConversionImplementor.class),
                            "wrapJavaObject",
                            Type.getMethodDescriptor(Type.getType(PythonLikeObject.class), Type.getType(Object.class)),
                            false);
                    localVariableHelper.writeLocal(methodVisitor, parameterIndex);
                } else {
                    methodVisitor.visitVarInsn(Opcodes.ALOAD, localVariableHelper.getParameterSlot(parameterIndex));
                    localVariableHelper.writeLocal(methodVisitor, parameterIndex);
                }
            } catch (ClassNotFoundException e) {
                methodVisitor.visitVarInsn(Opcodes.ALOAD, localVariableHelper.getParameterSlot(parameterIndex));
                localVariableHelper.writeLocal(methodVisitor, parameterIndex);
            }
        }
    }
}
