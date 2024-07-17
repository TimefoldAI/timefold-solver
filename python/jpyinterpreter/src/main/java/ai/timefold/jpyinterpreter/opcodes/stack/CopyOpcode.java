package ai.timefold.jpyinterpreter.opcodes.stack;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.implementors.StackManipulationImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;

public class CopyOpcode extends AbstractOpcode {

    public CopyOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return stackMetadata.push(stackMetadata.getValueSourceForStackIndex(instruction.arg() - 1));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StackManipulationImplementor.duplicateToTOS(functionMetadata, stackMetadata, instruction.arg() - 1);
    }
}
