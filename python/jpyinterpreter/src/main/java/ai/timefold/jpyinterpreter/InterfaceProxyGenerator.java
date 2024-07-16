package ai.timefold.jpyinterpreter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Set;

import ai.timefold.jpyinterpreter.implementors.DelegatingInterfaceImplementor;
import ai.timefold.jpyinterpreter.implementors.JavaPythonTypeConversionImplementor;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonLikeType;
import ai.timefold.jpyinterpreter.util.MethodVisitorAdapters;
import ai.timefold.jpyinterpreter.util.arguments.ArgumentSpec;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class InterfaceProxyGenerator {
    /**
     * Generate an interface that just calls an existing instance of the interface.
     * Needed so Java libraries that construct new instances using the no-args
     * constructor use the correct instance of the function (the one with __closure__
     * and other instance fields).
     */
    public static <T> Class<T> generateProxyForFunction(Class<T> interfaceClass, T interfaceInstance) {
        String maybeClassName = interfaceInstance.getClass().getCanonicalName() + "$Proxy";
        int numberOfInstances =
                PythonBytecodeToJavaBytecodeTranslator.classNameToSharedInstanceCount.merge(maybeClassName, 1, Integer::sum);
        if (numberOfInstances > 1) {
            maybeClassName = maybeClassName + "$$" + numberOfInstances;
        }
        String className = maybeClassName;
        String internalClassName = className.replace('.', '/');

        var classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        classWriter.visit(Opcodes.V11, Modifier.PUBLIC, internalClassName, null,
                Type.getInternalName(Object.class), new String[] { Type.getInternalName(interfaceClass) });

        classWriter.visitField(Modifier.PUBLIC | Modifier.STATIC, "proxy",
                Type.getDescriptor(interfaceClass), null, null);

        var constructor = classWriter.visitMethod(Modifier.PUBLIC, "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE), null, null);

        // Generates Proxy() {}
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Object.class),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();

        var interfaceMethod = PythonBytecodeToJavaBytecodeTranslator.getFunctionalInterfaceMethod(interfaceClass);
        var interfaceMethodDescriptor = Type.getMethodDescriptor(interfaceMethod);

        // Generates interfaceMethod(A a, B b, ...) { return proxy.interfaceMethod(a, b, ...); }
        var interfaceMethodVisitor = classWriter.visitMethod(Modifier.PUBLIC, interfaceMethod.getName(),
                interfaceMethodDescriptor, null, null);

        for (var parameter : interfaceMethod.getParameters()) {
            interfaceMethodVisitor.visitParameter(parameter.getName(), 0);
        }
        interfaceMethodVisitor.visitCode();
        interfaceMethodVisitor.visitFieldInsn(Opcodes.GETSTATIC, internalClassName, "proxy",
                Type.getDescriptor(interfaceClass));
        for (int i = 0; i < interfaceMethod.getParameterCount(); i++) {
            interfaceMethodVisitor.visitVarInsn(Type.getType(interfaceMethod.getParameterTypes()[i]).getOpcode(Opcodes.ILOAD),
                    i + 1);
        }
        interfaceMethodVisitor.visitMethodInsn(Opcodes.INVOKEINTERFACE, Type.getInternalName(interfaceClass),
                interfaceMethod.getName(), interfaceMethodDescriptor, true);
        interfaceMethodVisitor.visitInsn(Type.getType(interfaceMethod.getReturnType()).getOpcode(Opcodes.IRETURN));
        interfaceMethodVisitor.visitMaxs(0, 0);
        interfaceMethodVisitor.visitEnd();

        classWriter.visitEnd();

        PythonBytecodeToJavaBytecodeTranslator.writeClassOutput(BuiltinTypes.classNameToBytecode, className,
                classWriter.toByteArray());

        try {
            Class<T> compiledClass = (Class<T>) BuiltinTypes.asmClassLoader.loadClass(className);
            compiledClass.getField("proxy").set(null, interfaceInstance);
            return compiledClass;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(("Impossible State: Unable to load generated class (%s)" +
                    " despite it being just generated.").formatted(className), e);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(("Impossible State: Unable to access field on generated class (%s).")
                    .formatted(className), e);
        }
    }

    /**
     * Generate an interface that construct a new instance of a type and delegate all calls to that type's methods.
     */
    public static <T> Class<T> generateProxyForClass(Class<T> interfaceClass, PythonLikeType delegateType) {
        String maybeClassName = delegateType.getClass().getCanonicalName() + "$" + interfaceClass.getSimpleName() + "$Proxy";
        int numberOfInstances =
                PythonBytecodeToJavaBytecodeTranslator.classNameToSharedInstanceCount.merge(maybeClassName, 1, Integer::sum);
        if (numberOfInstances > 1) {
            maybeClassName = maybeClassName + "$$" + numberOfInstances;
        }
        String className = maybeClassName;
        String internalClassName = className.replace('.', '/');

        var classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);

        classWriter.visit(Opcodes.V11, Modifier.PUBLIC, internalClassName, null,
                Type.getInternalName(Object.class), new String[] { Type.getInternalName(interfaceClass) });

        classWriter.visitField(Modifier.PRIVATE | Modifier.FINAL, "delegate",
                delegateType.getJavaTypeDescriptor(), null, null);

        var createdNameSet = new HashSet<String>();
        for (var interfaceMethod : interfaceClass.getMethods()) {
            addArgumentSpecFieldForMethod(classWriter, delegateType, interfaceMethod, createdNameSet);
        }

        var constructor = classWriter.visitMethod(Modifier.PUBLIC, "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE), null, null);

        // Generates Proxy() {
        //     delegate = new Delegate();
        // }
        constructor.visitCode();
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Object.class),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitTypeInsn(Opcodes.NEW, delegateType.getJavaTypeInternalName());
        constructor.visitInsn(Opcodes.DUP);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, delegateType.getJavaTypeInternalName(), "<init>",
                Type.getMethodDescriptor(Type.VOID_TYPE), false);
        constructor.visitFieldInsn(Opcodes.PUTFIELD, internalClassName, "delegate", delegateType.getJavaTypeDescriptor());
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(0, 0);
        constructor.visitEnd();

        for (var interfaceMethod : interfaceClass.getMethods()) {
            createMethodDelegate(classWriter, internalClassName, delegateType, interfaceMethod);
        }

        classWriter.visitEnd();

        PythonBytecodeToJavaBytecodeTranslator.writeClassOutput(BuiltinTypes.classNameToBytecode, className,
                classWriter.toByteArray());

        try {
            Class<T> compiledClass = (Class<T>) BuiltinTypes.asmClassLoader.loadClass(className);
            for (var interfaceMethod : interfaceClass.getMethods()) {
                if (!interfaceMethod.getDeclaringClass().isInterface()) {
                    continue;
                }
                if (interfaceMethod.isDefault()) {
                    // Default method, does not need to be present
                    var methodType = delegateType.getMethodType(interfaceMethod.getName());
                    if (methodType.isEmpty()) {
                        continue;
                    }
                    var function = methodType.get().getDefaultFunctionSignature();
                    if (function.isEmpty()) {
                        continue;
                    }
                    compiledClass.getField("argumentSpec$" + interfaceMethod.getName()).set(null,
                            function.get().getArgumentSpec());
                } else {
                    compiledClass.getField("argumentSpec$" + interfaceMethod.getName()).set(null,
                            delegateType.getMethodType(interfaceMethod.getName())
                                    .orElseThrow()
                                    .getDefaultFunctionSignature()
                                    .orElseThrow()
                                    .getArgumentSpec());
                }
            }
            return compiledClass;
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalStateException(("Impossible State: Unable to load generated class (%s)" +
                    " despite it being just generated.").formatted(className), e);
        }
    }

    private static void addArgumentSpecFieldForMethod(ClassWriter classWriter,
            PythonLikeType delegateType, Method interfaceMethod, Set<String> createdNameSet) {
        if (createdNameSet.contains(interfaceMethod.getName()) || !interfaceMethod.getDeclaringClass().isInterface()) {
            return;
        }
        var methodType = delegateType.getMethodType(interfaceMethod.getName());
        if (methodType.isEmpty()) {
            if (interfaceMethod.isDefault()) {
                return;
            }
            throw new IllegalArgumentException("Type %s cannot implement interface %s because it missing method %s."
                    .formatted(delegateType, interfaceMethod.getDeclaringClass(), interfaceMethod));
        }
        var function = methodType.get().getDefaultFunctionSignature();
        if (function.isEmpty()) {
            throw new IllegalStateException();
        }
        classWriter.visitField(Modifier.PUBLIC | Modifier.STATIC, "argumentSpec$" + interfaceMethod.getName(),
                Type.getDescriptor(ArgumentSpec.class), null, null);
        createdNameSet.add(interfaceMethod.getName());
    }

    private static void createMethodDelegate(ClassWriter classWriter,
            String wrapperInternalName,
            PythonLikeType delegateType, Method interfaceMethod) {
        if (!interfaceMethod.getDeclaringClass().isInterface()) {
            return;
        }
        if (interfaceMethod.isDefault()) {
            // Default method, does not need to be present
            var methodType = delegateType.getMethodType(interfaceMethod.getName());
            if (methodType.isEmpty()) {
                return;
            }
            var function = methodType.get().getDefaultFunctionSignature();
            if (function.isEmpty()) {
                return;
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
        interfaceMethodVisitor.visitFieldInsn(Opcodes.GETFIELD, wrapperInternalName, "delegate",
                delegateType.getJavaTypeDescriptor());
        interfaceMethodVisitor.visitTypeInsn(Opcodes.NEW, Type.getInternalName(IdentityHashMap.class));
        interfaceMethodVisitor.visitInsn(Opcodes.DUP);
        interfaceMethodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(IdentityHashMap.class),
                "<init>", Type.getMethodDescriptor(Type.VOID_TYPE), false);
        interfaceMethodVisitor.visitVarInsn(Opcodes.ASTORE, interfaceMethod.getParameterCount() + 1);

        interfaceMethodVisitor.visitFieldInsn(Opcodes.GETSTATIC, wrapperInternalName,
                "argumentSpec$" + interfaceMethod.getName(),
                Type.getDescriptor(ArgumentSpec.class));

        var functionSignature = delegateType.getMethodType(interfaceMethod.getName())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Type %s cannot implement interface %s because it missing method %s."
                                .formatted(delegateType, interfaceMethod.getDeclaringClass(), interfaceMethod)))
                .getDefaultFunctionSignature()
                .orElseThrow();
        DelegatingInterfaceImplementor.prepareParametersForMethodCallFromArgumentSpec(
                interfaceMethod, interfaceMethodVisitor, functionSignature.getParameterTypes().length,
                Type.getType(functionSignature.getMethodDescriptor().getMethodDescriptor()),
                false);

        functionSignature.getMethodDescriptor().callMethod(interfaceMethodVisitor);

        var returnType = interfaceMethod.getReturnType();
        if (returnType.equals(void.class)) {
            interfaceMethodVisitor.visitInsn(Opcodes.RETURN);
        } else {
            if (returnType.isPrimitive()) {
                DelegatingInterfaceImplementor.loadBoxedPrimitiveTypeClass(returnType, interfaceMethodVisitor);
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
                DelegatingInterfaceImplementor.unboxBoxedPrimitiveType(returnType, interfaceMethodVisitor);
                interfaceMethodVisitor.visitInsn(Type.getType(returnType).getOpcode(Opcodes.IRETURN));
            } else {
                interfaceMethodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(returnType));
                interfaceMethodVisitor.visitInsn(Opcodes.ARETURN);
            }
        }
        interfaceMethodVisitor.visitMaxs(interfaceMethod.getParameterCount() + 2, 1);
        interfaceMethodVisitor.visitEnd();
    }
}
