package ai.timefold.jpyinterpreter.opcodes.stack;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.implementors.StackManipulationImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;

public class RotateFourOpcode extends AbstractOpcode {

    public RotateFourOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackMetadata) {
        return stackMetadata
                .pop()
                .pop()
                .pop()
                .pop()
                .push(stackMetadata.getValueSourceForStackIndex(0))
                .push(stackMetadata.getValueSourceForStackIndex(3))
                .push(stackMetadata.getValueSourceForStackIndex(2))
                .push(stackMetadata.getValueSourceForStackIndex(1));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StackManipulationImplementor.rotateFour(functionMetadata, stackMetadata);
    }
}
