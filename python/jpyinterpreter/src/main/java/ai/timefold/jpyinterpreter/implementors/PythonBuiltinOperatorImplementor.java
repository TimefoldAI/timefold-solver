package ai.timefold.jpyinterpreter.implementors;

import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.types.numeric.PythonBoolean;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Implementations of opcodes/operations that do not use dunder methods / are builtin.
 */
public class PythonBuiltinOperatorImplementor {

    /**
     * Replace TOS with not TOS.
     */
    public static void performNotOnTOS(MethodVisitor methodVisitor) {
        methodVisitor.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(PythonBoolean.class));
        methodVisitor.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(PythonBoolean.class),
                "not", Type.getMethodDescriptor(Type.getType(PythonBoolean.class)),
                false);
    }

    /**
     * Perform TOS is TOS1. If {@code instruction} argument is 1, perform TOS is not TOS1 instead.
     */
    public static void isOperator(MethodVisitor methodVisitor, PythonBytecodeInstruction instruction) {
        int opcode = (instruction.arg() == 0) ? Opcodes.IF_ACMPEQ : Opcodes.IF_ACMPNE;
        Label trueBranchLabel = new Label();
        Label endLabel = new Label();

        methodVisitor.visitJumpInsn(opcode, trueBranchLabel);
        PythonConstantsImplementor.loadFalse(methodVisitor);
        methodVisitor.visitJumpInsn(Opcodes.GOTO, endLabel);

        methodVisitor.visitLabel(trueBranchLabel);
        PythonConstantsImplementor.loadTrue(methodVisitor);
        methodVisitor.visitLabel(endLabel);
    }
}
