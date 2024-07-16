package ai.timefold.jpyinterpreter.opcodes.dunder;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.DunderOperatorImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;

public class GetSliceOpcode extends AbstractOpcode {
    public GetSliceOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        // TODO: Type the result
        return stackMetadata.pop(3)
                .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE,
                        stackMetadata.getValueSourcesUpToStackIndex(3)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        DunderOperatorImplementor.getSlice(functionMetadata, stackMetadata);
    }
}
