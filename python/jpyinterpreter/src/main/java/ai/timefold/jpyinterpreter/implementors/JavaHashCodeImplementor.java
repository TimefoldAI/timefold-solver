package ai.timefold.jpyinterpreter.implementors;

import java.lang.reflect.Modifier;

import ai.timefold.jpyinterpreter.PythonClassTranslator;
import ai.timefold.jpyinterpreter.PythonCompiledClass;
import ai.timefold.jpyinterpreter.PythonCompiledFunction;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class JavaHashCodeImplementor extends JavaInterfaceImplementor {
    final String internalClassName;

    public JavaHashCodeImplementor(String internalClassName) {
        this.internalClassName = internalClassName;
    }

    @Override
    public Class<?> getInterfaceClass() {
        return Object.class;
    }

    @Override
    public void implement(ClassWriter classWriter, PythonCompiledClass compiledClass) {
        PythonCompiledFunction comparisonFunction = compiledClass.instanceFunctionNameToPythonBytecode.get("__hash__");
        String methodName = PythonClassTranslator.getJavaMethodName("__hash__");
        Type returnType = PythonClassTranslator.getVirtualFunctionReturnType(comparisonFunction);

        MethodVisitor methodVisitor = classWriter.visitMethod(Modifier.PUBLIC, "hashCode",
                Type.getMethodDescriptor(Type.INT_TYPE),
                null,
                null);

        methodVisitor.visitCode();

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);

        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalClassName, methodName,
                Type.getMethodDescriptor(returnType),
                false);

        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Object.class), "hashCode",
                Type.getMethodDescriptor(Type.INT_TYPE),
                false);

        methodVisitor.visitInsn(Opcodes.IRETURN);

        methodVisitor.visitMaxs(0, 0);
        methodVisitor.visitEnd();
    }
}
