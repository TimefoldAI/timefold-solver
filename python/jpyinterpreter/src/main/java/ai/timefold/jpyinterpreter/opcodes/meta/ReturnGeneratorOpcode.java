package ai.timefold.jpyinterpreter.opcodes.meta;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;

import org.objectweb.asm.Opcodes;

public class ReturnGeneratorOpcode extends AbstractOpcode {

    public ReturnGeneratorOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        // Although this opcode does nothing, it is followed by a POP_TOP, which need something to pop
        return stackMetadata.push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        functionMetadata.methodVisitor.visitInsn(Opcodes.ACONST_NULL);
    }
}
