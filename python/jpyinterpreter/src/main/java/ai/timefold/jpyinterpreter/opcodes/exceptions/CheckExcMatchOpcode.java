package ai.timefold.jpyinterpreter.opcodes.exceptions;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.ExceptionImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;

public class CheckExcMatchOpcode extends AbstractOpcode {

    public CheckExcMatchOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        return stackMetadata
                .pop()
                .push(ValueSourceInfo.of(this, BuiltinTypes.BOOLEAN_TYPE, stackMetadata.getValueSourcesUpToStackIndex(2)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        ExceptionImplementor.checkExcMatch(functionMetadata, stackMetadata);
    }
}
