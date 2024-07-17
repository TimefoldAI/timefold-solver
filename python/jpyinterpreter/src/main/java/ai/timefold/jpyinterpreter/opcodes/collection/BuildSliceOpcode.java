package ai.timefold.jpyinterpreter.opcodes.collection;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.CollectionImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.PythonSlice;

public class BuildSliceOpcode extends AbstractOpcode {

    public BuildSliceOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop(instruction.arg()).push(ValueSourceInfo.of(this, PythonSlice.SLICE_TYPE,
                stackMetadata.getValueSourcesUpToStackIndex(instruction.arg())));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        CollectionImplementor.buildSlice(functionMetadata, stackMetadata, instruction.arg());
    }
}
