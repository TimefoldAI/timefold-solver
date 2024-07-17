package ai.timefold.jpyinterpreter.opcodes.collection;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.CollectionImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;

public class UnpackSequenceOpcode extends AbstractOpcode {

    public UnpackSequenceOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        StackMetadata newStackMetadata = stackMetadata.pop();
        for (int i = 0; i < instruction.arg(); i++) {
            newStackMetadata = newStackMetadata.push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE,
                    stackMetadata.getValueSourcesUpToStackIndex(1)));
        }
        return newStackMetadata;
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        CollectionImplementor.unpackSequence(functionMetadata.methodVisitor, instruction.arg(),
                stackMetadata.localVariableHelper);
    }
}
