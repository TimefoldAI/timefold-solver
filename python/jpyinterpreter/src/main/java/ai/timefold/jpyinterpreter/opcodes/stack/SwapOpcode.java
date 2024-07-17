package ai.timefold.jpyinterpreter.opcodes.stack;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.implementors.StackManipulationImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;

public class SwapOpcode extends AbstractOpcode {

    public SwapOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return stackMetadata
                .set(instruction.arg() - 1, stackMetadata.getTOSValueSource())
                .set(0, stackMetadata.getValueSourceForStackIndex(instruction.arg() - 1));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StackManipulationImplementor.swapTOSWithIndex(functionMetadata, stackMetadata, instruction.arg() - 1);
    }
}
