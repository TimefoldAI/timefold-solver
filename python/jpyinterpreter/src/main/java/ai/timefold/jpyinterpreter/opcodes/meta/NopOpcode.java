package ai.timefold.jpyinterpreter.opcodes.meta;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;

import org.objectweb.asm.Opcodes;

public class NopOpcode extends AbstractOpcode {

    public NopOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
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
}
