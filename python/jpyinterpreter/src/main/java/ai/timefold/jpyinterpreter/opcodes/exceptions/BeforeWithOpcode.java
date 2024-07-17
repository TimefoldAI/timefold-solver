package ai.timefold.jpyinterpreter.opcodes.exceptions;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.ExceptionImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;

public class BeforeWithOpcode extends AbstractOpcode {

    public BeforeWithOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata
                .pop()
                .push(ValueSourceInfo.of(this, PythonLikeFunction.getFunctionType(), stackMetadata.getTOSValueSource()))
                .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE, stackMetadata.getTOSValueSource()));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        ExceptionImplementor.beforeWith(functionMetadata, stackMetadata);
    }
}
