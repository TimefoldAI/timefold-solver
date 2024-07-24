package ai.timefold.jpyinterpreter.implementors;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import ai.timefold.jpyinterpreter.PythonBytecodeToJavaBytecodeTranslator;
import ai.timefold.jpyinterpreter.PythonClassTranslator;
import ai.timefold.jpyinterpreter.PythonCompiledClass;
import ai.timefold.jpyinterpreter.PythonLikeObject;
import ai.timefold.jpyinterpreter.types.errors.TypeError;
import ai.timefold.jpyinterpreter.util.MethodVisitorAdapters;
import ai.timefold.jpyinterpreter.util.arguments.ArgumentSpec;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class DelegatingInterfaceImplementor extends JavaInterfaceImplementor {
    final String internalClassName;
    final Class<?> interfaceClass;
    final Map<String, PythonClassTranslator.InterfaceDeclaration> methodNameToFieldDescriptor;

    public DelegatingInterfaceImplementor(String internalClassName, Class<?> interfaceClass,
            Map<String, PythonClassTranslator.InterfaceDeclaration> methodNameToFieldDescriptor) {
        this.internalClassName = internalClassName;
        this.interfaceClass = interfaceClass;
        this.methodNameToFieldDescriptor = methodNameToFieldDescriptor;
    }

    @Override
    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }

    @Override
    public void implement(ClassWriter classWriter, PythonCompiledClass compiledClass) {
        for (Method method : interfaceClass.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) && method.getDeclaringClass().isInterface()) {
                implementMethod(classWriter, compiledClass, method);
            }
        }
    }

    private void implementMethod(ClassWriter classWriter, PythonCompiledClass compiledClass, Method interfaceMethod) {
        if (!methodNameToFieldDescriptor.containsKey(interfaceMethod.getName())) {
            if (interfaceMethod.isDefault()) {
                return;
            } else {
                throw new TypeError("Class %s cannot implement interface %s because it does not implement method %s."
                        .formatted(compiledClass.className, interfaceMethod.getDeclaringClass().getName(),
                                interfaceMethod.getName()));
            }
        }
        var interfaceMethodDescriptor = Type.getMethodDescriptor(interfaceMethod);

        // Generates interfaceMethod(A a, B b, ...) { return delegate.interfaceMethod(a, b, ...); }
        var interfaceMethodVisitor = classWriter.visitMethod(Modifier.PUBLIC, interfaceMethod.getName(),
                interfaceMethodDescriptor, null, null);

        interfaceMethodVisitor =
                MethodVisitorAdapters.adapt(interfaceMethodVisitor, interfaceMethod.getName(), interfaceMethodDescriptor);

        for (var parameter : interfaceMethod.getParameters()) {
            interfaceMethodVisitor.visitParameter(parameter.getName(), 0);
        }
        interfaceMethodVisitor.visitCode();
        interfaceMethodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        interfaceMethodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(IdentityHashMap.class));
        interfaceMethodVisitor.visitInsn(Opcodes.DUP);
        interfaceMethodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(IdentityHashMap.class),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);
        interfaceMethodVisitor.visitVarInsn(Opcodes.ASTORE, interfaceMethod.getParameterCount() + 1);

        // Generates TOS = MyClass.methodInstance.getClass().getField(ARGUMENT_SPEC_INSTANCE_FIELD).get(MyClass.methodInstance);
        var functionInterfaceDeclaration = methodNameToFieldDescriptor.get(interfaceMethod.getName());
        interfaceMethodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        interfaceMethodVisitor.visitFieldInsn(Opcodes.GETSTATIC, internalClassName,
                PythonClassTranslator.getJavaMethodHolderName(interfaceMethod.getName()),
                functionInterfaceDeclaration.descriptor());
        interfaceMethodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Object.class),
                "getClass", Type.getMethodDescriptor(Type.getType(Class.class)), false);
        interfaceMethodVisitor.visitLdcInsn(PythonBytecodeToJavaBytecodeTranslator.ARGUMENT_SPEC_INSTANCE_FIELD_NAME);
        interfaceMethodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Class.class),
                "getField", Type.getMethodDescriptor(Type.getType(Field.class), Type.getType(String.class)), false);
        interfaceMethodVisitor.visitFieldInsn(Opcodes.GETSTATIC, internalClassName,
                PythonClassTranslator.getJavaMethodHolderName(interfaceMethod.getName()),
                functionInterfaceDeclaration.descriptor());
        interfaceMethodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Field.class),
                "get", Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Object.class)), false);
        interfaceMethodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(ArgumentSpec.class));
        var methodType = functionInterfaceDeclaration.methodType();
        int argumentCount = methodType.getArgumentCount();

        prepareParametersForMethodCallFromArgumentSpec(interfaceMethod, interfaceMethodVisitor, argumentCount, methodType,
                true);

        Type[] javaParameterTypes = new Type[Math.max(0, argumentCount - 1)];

        for (int i = 1; i < argumentCount; i++) {
            javaParameterTypes[i - 1] = methodType.getArgumentTypes()[i];
        }
        String javaMethodDescriptor = Type.getMethodDescriptor(methodType.getReturnType(), javaParameterTypes);

        interfaceMethodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalClassName,
                PythonClassTranslator.getJavaMethodName(interfaceMethod.getName()),
                javaMethodDescriptor, false);

        var returnType = interfaceMethod.getReturnType();
        if (returnType.equals(void.class)) {
            interfaceMethodVisitor.visitInsn(Opcodes.RETURN);
        } else {
            if (returnType.isPrimitive()) {
                loadBoxedPrimitiveTypeClass(returnType, interfaceMethodVisitor);
            } else {
                interfaceMethodVisitor.visitLdcInsn(Type.getType(returnType));
            }
            interfaceMethodVisitor.visitInsn(Opcodes.SWAP);
            interfaceMethodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                    Type.getInternalName(JavaPythonTypeConversionImplementor.class),
                    "convertPythonObjectToJavaType",
                    Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Class.class), Type.getType(
                            PythonLikeObject.class)),
                    false);
            if (returnType.isPrimitive()) {
                unboxBoxedPrimitiveType(returnType, interfaceMethodVisitor);
                interfaceMethodVisitor.visitInsn(Type.getType(returnType).getOpcode(Opcodes.IRETURN));
            } else {
                interfaceMethodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(returnType));
                interfaceMethodVisitor.visitInsn(Opcodes.ARETURN);
            }
        }
        interfaceMethodVisitor.visitMaxs(interfaceMethod.getParameterCount() + 2, 1);
        interfaceMethodVisitor.visitEnd();
    }

    public static void prepareParametersForMethodCallFromArgumentSpec(Method interfaceMethod,
            MethodVisitor interfaceMethodVisitor, int argumentCount,
            Type methodType, boolean skipSelf) {
        int parameterStart = skipSelf ? 1 : 0;
        interfaceMethodVisitor.visitLdcInsn(interfaceMethod.getParameterCount());
        interfaceMethodVisitor.visitTypeInsn(Opcodes.ANEWARRAY, Type.getInternalName(PythonLikeObject.class));
        interfaceMethodVisitor.visitVarInsn(Opcodes.ASTORE, interfaceMethod.getParameterCount() + 2);
        for (int i = 0; i < interfaceMethod.getParameterCount(); i++) {
            var parameterType = interfaceMethod.getParameterTypes()[i];
            interfaceMethodVisitor.visitVarInsn(Opcodes.ALOAD, interfaceMethod.getParameterCount() + 2);
            interfaceMethodVisitor.visitLdcInsn(i);
            interfaceMethodVisitor.visitVarInsn(Type.getType(parameterType).getOpcode(Opcodes.ILOAD),
                    i + 1);
            if (parameterType.isPrimitive()) {
                convertPrimitiveToObjectType(parameterType, interfaceMethodVisitor);
            }
            interfaceMethodVisitor.visitVarInsn(Opcodes.ALOAD, interfaceMethod.getParameterCount() + 1);
            interfaceMethodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC,
                    Type.getInternalName(JavaPythonTypeConversionImplementor.class),
                    "wrapJavaObject",
                    Type.getMethodDescriptor(Type.getType(PythonLikeObject.class), Type.getType(Object.class), Type.getType(
                            Map.class)),
                    false);
            interfaceMethodVisitor.visitInsn(Opcodes.AASTORE);
        }

        interfaceMethodVisitor.visitVarInsn(Opcodes.ALOAD, interfaceMethod.getParameterCount() + 2);
        interfaceMethodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(List.class),
                "of", Type.getMethodDescriptor(Type.getType(List.class), Type.getType(Object[].class)),
                true);
        interfaceMethodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Collections.class),
                "emptyMap", Type.getMethodDescriptor(Type.getType(Map.class)), false);
        interfaceMethodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(ArgumentSpec.class),
                "extractArgumentList", Type.getMethodDescriptor(
                        Type.getType(List.class), Type.getType(List.class), Type.getType(Map.class)),
                false);

        for (int i = 0; i < argumentCount - parameterStart; i++) {
            interfaceMethodVisitor.visitInsn(Opcodes.DUP);
            interfaceMethodVisitor.visitLdcInsn(i);
            interfaceMethodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class),
                    "get", Type.getMethodDescriptor(Type.getType(Object.class), Type.INT_TYPE), true);
            interfaceMethodVisitor.visitTypeInsn(Opcodes.CHECKCAST,
                    methodType.getArgumentTypes()[i + parameterStart].getInternalName());
            interfaceMethodVisitor.visitInsn(Opcodes.SWAP);
        }
        interfaceMethodVisitor.visitInsn(Opcodes.POP);
    }

    public static void convertPrimitiveToObjectType(Class<?> primitiveType, MethodVisitor methodVisitor) {
        if (primitiveType.equals(boolean.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Boolean.class),
                    "valueOf", Type.getMethodDescriptor(Type.getType(Boolean.class), Type.BOOLEAN_TYPE), false);
        } else if (primitiveType.equals(byte.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Byte.class),
                    "valueOf", Type.getMethodDescriptor(Type.getType(Byte.class), Type.BYTE_TYPE), false);
        } else if (primitiveType.equals(char.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Character.class),
                    "valueOf", Type.getMethodDescriptor(Type.getType(Character.class), Type.CHAR_TYPE), false);
        } else if (primitiveType.equals(short.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Short.class),
                    "valueOf", Type.getMethodDescriptor(Type.getType(Short.class), Type.SHORT_TYPE), false);
        } else if (primitiveType.equals(int.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Integer.class),
                    "valueOf", Type.getMethodDescriptor(Type.getType(Integer.class), Type.INT_TYPE), false);
        } else if (primitiveType.equals(long.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Long.class),
                    "valueOf", Type.getMethodDescriptor(Type.getType(Long.class), Type.LONG_TYPE), false);
        } else if (primitiveType.equals(float.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Float.class),
                    "valueOf", Type.getMethodDescriptor(Type.getType(Float.class), Type.FLOAT_TYPE), false);
        } else if (primitiveType.equals(double.class)) {
            methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(Double.class),
                    "valueOf", Type.getMethodDescriptor(Type.getType(Double.class), Type.DOUBLE_TYPE), false);
        } else {
            throw new IllegalStateException("Unknown primitive type %s.".formatted(primitiveType));
        }
    }

    public static void loadBoxedPrimitiveTypeClass(Class<?> primitiveType, MethodVisitor methodVisitor) {
        if (primitiveType.equals(boolean.class)) {
            methodVisitor.visitLdcInsn(Type.getType(Boolean.class));
        } else if (primitiveType.equals(byte.class)) {
            methodVisitor.visitLdcInsn(Type.getType(Byte.class));
        } else if (primitiveType.equals(char.class)) {
            methodVisitor.visitLdcInsn(Type.getType(Character.class));
        } else if (primitiveType.equals(short.class)) {
            methodVisitor.visitLdcInsn(Type.getType(Short.class));
        } else if (primitiveType.equals(int.class)) {
            methodVisitor.visitLdcInsn(Type.getType(Integer.class));
        } else if (primitiveType.equals(long.class)) {
            methodVisitor.visitLdcInsn(Type.getType(Long.class));
        } else if (primitiveType.equals(float.class)) {
            methodVisitor.visitLdcInsn(Type.getType(Float.class));
        } else if (primitiveType.equals(double.class)) {
            methodVisitor.visitLdcInsn(Type.getType(Double.class));
        } else {
            throw new IllegalStateException("Unknown primitive type %s.".formatted(primitiveType));
        }
    }

    public static void unboxBoxedPrimitiveType(Class<?> primitiveType, MethodVisitor methodVisitor) {
        if (primitiveType.equals(boolean.class)) {
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Boolean.class));
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Boolean.class),
                    "booleanValue", Type.getMethodDescriptor(Type.BOOLEAN_TYPE), false);
        } else if (primitiveType.equals(byte.class)) {
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Byte.class));
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Byte.class),
                    "byteValue", Type.getMethodDescriptor(Type.BYTE_TYPE), false);
        } else if (primitiveType.equals(char.class)) {
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Character.class));
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Character.class),
                    "charValue", Type.getMethodDescriptor(Type.CHAR_TYPE), false);
        } else if (primitiveType.equals(short.class)) {
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Short.class));
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Short.class),
                    "shortValue", Type.getMethodDescriptor(Type.SHORT_TYPE), false);
        } else if (primitiveType.equals(int.class)) {
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Integer.class));
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Integer.class),
                    "intValue", Type.getMethodDescriptor(Type.INT_TYPE), false);
        } else if (primitiveType.equals(long.class)) {
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Long.class));
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Long.class),
                    "longValue", Type.getMethodDescriptor(Type.LONG_TYPE), false);
        } else if (primitiveType.equals(float.class)) {
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Float.class));
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Float.class),
                    "floatValue", Type.getMethodDescriptor(Type.FLOAT_TYPE), false);
        } else if (primitiveType.equals(double.class)) {
            methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(Double.class));
            methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Double.class),
                    "doubleValue", Type.getMethodDescriptor(Type.DOUBLE_TYPE), false);
        } else {
            throw new IllegalStateException("Unknown primitive type %s.".formatted(primitiveType));
        }
    }
}
