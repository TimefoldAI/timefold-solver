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

public class CallFunctionKeywordOpcode extends AbstractOpcode {

    public CallFunctionKeywordOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        PythonLikeType functionType = stackMetadata.getTypeAtStackIndex(instruction.arg() + 1);
        if (functionType instanceof PythonLikeGenericType) {
            functionType = ((PythonLikeGenericType) functionType).getOrigin().getConstructorType().orElse(null);
        }
        if (functionType instanceof PythonKnownFunctionType) {
            PythonKnownFunctionType knownFunctionType = (PythonKnownFunctionType) functionType;
            return knownFunctionType.getDefaultFunctionSignature()
                    .map(functionSignature -> stackMetadata.pop(instruction.arg() + 2).push(ValueSourceInfo.of(this,
                            functionSignature.getReturnType(),
                            stackMetadata.getValueSourcesUpToStackIndex(instruction.arg() + 2))))
                    .orElseGet(() -> stackMetadata.pop(instruction.arg() + 2)
                            .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE,
                                    stackMetadata.getValueSourcesUpToStackIndex(instruction.arg() + 2))));
        }
        return stackMetadata.pop(instruction.arg() + 2).push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE,
                stackMetadata.getValueSourcesUpToStackIndex(instruction.arg() + 2)));
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        FunctionImplementor.callFunctionWithKeywords(functionMetadata, stackMetadata, functionMetadata.methodVisitor,
                instruction);
    }
}
