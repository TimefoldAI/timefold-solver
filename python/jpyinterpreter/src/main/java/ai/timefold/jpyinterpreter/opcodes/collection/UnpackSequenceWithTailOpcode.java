package ai.timefold.jpyinterpreter.opcodes.collection;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.CollectionImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;

public class UnpackSequenceWithTailOpcode extends AbstractOpcode {

    public UnpackSequenceWithTailOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        // TODO: Correctly handle when high byte is set
        StackMetadata newStackMetadata = stackMetadata.pop();

        newStackMetadata = newStackMetadata
                .push(ValueSourceInfo.of(this, BuiltinTypes.LIST_TYPE, stackMetadata.getValueSourcesUpToStackIndex(1)));
        for (int i = 0; i < instruction.arg(); i++) {
            newStackMetadata = newStackMetadata.push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE,
                    stackMetadata.getValueSourcesUpToStackIndex(1)));
        }
        return newStackMetadata;
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        CollectionImplementor.unpackSequenceWithTail(functionMetadata.methodVisitor, instruction.arg(),
                stackMetadata.localVariableHelper);
    }
}
