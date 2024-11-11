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
        var methodReturnType =
                PythonClassTranslator.getVirtualFunctionReturnType(compiledClass.instanceFunctionNameToPythonBytecode
                        .get(interfaceMethod.getName()));
        String javaMethodDescriptor = Type.getMethodDescriptor(methodReturnType, javaParameterTypes);

        interfaceMethodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalClassName,
                PythonClassTranslator.getJavaMethodName(interfaceMethod.getName()),
                javaMethodDescriptor, false);

        var returnType = interfaceMethod.getReturnType();
        if (returnType.equals(void.class)) {
            interfaceMethodVisitor.visitInsn(Opcodes.RETURN);
        } else {
            if (returnType.isPrimitive()) {
                JavaPythonTypeConversionImplementor.loadTypeClass(returnType, interfaceMethodVisitor);
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
                JavaPythonTypeConversionImplementor.unboxBoxedPrimitiveType(returnType, interfaceMethodVisitor);
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
                JavaPythonTypeConversionImplementor.boxPrimitiveType(parameterType, interfaceMethodVisitor);
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

}
