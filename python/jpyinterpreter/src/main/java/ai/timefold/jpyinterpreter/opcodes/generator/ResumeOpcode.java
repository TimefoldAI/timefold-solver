package ai.timefold.jpyinterpreter.opcodes.generator;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;

import org.objectweb.asm.Opcodes;

public class ResumeOpcode extends AbstractOpcode {

    public ResumeOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    public static ResumeType getResumeType(int arg) {
        switch (arg) {
            case 0:
                return ResumeType.START;

            case 1:
                return ResumeType.YIELD;

            case 2:
                return ResumeType.YIELD_FROM;

            case 3:
                return ResumeType.AWAIT;

            default:
                throw new IllegalArgumentException("Invalid RESUME opcode argument: " + arg);
        }
    }

    public ResumeType getResumeType() {
        return getResumeType(instruction.arg());
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return stackMetadata.copy();
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        functionMetadata.methodVisitor.visitInsn(Opcodes.NOP);
    }

    public enum ResumeType {
        START,
        YIELD,
        YIELD_FROM,
        AWAIT
    }
}
