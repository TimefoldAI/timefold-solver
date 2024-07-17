package ai.timefold.jpyinterpreter.opcodes.function;

import ai.timefold.jpyinterpreter.FunctionMetadata;
import ai.timefold.jpyinterpreter.PythonBytecodeInstruction;
import ai.timefold.jpyinterpreter.StackMetadata;
import ai.timefold.jpyinterpreter.ValueSourceInfo;
import ai.timefold.jpyinterpreter.implementors.FunctionImplementor;
import ai.timefold.jpyinterpreter.opcodes.AbstractOpcode;
import ai.timefold.jpyinterpreter.types.BuiltinTypes;
import ai.timefold.jpyinterpreter.types.PythonKnownFunctionType;
import ai.timefold.jpyinterpreter.types.PythonLikeType;

public class CallMethodOpcode extends AbstractOpcode {

    public CallMethodOpcode(PythonBytecodeInstruction instruction) {
        super(instruction);
    }

    @Override
    protected StackMetadata getStackMetadataAfterInstruction(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        PythonLikeType functionType = stackMetadata.getTypeAtStackIndex(instruction.arg() + 1);
        if (functionType instanceof PythonKnownFunctionType) {
            PythonKnownFunctionType knownFunctionType = (PythonKnownFunctionType) functionType;
            PythonLikeType[] parameterTypes =
                    new PythonLikeType[instruction.arg()];
            for (int i = 0; i < parameterTypes.length; i++) {
                parameterTypes[parameterTypes.length - i - 1] = stackMetadata.getTypeAtStackIndex(i);
            }
            return knownFunctionType.getFunctionForParameters(parameterTypes)
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
        FunctionImplementor.callMethod(functionMetadata, stackMetadata, functionMetadata.methodVisitor, instruction,
                stackMetadata.localVariableHelper);
    }
}
