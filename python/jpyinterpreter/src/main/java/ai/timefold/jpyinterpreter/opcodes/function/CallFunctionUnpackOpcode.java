package ai.timefold.jpyinterpreter.opcodes.function;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonVersion;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.FunctionImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;

public class CallFunctionUnpackOpcode extends AbstractOpcode {

    public CallFunctionUnpackOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        if (functionMetadata.pythonCompiledFunction.pythonVersion.isBefore(PythonVersion.PYTHON_3_11)) {
            if ((instruction.arg() & 1) == 1) {
                // Stack is callable, iterable, map
                return stackMetadata.pop(3).push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE,
                        stackMetadata.getValueSourcesUpToStackIndex(3)));
            } else {
                // Stack is callable, iterable
                return stackMetadata.pop(2).push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE,
                        stackMetadata.getValueSourcesUpToStackIndex(2)));
            }
        } else {
            if ((instruction.arg() & 1) == 1) {
                // Stack is null, callable, iterable, map
                return stackMetadata.pop(4).push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE,
                        stackMetadata.getValueSourcesUpToStackIndex(3)));
            } else {
                // Stack is null, callable, iterable
                return stackMetadata.pop(3).push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE,
                        stackMetadata.getValueSourcesUpToStackIndex(2)));
            }
        }
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        FunctionImplementor.callFunctionUnpack(functionMetadata, stackMetadata, instruction);
    }
}
