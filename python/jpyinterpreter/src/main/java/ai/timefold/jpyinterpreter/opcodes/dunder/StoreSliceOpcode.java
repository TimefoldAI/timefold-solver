package ai.timefold.jpyinterpreter.opcodes.dunder;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.implementors.DunderOperatorImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;

public class StoreSliceOpcode extends AbstractOpcode {
    public StoreSliceOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop(4);
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        DunderOperatorImplementor.storeSlice(functionMetadata, stackMetadata);
    }
}
