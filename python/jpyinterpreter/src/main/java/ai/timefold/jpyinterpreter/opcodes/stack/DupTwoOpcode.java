package ai.timefold.jpyinterpreter.opcodes.stack;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.implementors.StackManipulationImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;

public class DupTwoOpcode extends AbstractOpcode {

    public DupTwoOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    public StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata,
            StackMetadata stackTypesBeforeInstruction) {
        return stackTypesBeforeInstruction
                .push(stackTypesBeforeInstruction.getValueSourceForStackIndex(1))
                .push(stackTypesBeforeInstruction.getValueSourceForStackIndex(0));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StackManipulationImplementor.duplicateTOSAndTOS1(functionMetadata.methodVisitor);
    }
}
