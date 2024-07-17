package ai.timefold.jpyinterpreter.opcodes.function;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

public class CallOpcode extends AbstractOpcode {

    public CallOpcode(PythonBytecodeInstruction instruction) {
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
            List<String> keywordArgumentNameList = stackMetadata.getCallKeywordNameList();
            List<PythonLikeType> callStackParameterTypes = stackMetadata.getValueSourcesUpToStackIndex(instruction.arg())
                    .stream().map(ValueSourceInfo::getValueType).collect(Collectors.toList());

            return knownFunctionType.getFunctionForParameters(instruction.arg() - keywordArgumentNameList.size(),
                    keywordArgumentNameList,
                    callStackParameterTypes)
                    .map(functionSignature -> stackMetadata.pop(instruction.arg() + 2).push(ValueSourceInfo.of(this,
                            functionSignature.getReturnType(),
                            stackMetadata.getValueSourcesUpToStackIndex(instruction.arg() + 2))))
                    .orElseGet(() -> stackMetadata.pop(instruction.arg() + 2)
                            .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE,
                                    stackMetadata.getValueSourcesUpToStackIndex(instruction.arg() + 2))))
                    .setCallKeywordNameList(Collections.emptyList());
        }

        functionType = stackMetadata.getTypeAtStackIndex(instruction.arg());
        if (functionType instanceof PythonLikeGenericType) {
            functionType = ((PythonLikeGenericType) functionType).getOrigin().getConstructorType().orElse(null);
        }
        if (functionType instanceof PythonKnownFunctionType) {
            PythonKnownFunctionType knownFunctionType = (PythonKnownFunctionType) functionType;
            List<String> keywordArgumentNameList = stackMetadata.getCallKeywordNameList();
            List<PythonLikeType> callStackParameterTypes = stackMetadata.getValueSourcesUpToStackIndex(instruction.arg())
                    .stream().map(ValueSourceInfo::getValueType).collect(Collectors.toList());

            return knownFunctionType.getFunctionForParameters(instruction.arg() - keywordArgumentNameList.size(),
                    keywordArgumentNameList,
                    callStackParameterTypes)
                    .map(functionSignature -> stackMetadata.pop(instruction.arg() + 2).push(ValueSourceInfo.of(this,
                            functionSignature.getReturnType(),
                            stackMetadata.getValueSourcesUpToStackIndex(instruction.arg() + 2))))
                    .orElseGet(() -> stackMetadata.pop(instruction.arg() + 2)
                            .push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE,
                                    stackMetadata.getValueSourcesUpToStackIndex(instruction.arg() + 2))))
                    .setCallKeywordNameList(Collections.emptyList());
        }

        return stackMetadata.pop(instruction.arg() + 2).push(ValueSourceInfo.of(this, BuiltinTypes.BASE_TYPE,
                stackMetadata.getValueSourcesUpToStackIndex(instruction.arg() + 2)))
                .setCallKeywordNameList(Collections.emptyList());
    }

    @Override
    public void implement(FunctionMetadata functionMetadata, StackMetadata stackMetadata) {
        FunctionImplementor.call(functionMetadata, stackMetadata, instruction.arg());
    }
}
