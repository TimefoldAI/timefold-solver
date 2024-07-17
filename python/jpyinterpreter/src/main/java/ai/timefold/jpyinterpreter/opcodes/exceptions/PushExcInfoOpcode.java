package ai.timefold.jpyinterpreter.opcodes.exceptions;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.ExceptionImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.errors.PythonBaseException;

public class PushExcInfoOpcode extends AbstractOpcode {

    public PushExcInfoOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata
                .pop()
                .push(ValueSourceInfo.of(this, PythonBaseException.BASE_EXCEPTION_TYPE))
                .push(stackMetadata.getTOSValueSource());
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        ExceptionImplementor.pushExcInfo(functionMetadata, stackMetadata);
    }
}
