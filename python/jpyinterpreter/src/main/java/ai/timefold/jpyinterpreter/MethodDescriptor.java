package ai.timefold.jpyinterpreter;

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class MethodDescriptor {
    private final String declaringClassInternalName;
    private final String methodName;
    private final String methodDescriptor;

    private final MethodType methodType;

    private static Type resolveGenericType(java.lang.reflect.Type type, TypeVariable[] interfaceTypeVariables,
            List<Class<?>> typeArgumentList) {
        if (type instanceof Class) {
            return Type.getType((Class<?>) type);
        } else if (type instanceof TypeVariable) {
            for (int i = 0; i < interfaceTypeVariables.length; i++) {
                if (interfaceTypeVariables[i].equals(type)) {
                    return Type.getType(typeArgumentList.get(i));
                }
            }
            throw new IllegalStateException("Unknown TypeVariable " + type);
        } else if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            java.lang.reflect.Type[] lowerBounds = wildcardType.getLowerBounds();
            java.lang.reflect.Type[] upperBounds = wildcardType.getUpperBounds();

            if (lowerBounds.length > 0) {
                return resolveGenericType(lowerBounds[0], interfaceTypeVariables, typeArgumentList);
            }

            if (upperBounds.length > 0) {
                return resolveGenericType(upperBounds[0], interfaceTypeVariables, typeArgumentList);
            }

            throw new IllegalStateException("Wildcard Type " + wildcardType + " has no upper or lower bounds.");
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            return resolveGenericType(parameterizedType.getRawType(), interfaceTypeVariables, typeArgumentList);
        } else if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            return Type.getType('[' +
                    resolveGenericType(genericArrayType.getGenericComponentType(),
                            interfaceTypeVariables,
                            typeArgumentList).getDescriptor());
        } else {
            throw new IllegalArgumentException("Unknown class (" + type.getClass() + ") of argument (" + type + ")");
        }
    }

    public MethodDescriptor(Class<?> interfaceClass, Method method, List<Class<?>> typeArgumentList) {
        TypeVariable[] interfaceTypeVariables = interfaceClass.getTypeParameters();
        if (interfaceTypeVariables.length != typeArgumentList.size()) {
            throw new IllegalArgumentException("Type argument list (" + typeArgumentList + ") does not have same size as " +
                    interfaceClass + " generic argument list (" + Arrays.toString(interfaceTypeVariables) + ")");
        }
        this.declaringClassInternalName = Type.getInternalName(method.getDeclaringClass());
        this.methodName = method.getName();
        if (method.getDeclaringClass().isInterface()) {
            this.methodType = MethodType.INTERFACE;
        } else if (Modifier.isStatic(method.getModifiers())) {
            this.methodType = MethodType.STATIC;
        } else {
            this.methodType = MethodType.VIRTUAL;
        }

        String methodDescriptorString = "";
        Type returnType = resolveGenericType(method.getGenericReturnType(), interfaceTypeVariables, typeArgumentList);
        Type[] argumentTypes = new Type[method.getParameterCount()];
        for (int i = 0; i < argumentTypes.length; i++) {
            argumentTypes[i] =
                    resolveGenericType(method.getGenericParameterTypes()[i], interfaceTypeVariables, typeArgumentList);
        }
        this.methodDescriptor = Type.getMethodDescriptor(returnType, argumentTypes);
    }

    public MethodDescriptor(Method method) {
        this.declaringClassInternalName = Type.getInternalName(method.getDeclaringClass());
        this.methodName = method.getName();
        this.methodDescriptor = Type.getMethodDescriptor(method);
        if (method.getDeclaringClass().isInterface()) {
            this.methodType = MethodType.INTERFACE;
        } else if (Modifier.isStatic(method.getModifiers())) {
            this.methodType = MethodType.STATIC;
        } else {
            this.methodType = MethodType.VIRTUAL;
        }
    }

    public MethodDescriptor(Method method, MethodType type) {
        this.declaringClassInternalName = Type.getInternalName(method.getDeclaringClass());
        this.methodName = method.getName();
        this.methodDescriptor = Type.getMethodDescriptor(method);
        this.methodType = type;
    }

    public MethodDescriptor(Constructor<?> constructor) {
        this.declaringClassInternalName = Type.getInternalName(constructor.getDeclaringClass());
        this.methodName = constructor.getName();
        this.methodDescriptor = Type.getConstructorDescriptor(constructor);
        this.methodType = MethodType.CONSTRUCTOR;
    }

    public MethodDescriptor(String declaringClassInternalName, MethodType methodType, String methodName,
            String methodDescriptor) {
        this.declaringClassInternalName = declaringClassInternalName;
        this.methodName = methodName;
        this.methodDescriptor = methodDescriptor;
        this.methodType = methodType;
    }

    public MethodDescriptor(Class<?> declaringClass, MethodType methodType,
            String methodName, Class<?> returnType, Class<?>... parameterTypes) {
        this.declaringClassInternalName = Type.getInternalName(declaringClass);
        this.methodName = methodName;
        Type[] parameterAsmTypes = new Type[parameterTypes.length];
        for (int i = 0; i < parameterAsmTypes.length; i++) {
            parameterAsmTypes[i] = Type.getType(parameterTypes[i]);
        }
        this.methodDescriptor = Type.getMethodDescriptor(Type.getType(returnType), parameterAsmTypes);
        this.methodType = methodType;
    }

    public static MethodDescriptor useStaticMethodAsVirtual(Method method) {
        return new MethodDescriptor(method.getDeclaringClass(), MethodType.STATIC_AS_VIRTUAL,
                method.getName(), method.getReturnType(), method.getParameterTypes());
    }

    public String getDeclaringClassInternalName() {
        return declaringClassInternalName;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodDescriptor() {
        return methodDescriptor;
    }

    public MethodType getMethodType() {
        return methodType;
    }

    public void callMethod(MethodVisitor methodVisitor) {
        methodVisitor.visitMethodInsn(getMethodType().getOpcode(), getDeclaringClassInternalName(), getMethodName(),
                getMethodDescriptor(),
                getMethodType() == MethodType.INTERFACE);
    }

    public MethodDescriptor withReturnType(Type returnType) {
        return new MethodDescriptor(getDeclaringClassInternalName(), getMethodType(), getMethodName(),
                Type.getMethodDescriptor(returnType, Type.getArgumentTypes(getMethodDescriptor())));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MethodDescriptor that = (MethodDescriptor) o;
        return getDeclaringClassInternalName().equals(that.getDeclaringClassInternalName())
                && getMethodName().equals(that.getMethodName())
                && getMethodDescriptor().equals(that.getMethodDescriptor());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDeclaringClassInternalName(), getMethodName(), getMethodDescriptor());
    }

    public Type getReturnType() {
        return Type.getReturnType(getMethodDescriptor());
    }

    public Type[] getParameterTypes() {
        return Type.getArgumentTypes(getMethodDescriptor());
    }

    public enum MethodType {
        VIRTUAL(Opcodes.INVOKEVIRTUAL, false),
        STATIC(Opcodes.INVOKESTATIC, true),
        CLASS(Opcodes.INVOKESTATIC, true),
        STATIC_AS_VIRTUAL(Opcodes.INVOKESTATIC, true),
        INTERFACE(Opcodes.INVOKEINTERFACE, false),
        CONSTRUCTOR(Opcodes.INVOKESPECIAL, false);

        private final int opcode;
        private final boolean isStatic;

        MethodType(int opcode, boolean isStatic) {
            this.opcode = opcode;
            this.isStatic = isStatic;
        }

        public int getOpcode() {
            return opcode;
        }

        public boolean isStatic() {
            return isStatic;
        }
    }
}
