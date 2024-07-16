package ai.timefold.jpyinterpreter.opcodes.function;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.PythonVersion;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.FunctionImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;

public class MakeFunctionOpcode extends AbstractOpcode {

    public MakeFunctionOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        int stackElements =
                (functionMetadata.pythonCompiledFunction.pythonVersion.isAtLeast(PythonVersion.PYTHON_3_11)) ? 1 : 2;
        return stackMetadata.pop(stackElements + Integer.bitCount(instruction.arg()))
                .push(ValueSourceInfo.of(this, PythonLikeFunction.getFunctionType(),
                        stackMetadata.getValueSourcesUpToStackIndex(stackElements + Integer.bitCount(instruction.arg()))));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        FunctionImplementor.createFunction(functionMetadata, stackMetadata, instruction);
    }
}
