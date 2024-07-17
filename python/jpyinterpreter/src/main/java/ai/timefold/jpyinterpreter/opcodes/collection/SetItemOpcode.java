package ai.timefold.jpyinterpreter.opcodes.collection;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.implementors.CollectionImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;

public class SetItemOpcode extends AbstractOpcode {

    public SetItemOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata.pop(3);
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        CollectionImplementor.setItem(functionMetadata, stackMetadata);
    }
}
