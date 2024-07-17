package ai.timefold.jpyinterpreter.opcodes.exceptions;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonVersion;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.ExceptionImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;

public class WithExceptStartOpcode extends AbstractOpcode {

    public WithExceptStartOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        if (functionMetadata.pythonCompiledFunction.pythonVersion.isAtLeast(PythonVersion.PYTHON_3_11)) {
            return stackMetadata
                    .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE, stackMetadata.getValueSourceForStackIndex(1)));
        }
        return stackMetadata
                .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE, stackMetadata.getValueSourceForStackIndex(6)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        ExceptionImplementor.handleExceptionInWith(functionMetadata, stackMetadata);
    }
}
