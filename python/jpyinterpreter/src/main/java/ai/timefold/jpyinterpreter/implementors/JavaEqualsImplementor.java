package ai.timefold.jpyinterpreter.implementors;

import java.lang.reflect.Modifier;

import ai.timefold.jpyinterpreter.PythonClassTranslator;
import ai.timefold.jpyinterpreter.PythonCompiledClass;
import ai.timefold.jpyinterpreter.PythonCompiledFunction;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class JavaEqualsImplementor extends JavaInterfaceImplementor {
    final String internalClassName;

    public JavaEqualsImplementor(String internalClassName) {
        this.internalClassName = internalClassName;
    }

    @Override
    public Class<?> getInterfaceClass() {
        return Object.class;
    }

    private void typeCheck(MethodVisitor methodVisitor, PythonLikeType parameterType) {
        methodVisitor.visitInsn(Opcodes.DUP);
        methodVisitor.visitTypeInsn(Opcodes.INSTANCEOF, parameterType.getJavaTypeInternalName());

        Label isInstanceOfClass = new Label();
        methodVisitor.visitJumpInsn(Opcodes.IFNE, isInstanceOfClass);

        // Return false since the objects cannot be compared for equals
        methodVisitor.visitInsn(Opcodes.ICONST_0);
        methodVisitor.visitInsn(Opcodes.IRETURN);

        // Cast
        methodVisitor.visitLabel(isInstanceOfClass);
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, parameterType.getJavaTypeInternalName());
    }

    @Override
    public void implement(ClassWriter classWriter, PythonCompiledClass compiledClass) {
        PythonCompiledFunction comparisonFunction = compiledClass.instanceFunctionNameToPythonBytecode.get("__eq__");
        PythonLikeType parameterType = comparisonFunction.getParameterTypes().get(1);
        String methodName = PythonClassTranslator.getJavaMethodName("__eq__");
        Type returnType = PythonClassTranslator.getVirtualFunctionReturnType(comparisonFunction);

        MethodVisitor methodVisitor = classWriter.visitMethod(Modifier.PUBLIC, "equals",
                Type.getMethodDescriptor(Type.BOOLEAN_TYPE,
                        Type.getType(Object.class)),
                null,
                null);

        methodVisitor.visitParameter("other", 0);
        methodVisitor.visitCode();

        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
        typeCheck(methodVisitor, parameterType);

        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, internalClassName, methodName,
                Type.getMethodDescriptor(returnType, Type.getType(parameterType.getJavaTypeDescriptor())),
                false);

        Label ifNotEquals = new Label();
        PythonConstantsImplementor.loadTrue(methodVisitor);
        methodVisitor.visitJumpInsn(Opcodes.IF_ACMPNE, ifNotEquals);
        methodVisitor.visitInsn(Opcodes.ICONST_1);
        methodVisitor.visitInsn(Opcodes.IRETURN);

        methodVisitor.visitLabel(ifNotEquals);
        methodVisitor.visitInsn(Opcodes.ICONST_0);
        methodVisitor.visitInsn(Opcodes.IRETURN);

        methodVisitor.visitMaxs(0, 0);
        methodVisitor.visitEnd();
    }
}
