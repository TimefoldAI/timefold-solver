package ai.timefold.jpyinterpreter.opcodes.function;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.FunctionImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonKnownFunctionType;
import ai.timefold.jpyinterpreter.types.PythonLikeGenericType;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

public class CallFunctionOpcode extends AbstractOpcode {

    public CallFunctionOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        PythonLikeType functionType = stackMetadata.getTypeAtStackIndex(instruction.arg());
        if (functionType instanceof PythonLikeGenericType) {
            functionType = ((PythonLikeGenericType) functionType).getOrigin().getConstructorType().orElse(null);
        }
        if (functionType instanceof PythonKnownFunctionType) {
            PythonKnownFunctionType knownFunctionType = (PythonKnownFunctionType) functionType;
            return knownFunctionType.getDefaultFunctionSignature()
                    .map(functionSignature -> stackMetadata.pop(instruction.arg() + 1).push(ValueSourceInfo.of(this,
                            functionSignature.getReturnType(),
                            stackMetadata.getValueSourcesUpToStackIndex(instruction.arg() + 1))))
                    .orElseGet(() -> stackMetadata.pop(instruction.arg() + 1)
                            .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE,
                                    stackMetadata.getValueSourcesUpToStackIndex(instruction.arg() + 1))));
        }
        return stackMetadata.pop(instruction.arg() + 1).push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE,
                stackMetadata.getValueSourcesUpToStackIndex(instruction.arg() + 1)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        FunctionImplementor.callFunction(functionMetadata, stackMetadata, functionMetadata.methodVisitor, instruction);
    }
}
