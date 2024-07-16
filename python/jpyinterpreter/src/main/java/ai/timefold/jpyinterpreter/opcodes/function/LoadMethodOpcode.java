package ai.timefold.jpyinterpreter.opcodes.function;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.FunctionImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonLikeFunction;
import ai.timefold.jpyinterpreter.types.PythonLikeGenericType;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

public class LoadMethodOpcode extends AbstractOpcode {

    public LoadMethodOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        PythonLikeType stackTosType = stackMetadata.getTOSType();
        PythonLikeType tosType;
        if (stackTosType instanceof PythonLikeGenericType) {
            tosType = ((PythonLikeGenericType) stackTosType).getOrigin();
        } else {
            tosType = stackTosType;
        }

        return tosType.getMethodType(functionMetadata.pythonCompiledFunction.co_names.get(instruction.arg()))
                .map(knownFunction -> stackMetadata.pop()
                        .push(ValueSourceInfo.of(this, knownFunction, stackMetadata.getValueSourcesUpToStackIndex(1)))
                        .push(ValueSourceInfo.of(this, tosType, stackMetadata.getValueSourcesUpToStackIndex(1))) // TOS, since we know the function exists
                )
                .orElseGet(() -> stackMetadata.pop()
                        .push(ValueSourceInfo.of(this, PythonLikeFunction.getFunctionType(),
                                stackMetadata.getValueSourcesUpToStackIndex(1)))
                        .push(ValueSourceInfo.of(this, BuiltinTypes.NULL_TYPE,
                                stackMetadata.getValueSourcesUpToStackIndex(1)))); // either TOS or NULL
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        FunctionImplementor.loadMethod(functionMetadata, stackMetadata, instruction.arg());
    }
}
